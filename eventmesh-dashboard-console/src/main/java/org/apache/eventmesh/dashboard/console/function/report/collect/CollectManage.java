/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.annotation.ClusterTypeMark;
import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.common.model.base.BaseOrganizationBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.common.utils.RowMapperUtil;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.CollectExporter;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.factory.KafkaCollectFactory;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * 一个集群只允许一个模式，因为 PROMETHEUS是集群采集. 因为 prometheus 有两个模式
 * <pre>
 * 1. rocketmq 是多注册中心，可以采集多集群 。只能绑定到一个集群里面。 <p>
 *     1. TODO 如果多个集群共享 多个注册中心  然后有多个prometheus，会出现重复采集问题。 <p>
 *         所以共享 meta 集群不允许 使用 PROMETHEUS 模式。只允许使用 EVENTMESH  <p>
 *     2. meta 集群 创建的时候可以默认创建 PROMETHEUS   <p>
 *     3. meta 集群节点发生更新的时候，也需要更新PROMETHEUS  <p>
 *     4. 提供重启功能  <p>
 *     5. 只允许手动 与 私有 meta 集群被差集   <p>
 * 2. kafka 单注册单集群，可以绑定 kafka 集群上面  <p>
 * 3. 查询 collect 表信息，创建 cluster 的时候，会默认有 collect 配置 <p>
 * 4. 定时同步 collect ， runtime 信息。 collect 用于触发 collect 行为， <p>
 *     runtime信息 用于触发 runtime 差集
 * </pre>
 *
 * 
 */
@Slf4j
public class CollectManage {

    private static final LocalDateTime INITIAL_KAFKA_SYNC = LocalDateTime.of(2000, 1, 1, 0, 0);

    /** 与现有采集配置处理一致，排除状态为 2 的配置。 */
    private static final long EXCLUDED_COLLECT_STATUS = 2L;

    private static final Map<ClusterType, Class<?>> CLUSTER_TYPE_CLASS = new HashMap<>();

    static {
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(CollectManage.class).subPath("/exporter/**").annotationSet(Set.of(ClusterTypeMark.class)).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(CollectManage::handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void handler(Class<?> clazz) {
        ClusterTypeMark clusterTypeMark = clazz.getAnnotation(ClusterTypeMark.class);
        if (Objects.isNull(clusterTypeMark)) {
            return;
        }
        for (ClusterType clusterType : clusterTypeMark.clusterType()) {
            CLUSTER_TYPE_CLASS.put(clusterType, clazz);
        }
    }


    private final ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(20, 100, 5, TimeUnit.HOURS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "collect-thread-" + index.incrementAndGet());
            }
        });
    /**
     * 等待执行的 collect，在 cluster runtime ，加载的时候就写入与删除
     * <pre>
     *     TODO collectCacheMap 冲突，
     *          collectCacheMap 用于解决 普罗米修斯 采集问题，
     *          已经把 普罗米修斯 与 主动采集，统一在一起
     * </pre>
     */
    private final Map<String, Collect> waitExecuteCollectMap = new ConcurrentHashMap<>();

    /**
     * 执行中，不是运行中， 在 collect 加载的时候触发
     * <pre>
     *     TODO collectExecuteMap 冲突
     *          collectCacheMap 用于解决 普罗米修斯 采集问题，
     *          已经把 普罗米修斯 与 主动采集，统一在一起
     * </pre>
     */
    private final Map<String, Collect> executeingCollectMap = new ConcurrentHashMap<>();


    /**
     * runtime 元数据，用于聚合操作
     */
    private final Map<Long, Map<String, RuntimeMetadata>> runtimeMetadataMap = new ConcurrentHashMap<>();

    /** Kafka 同名节点不能共用原名称键，按元数据 ID 索引节点。 */
    private final Map<Long, Map<Long, RuntimeMetadata>> runtimeMetadataById = new ConcurrentHashMap<>();

    /**
     * cluster 元数据，用于聚合操作
     */
    private final Map<Long, ClusterMetadata> clusterMetadataMap = new ConcurrentHashMap<>();

    /**
     * collect 元数据，用于聚合操作 , key 为 clusterId
     */
    private final Map<Long, CollectMetadata> collectMetadataMap = new ConcurrentHashMap<>();


    private final SyncAndHandlerData syncAndHandlerData = new SyncAndHandlerData();

    private final PaddingService paddingService = new PaddingService();

    private final DataSyncHandler dataSyncHandler = new DataSyncHandler();

    private final KafkaCollectFactory kafkaCollectFactory = new KafkaCollectFactory();

    /** 旧配置缓存按配置 ID 存储，Kafka 按集群 ID 查询时需要隔离键空间。 */
    private final Map<Long, CollectMetadata> kafkaConfigurations = new ConcurrentHashMap<>();

    private LocalDateTime kafkaLastSync = INITIAL_KAFKA_SYNC;

    private KafkaDataSyncHandler kafkaDataSyncHandler;

    @Setter
    private DruidDataSource dataSource;

    @Setter
    private DruidDataSource kafkaDataSource;

    @Setter
    private ReportHandlerManage reportHandlerManage;


    public void init() {
        this.dataSyncHandler.setPaddingService(paddingService);
        this.dataSyncHandler.setReportEngine(this.reportHandlerManage.getReportEngine());
        // Kafka 结果独立交付，现有采集任务仍使用原来的批次处理器。
        this.kafkaDataSyncHandler = new KafkaDataSyncHandler(paddingService, this.reportHandlerManage.getReportEngine());
    }

    public void collect() {
        if (this.executeingCollectMap.isEmpty()) {
            log.info("Not executed because there is no collector to be executed ");
            return;
        }
        // 统一任务表中区分结果交付方式；Kafka 独立交付，其他采集器仍使用原批次处理。
        List<Collect> regularCollectors = new ArrayList<>();
        this.executeingCollectMap.values().forEach(collector -> {
            if (isKafkaCollector(collector)) {
                DataSyncHandlerWrapper wrapper = this.kafkaDataSyncHandler.getDataSyncHandlerWrapper(1);
                this.threadPoolExecutor.execute(() -> collector.collect(0, wrapper));
            } else {
                regularCollectors.add(collector);
            }
        });
        if (!regularCollectors.isEmpty()) {
            DataSyncHandlerWrapper wrapper = this.dataSyncHandler.getDataSyncHandlerWrapper(regularCollectors.size());
            regularCollectors.forEach(collector ->
                this.threadPoolExecutor.execute(() -> collector.collect(wrapper.getIndex(), wrapper)));
        }
    }

    public void syncData() {
        if (this.kafkaDataSource != null) {
            try (Connection connection = this.kafkaDataSource.getConnection()) {
                // 统一任务注册；仅 Kafka 元数据读取保留兼容现有表类型的映射。
                final LocalDateTime nextSync = LocalDateTime.now().minusSeconds(1);
                Set<Long> knownIds = new HashSet<>(this.kafkaConfigurations.keySet());
                knownIds.addAll(this.runtimeMetadataById.keySet());
                this.clusterMetadataMap.values().stream().filter(CollectManage::isKafkaCluster)
                    .map(ClusterMetadata::getId).forEach(knownIds::add);
                List<ClusterMetadata> clusters = KafkaMetadataReader.readClusters(connection, this.kafkaLastSync, knownIds);
                clusters.stream().filter(CollectManage::isKafkaCluster).map(ClusterMetadata::getId).forEach(knownIds::add);
                List<RuntimeMetadata> runtimes = KafkaMetadataReader.readRuntimes(connection, this.kafkaLastSync, knownIds);
                // 新接入或变更为 Kafka 的集群可能沿用更新时间更早的采集配置。
                boolean needsFullConfig = clusters.stream().anyMatch(cluster -> isKafkaCluster(cluster)
                    && !this.kafkaConfigurations.containsKey(cluster.getId()));
                LocalDateTime configSince = needsFullConfig ? INITIAL_KAFKA_SYNC : this.kafkaLastSync;
                List<CollectMetadata> configurations = KafkaMetadataReader.readConfigurations(connection, configSince, knownIds);
                this.handleKafkaMetadata(clusters, runtimes, configurations);
                this.kafkaLastSync = nextSync;
            } catch (Exception e) {
                log.error("Failed to synchronize Kafka collector metadata", e);
            }
        }
        this.syncAndHandlerData.syncData();
    }

    @Deprecated
    public List<Time> getData() {
        List<Time> timeList = new ArrayList<>();
        this.waitExecuteCollectMap.forEach((clusterId, exporter) -> {
        });
        return timeList;
    }

    public void register(String url, ClusterMetadata clusterMetadata) {
        CollectExporter collectExporter = new CollectExporter();
        collectExporter.setUrl(url);
        collectExporter.setClusterMetadata(clusterMetadata);
        collectExporter.setReportMetaDataMap(this.reportHandlerManage.getReportMetaDataMap());
        collectExporter.setAggregationMetaDataMap(this.reportHandlerManage.getAggregationMetaDataMap());
        collectExporter.init();
        waitExecuteCollectMap.put(this.createCollectKey(clusterMetadata), collectExporter);
    }

    public void handlerData(List<ClusterMetadata> clusterMetadataList, List<RuntimeMetadata> runtimeMetadataList,
        List<CollectMetadata> collectMetadataList) {
        // 先处理 Kafka，避免旧采集器的异常阻断同批次的 Kafka 任务注册。
        this.handleKafkaMetadata(clusterMetadataList, runtimeMetadataList, collectMetadataList);
        clusterMetadataList.forEach((clusterMetadata) -> {
            if (clusterMetadata.isDelete() || Objects.equals(clusterMetadata.getStatus(), 0L)) {
                clusterMetadataMap.remove(clusterMetadata.getId());
                this.deleteCollect(clusterMetadata);
                return;
            }
            this.clusterMetadataMap.put(clusterMetadata.getId(), clusterMetadata);
        });

        // clusterId , runtimeMetadata list
        // 需要全部缓存，因为 collect 可以从 普罗米修斯修改成 eventmesh
        runtimeMetadataList.forEach(runtimeMetadata -> {
            //TODO 需要识别是否是 启动状态
            if (runtimeMetadata.isDelete() || Objects.equals(runtimeMetadata.getStatus(), 0L)) {
                if (runtimeMetadataMap.containsKey(runtimeMetadata.getClusterId())) {
                    runtimeMetadataMap.get(runtimeMetadata.getClusterId()).remove(runtimeMetadata.getName());
                }
                return;
            }
            runtimeMetadataMap.computeIfAbsent(runtimeMetadata.getClusterId(), id -> new ConcurrentHashMap<>())
                .put(runtimeMetadata.getName(), runtimeMetadata);
            if (isKafkaRuntime(runtimeMetadata) || this.isKnownKafkaCluster(runtimeMetadata.getClusterId())) {
                return;
            }
            if (Objects.equals(runtimeMetadata.getClusterType(), ClusterType.PROMETHEUS_EXPORTER)) {
                String url = runtimeMetadata.getHost() + ":" + runtimeMetadata.getPort();
                register(url, clusterMetadataMap.get(runtimeMetadata.getClusterId()));
                String key = this.createCollectKey(runtimeMetadata);
                if (this.executeingCollectMap.containsKey(key)) {
                    this.executeingCollectMap.put(key, this.waitExecuteCollectMap.get(key));
                }
            } else {
                CollectMetadata collectMetadata = this.collectMetadataMap.get(runtimeMetadata.getClusterId());
                if (Objects.isNull(collectMetadata)) {
                    return;
                }
                ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(runtimeMetadata.getClusterType());
                Class<?> clazz = CLUSTER_TYPE_CLASS.get(runtimeMetadata.getClusterType());
                if (clusterFramework.isCAP()) {
                    return;
                }
                this.buildCollect(collectMetadata, this.clusterMetadataMap.get(runtimeMetadata.getClusterId()), runtimeMetadata, clazz);
            }
        });

        collectMetadataList.forEach(collectMetadata -> {
            if (this.isKafkaConfiguration(collectMetadata)) {
                return;
            }
            if (collectMetadata.isDelete() || Objects.equals(collectMetadata.getStatus(), 0L) || Objects.equals(collectMetadata.getStatus(), 2L)) {
                this.deleteByCollectMetadata(collectMetadata);
                return;
            }
            this.collectMetadataMap.put(collectMetadata.getId(), collectMetadata);
            if (Objects.equals(CollectType.EVENTMESH, collectMetadata.getCollectType())) {
                this.buildCollect(collectMetadata);
                return;
            }
            if (Objects.equals(CollectType.PROMETHEUS, collectMetadata.getCollectType())) {
                String key = this.createCollectKey(collectMetadata);
                Collect collect = waitExecuteCollectMap.get(key);
                executeingCollectMap.put(key, collect);
            }
        });
    }

    /**
     * <pre>
     *     1. 删除 普罗米修斯 的 collect
     *     2. 删除 runtime collect
     *     3. 删除 cluster collect
     * </pre>
     */
    public void deleteByCollectMetadata(CollectMetadata collectMetadata) {
        if (this.isKafkaConfiguration(collectMetadata)) {
            // 单独移除 Kafka 的集群与节点任务，不改变其他采集器的删除流程。
            this.deleteKafkaConfiguration(collectMetadata);
            return;
        }
        this.collectMetadataMap.remove(collectMetadata.getClusterId());
        if (Objects.equals(collectMetadata.getCollectType(), CollectType.PROMETHEUS)) {
            this.deleteCollect(collectMetadata);
            return;
        }

        Map<String, RuntimeMetadata> runtimeMetadataMap = this.runtimeMetadataMap.get(collectMetadata.getClusterId());
        if (Objects.nonNull(runtimeMetadataMap)) {
            runtimeMetadataMap.forEach((clusterId, runtimeMetadata) -> {
                this.deleteCollect(runtimeMetadata);
            });
            return;
        }
        this.deleteCollect(this.clusterMetadataMap.get(collectMetadata.getClusterId()));
    }


    /**
     * <pre>
     *     1. 构建 runtime collect
     *     2. 构建 cluster collect
     * </pre>
     */
    private void buildCollect(CollectMetadata collectMetadata) {
        ClusterMetadata clusterMetadata = clusterMetadataMap.get(collectMetadata.getClusterId());
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterMetadata.getClusterType());

        Class<?> clazz = CLUSTER_TYPE_CLASS.get(clusterMetadata.getClusterType());

        if (clusterFramework.isCAP()) {
            this.buildCollect(collectMetadata, clusterMetadata, null, clazz);
            return;
        }
        Map<String, RuntimeMetadata> runtimeMetadataMap = this.runtimeMetadataMap.get(clusterMetadata.getId());
        runtimeMetadataMap.values().forEach(runtimeMetadata -> buildCollect(collectMetadata, clusterMetadata, runtimeMetadata, clazz));

    }

    @SuppressWarnings({"AliDeprecation", "deprecation"})
    private void buildCollect(CollectMetadata collectMetadata, ClusterMetadata clusterMetadata, RuntimeMetadata runtimeMetadata, Class<?> clazz) {
        try {
            AbstractCollect object = (AbstractCollect) clazz.newInstance();
            object.setCollectMetadata(collectMetadata);
            object.setClusterMetadata(clusterMetadata);
            object.setRuntimeMetadata(runtimeMetadata);
            this.executeingCollectMap.put(this.createCollectKey(Objects.nonNull(runtimeMetadata) ? runtimeMetadata : clusterMetadata), object);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isKafkaCollector(Collect collector) {
        return collector instanceof KafkaAdminCollect || collector instanceof KafkaJmxCollect;
    }

    private static boolean isKafkaCluster(ClusterMetadata cluster) {
        return cluster != null && Objects.equals(cluster.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER);
    }

    private static boolean isKafkaRuntime(RuntimeMetadata runtime) {
        return runtime != null && (Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_BROKER)
            || Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_RAFT));
    }

    private boolean isKnownKafkaCluster(Long clusterId) {
        return clusterId != null && (this.kafkaConfigurations.containsKey(clusterId)
            || this.runtimeMetadataById.containsKey(clusterId)
            || isKafkaCluster(this.clusterMetadataMap.get(clusterId)));
    }

    private boolean isKafkaConfiguration(CollectMetadata configuration) {
        return configuration != null && (Objects.equals(configuration.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER)
            || this.isKnownKafkaCluster(configuration.getClusterId()));
    }

    private synchronized void handleKafkaMetadata(List<ClusterMetadata> clusters, List<RuntimeMetadata> runtimes,
        List<CollectMetadata> configurations) {
        Set<Long> changed = new HashSet<>();
        for (ClusterMetadata cluster : clusters) {
            Long clusterId = cluster.getId();
            if (isKafkaCluster(cluster)) {
                if (cluster.isDelete() || Objects.equals(cluster.getStatus(), RecordStatus.INACTIVE.getNumber().longValue())) {
                    this.clusterMetadataMap.remove(clusterId);
                } else {
                    this.clusterMetadataMap.put(clusterId, cluster);
                }
                changed.add(clusterId);
            } else if (this.isKnownKafkaCluster(clusterId)) {
                this.removeKafkaCollectors(clusterId);
                this.kafkaConfigurations.remove(clusterId);
                this.runtimeMetadataById.remove(clusterId);
                // 同批次的节点和配置需看到新集群类型，不能重新注册旧 Kafka 任务。
                if (cluster.isDelete() || Objects.equals(cluster.getStatus(), RecordStatus.INACTIVE.getNumber().longValue())) {
                    this.clusterMetadataMap.remove(clusterId);
                } else {
                    this.clusterMetadataMap.put(clusterId, cluster);
                }
            }
        }
        for (RuntimeMetadata runtime : runtimes) {
            Long clusterId = runtime.getClusterId();
            if (!isKafkaRuntime(runtime) && !this.isKnownKafkaCluster(clusterId)) {
                continue;
            }
            Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataById.computeIfAbsent(clusterId, id -> new ConcurrentHashMap<>());
            String key = this.createCollectKey(runtime);
            if (!isKafkaRuntime(runtime) || runtime.isDelete()
                || Objects.equals(runtime.getStatus(), RecordStatus.INACTIVE.getNumber().longValue())) {
                nodes.remove(runtime.getId());
                this.removeKafkaCollector(key);
            } else {
                nodes.put(runtime.getId(), runtime);
            }
            changed.add(clusterId);
        }
        for (CollectMetadata configuration : configurations) {
            if (!this.isKafkaConfiguration(configuration)) {
                continue;
            }
            this.kafkaConfigurations.put(configuration.getClusterId(), configuration);
            changed.add(configuration.getClusterId());
        }
        changed.forEach(this::rebuildKafkaCollectors);
    }

    private void rebuildKafkaCollectors(Long clusterId) {
        this.removeKafkaCollectors(clusterId);
        ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
        CollectMetadata configuration = this.kafkaConfigurations.get(clusterId);
        if (!isKafkaCluster(cluster) || cluster.isDelete()
            || Objects.equals(cluster.getStatus(), RecordStatus.INACTIVE.getNumber().longValue())
            || configuration == null || configuration.isDelete()
            || Objects.equals(configuration.getStatus(), RecordStatus.INACTIVE.getNumber().longValue())
            || Objects.equals(configuration.getStatus(), EXCLUDED_COLLECT_STATUS) || Boolean.FALSE.equals(configuration.getEnable())
            || !Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH)) {
            return;
        }
        Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataById.getOrDefault(clusterId, Map.of());
        this.executeingCollectMap.putAll(
            this.kafkaCollectFactory.createCollectors(configuration, cluster, new ArrayList<>(nodes.values())));
    }

    private void removeKafkaCollectors(Long clusterId) {
        this.removeKafkaCollector(ClusterMetadata.class.getSimpleName() + "-" + clusterId);
        Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataById.get(clusterId);
        if (nodes != null) {
            nodes.values().forEach(runtime -> this.removeKafkaCollector(this.createCollectKey(runtime)));
        }
    }

    private void removeKafkaCollector(String key) {
        Collect collector = this.executeingCollectMap.get(key);
        if (isKafkaCollector(collector)) {
            this.executeingCollectMap.remove(key, collector);
        }
    }

    private synchronized void deleteKafkaConfiguration(CollectMetadata configuration) {
        this.kafkaConfigurations.remove(configuration.getClusterId());
        this.removeKafkaCollectors(configuration.getClusterId());
    }

    private String createCollectKey(BaseOrganizationBase base) {
        return base.getClass().getSimpleName() + "-" + base.getId();
    }

    private void deleteCollect(BaseOrganizationBase base) {
        if (Objects.isNull(base)) {
            return;
        }
        String key = this.createCollectKey(base);
        this.executeingCollectMap.remove(key);
        if (base instanceof CollectMetadata) {
            return;
        }
        this.waitExecuteCollectMap.remove(key);
    }

    private class SyncAndHandlerData {

        private List<ClusterMetadata> clusterMetadataList;
        private List<RuntimeMetadata> runtimeMetadataList;
        private List<CollectMetadata> collectMetadataList;
        private LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);

        public void syncData() {
            LocalDateTime localDateTime = LocalDateTime.now();
            try (Connection connection = dataSource.getConnection()) {
                this.readCluster(connection);
                this.readRuntime(connection);
                this.readCollect(connection);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                handlerData(this.clusterMetadataList, this.runtimeMetadataList, this.collectMetadataList);
                paddingService.setClusterMetadata(clusterMetadataList);
                paddingService.setRuntimeMetadata(runtimeMetadataList);
                this.localDateTime = localDateTime;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                this.localDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);
            }
        }

        public void readCluster(Connection connection) throws SQLException {
            String sql = "select * from cluster where update_time >= ? ";
            this.clusterMetadataList = RowMapperUtil.executeQuery(connection, sql, List.of(this.localDateTime), ClusterMetadata.class);
        }

        public void readRuntime(Connection connection) throws SQLException {
            String sql =
                "select `id`, `organization_id`, `cluster_id`, `cluster_type`, `name`, INET_NTOA(`host`) AS `host`, `port`, `runtime_index`, "
                + "`version`, `jmx_port`, `trusteeship_type`, `first_to_whom`, `first_sync_state`, `replication_type`, `sync_error_type`,"
                + " `deploy_status_type`, `kubernetes_cluster_id`, `create_script_content`, `resources_config_id`, `deploy_script_id`,"
                + " `deploy_script_name`, `deploy_script_version`, `auth_type`, `description`, `rack`, `status`, `online_timestamp`, "
                + "`offline_timestamp`, `create_time`, `update_time`, `endpoint_map`, `is_delete` from runtime where update_time >= ? ";
            this.runtimeMetadataList = RowMapperUtil.executeQuery(connection, sql, List.of(this.localDateTime), RuntimeMetadata.class);
        }

        public void readCollect(Connection connection) throws SQLException {
            String sql = "select * from collect where update_time >= ? ";
            this.collectMetadataList = RowMapperUtil.executeQuery(connection, sql, List.of(this.localDateTime), CollectMetadata.class);
        }


    }

}
