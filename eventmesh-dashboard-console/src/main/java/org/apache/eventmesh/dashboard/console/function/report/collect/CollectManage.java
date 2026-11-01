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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

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
    // 节点名称不是唯一键，同名节点需要各自保留，更新与删除也必须使用同一 ID。
    private final Map<Long, Map<Long, RuntimeMetadata>> runtimeMetadataMap = new ConcurrentHashMap<>();

    /**
     * cluster 元数据，用于聚合操作
     */
    private final Map<Long, ClusterMetadata> clusterMetadataMap = new ConcurrentHashMap<>();

    /**
     * collect 元数据，按配置 ID 保存，以保留同一集群的不同配置。
     */
    private final Map<Long, CollectMetadata> collectMetadataMap = new ConcurrentHashMap<>();


    private final SyncAndHandlerData syncAndHandlerData = new SyncAndHandlerData();

    private final PaddingService paddingService = new PaddingService();

    private final DataSyncHandler dataSyncHandler = new DataSyncHandler();

    private final KafkaCollectFactory kafkaCollectFactory = new KafkaCollectFactory();

    private KafkaDataSyncHandler kafkaDataSyncHandler;

    @Setter
    private DruidDataSource dataSource;

    @Setter
    private ReportHandlerManage reportHandlerManage;


    public void init() {
        this.dataSyncHandler.setPaddingService(paddingService);
        this.dataSyncHandler.setReportEngine(this.reportHandlerManage.getReportEngine());
        this.kafkaDataSyncHandler = new KafkaDataSyncHandler(paddingService, this.reportHandlerManage.getReportEngine());
    }

    public void collect() {
        if (this.executeingCollectMap.isEmpty()) {
            log.info("Not executed because there is no collector to be executed ");
            return;
        }
        // Kafka 结果独立交付，沿用参数绑定以保持 IoTDB 时间与特殊字符串的写入语义。
        List<Collect> regularCollectors = new ArrayList<>();
        this.executeingCollectMap.values().forEach(collector -> {
            if (isKafkaCollector(collector)) {
                DataSyncHandlerWrapper wrapper = this.kafkaDataSyncHandler.getDataSyncHandlerWrapper(1);
                this.submitCollect(collector, wrapper.getIndex(), wrapper);
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

    private void submitCollect(Collect collector, int index, DataSyncHandlerWrapper wrapper) {
        // Kafka 使用独立批次；采集异常只影响当前任务，不阻塞其他采集器交付。
        try {
            this.threadPoolExecutor.execute(() -> {
                try {
                    collector.collect(index, wrapper);
                } catch (RuntimeException e) {
                    log.error("Collection failed for task {}", index, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Collection rejected for task {}", index, e);
        }
    }

    public void syncData() {
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

    public synchronized void handlerData(List<ClusterMetadata> clusterMetadataList, List<RuntimeMetadata> runtimeMetadataList,
        List<CollectMetadata> collectMetadataList) {
        Set<Long> factoryClusters = new HashSet<>();
        List<RuntimeException> failures = new ArrayList<>();
        // 一条元数据处理失败时继续处理其他记录，最后报告错误以保留同步游标。
        processMetadata(clusterMetadataList, "cluster", ClusterMetadata::getId, clusterMetadata -> {
            boolean inactive = clusterMetadata.isDelete() || Objects.equals(clusterMetadata.getStatus(), 0L);
            // 类型变更时，旧集群级任务可能仍属于工厂，需要清理旧节点任务。
            if (isKafkaCluster(clusterMetadata) || isKafkaCollector(this.executeingCollectMap.get(this.createCollectKey(clusterMetadata)))) {
                this.markFactoryCluster(factoryClusters, clusterMetadata.getId(),
                    !sameCluster(this.clusterMetadataMap.get(clusterMetadata.getId()), clusterMetadata));
                if (inactive) {
                    this.clearClusterTasks(clusterMetadata.getId());
                }
            }
            if (inactive) {
                this.clusterMetadataMap.remove(clusterMetadata.getId());
                this.deleteCollect(clusterMetadata);
                return;
            }
            this.clusterMetadataMap.put(clusterMetadata.getId(), clusterMetadata);
        }, failures);

        processMetadata(runtimeMetadataList, "runtime", RuntimeMetadata::getId, runtimeMetadata -> {
            Long clusterId = runtimeMetadata.getClusterId();
            boolean factoryManaged = isKafkaRuntime(runtimeMetadata) || isKafkaCluster(this.clusterMetadataMap.get(clusterId));
            // runtime ID 全表唯一；迁移后的当前行无论启停都能识别属于旧集群的任务。
            if (factoryManaged && clusterId != null) {
                String key = this.createCollectKey(runtimeMetadata);
                Collect collector = this.executeingCollectMap.get(key);
                if (collector instanceof AbstractCollect task && task.belongsToDifferentCluster(clusterId)) {
                    this.executeingCollectMap.remove(key, collector);
                }
            }
            // 原集群可能已停用并从缓存移除；迁移仍需移除其节点，否则旧配置会删除新任务。
            this.runtimeMetadataMap.forEach((previousClusterId, nodes) -> {
                if (!Objects.equals(previousClusterId, clusterId) && nodes.containsKey(runtimeMetadata.getId())
                    && (factoryManaged || !this.clusterMetadataMap.containsKey(previousClusterId)
                    || isKafkaCluster(this.clusterMetadataMap.get(previousClusterId)))) {
                    RuntimeMetadata previous = nodes.get(runtimeMetadata.getId());
                    if (Objects.equals(previous.getClusterType(), ClusterType.PROMETHEUS_EXPORTER)) {
                        // 先切换旧集群的导出器；切换失败时保留节点归属以供重试。
                        this.refreshPrometheusExporter(previousClusterId, previous.getId());
                    }
                    nodes.remove(runtimeMetadata.getId());
                    this.deleteCollect(runtimeMetadata);
                    this.markFactoryCluster(factoryClusters, previousClusterId, true);
                }
            });
            if (factoryManaged) {
                RuntimeMetadata previous = this.runtimeMetadataMap.getOrDefault(clusterId, Map.of()).get(runtimeMetadata.getId());
                boolean inactive = runtimeMetadata.isDelete() || Objects.equals(runtimeMetadata.getStatus(), 0L);
                boolean changed = inactive
                    ? previous != null || isKafkaCollector(this.executeingCollectMap.get(this.createCollectKey(runtimeMetadata)))
                    : !sameRuntime(previous, runtimeMetadata);
                this.markFactoryCluster(factoryClusters, clusterId, changed);
            }
            if (runtimeMetadata.isDelete() || Objects.equals(runtimeMetadata.getStatus(), 0L)) {
                if (this.runtimeMetadataMap.containsKey(clusterId)) {
                    this.runtimeMetadataMap.get(clusterId).remove(runtimeMetadata.getId());
                }
                if (factoryManaged) {
                    this.removeKafkaCollector(this.createCollectKey(runtimeMetadata));
                }
                return;
            }
            // 节点名称可以重复，所有采集器共用按元数据 ID 索引的缓存。
            this.runtimeMetadataMap.computeIfAbsent(clusterId, id -> new ConcurrentHashMap<>())
                .put(runtimeMetadata.getId(), runtimeMetadata);
            if (factoryManaged || !this.clusterMetadataMap.containsKey(clusterId)) {
                return;
            }
            if (Objects.equals(runtimeMetadata.getClusterType(), ClusterType.PROMETHEUS_EXPORTER)) {
                String url = runtimeMetadata.getHost() + ":" + runtimeMetadata.getPort();
                ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
                this.register(url, cluster);
                String key = this.createCollectKey(runtimeMetadata);
                if (this.executeingCollectMap.containsKey(key)) {
                    this.executeingCollectMap.put(key, this.waitExecuteCollectMap.get(key));
                }
            } else {
                // 配置以配置 ID 缓存；按集群查找，避免将 Kafka 配置关联到同 ID 的其他集群。
                CollectMetadata collectMetadata = this.collectMetadataMap.values().stream()
                    .filter(configuration -> Objects.equals(configuration.getClusterId(), clusterId))
                    .filter(configuration -> Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH))
                    .findFirst().orElse(null);
                if (Objects.isNull(collectMetadata)) {
                    return;
                }
                ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(runtimeMetadata.getClusterType());
                Class<?> clazz = CLUSTER_TYPE_CLASS.get(runtimeMetadata.getClusterType());
                if (clusterFramework.isCAP()) {
                    return;
                }
                this.buildCollect(collectMetadata, this.clusterMetadataMap.get(clusterId), runtimeMetadata, clazz);
            }
        }, failures);

        processMetadata(collectMetadataList, "collect", CollectMetadata::getId, collectMetadata -> {
            Long clusterId = collectMetadata.getClusterId();
            ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
            boolean factoryManaged = isKafkaCluster(cluster) || (cluster == null && factoryClusters.contains(clusterId));
            CollectMetadata previous = this.collectMetadataMap.get(collectMetadata.getId());
            Long previousClusterId = previous == null ? null : previous.getClusterId();
            if (previousClusterId != null && !Objects.equals(previousClusterId, clusterId)) {
                // 同一配置迁移后，原集群也必须移除或重建对应任务。
                if (isKafkaCluster(this.clusterMetadataMap.get(previousClusterId))) {
                    this.markFactoryCluster(factoryClusters, previousClusterId, true);
                } else if (factoryManaged || cluster == null) {
                    this.detachLegacyConfiguration(previous);
                }
            }
            if (factoryManaged) {
                boolean inactive = collectMetadata.isDelete() || Objects.equals(collectMetadata.getStatus(), 0L)
                    || Objects.equals(collectMetadata.getStatus(), 2L) || Boolean.FALSE.equals(collectMetadata.getEnable());
                this.markFactoryCluster(factoryClusters, clusterId,
                    inactive ? previous != null : !sameConfiguration(previous, collectMetadata));
            }
            if (collectMetadata.isDelete() || Objects.equals(collectMetadata.getStatus(), 0L)
                || Objects.equals(collectMetadata.getStatus(), 2L)
                || (factoryManaged && Boolean.FALSE.equals(collectMetadata.getEnable()))) {
                if (factoryManaged) {
                    this.collectMetadataMap.remove(collectMetadata.getId());
                    // 同一批次可能从旧 Prometheus 切换为 Kafka，已停用配置也要清理原任务。
                    this.deleteCollect(collectMetadata);
                } else {
                    this.deleteByCollectMetadata(collectMetadata);
                }
                return;
            }
            this.collectMetadataMap.put(collectMetadata.getId(), collectMetadata);
            if (factoryManaged || !this.clusterMetadataMap.containsKey(clusterId)) {
                return;
            }
            if (Objects.equals(CollectType.EVENTMESH, collectMetadata.getCollectType())) {
                this.buildCollect(collectMetadata);
                return;
            }
            if (Objects.equals(CollectType.PROMETHEUS, collectMetadata.getCollectType())) {
                String key = this.createCollectKey(collectMetadata);
                Collect collect = this.waitExecuteCollectMap.get(key);
                // 等待表可能没有该配置；空值不能进入并发任务表并阻断同批 Kafka 同步。
                if (collect != null) {
                    this.executeingCollectMap.put(key, collect);
                }
            }
        }, failures);
        // 迁移后的重放只包含新集群；从现有配置补充先前重建失败的旧集群。
        this.collectMetadataMap.values().stream()
            .filter(configuration -> Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH))
            .filter(configuration -> !configuration.isDelete() && !Boolean.FALSE.equals(configuration.getEnable()))
            .map(CollectMetadata::getClusterId)
            .filter(Objects::nonNull)
            .filter(clusterId -> isKafkaCluster(this.clusterMetadataMap.get(clusterId)))
            .filter(clusterId -> !isKafkaCollector(this.executeingCollectMap.get(ClusterMetadata.class.getSimpleName() + "-" + clusterId)))
            .forEach(factoryClusters::add);
        processMetadata(factoryClusters, "collector cluster", Function.identity(), this::rebuildKafkaCollectors, failures);
        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException("Failed to process collector metadata");
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
    }

    private <T> void processMetadata(Iterable<T> records, String type, Function<T, ?> id,
        Consumer<T> action, List<RuntimeException> failures) {
        for (T record : records) {
            try {
                action.accept(record);
            } catch (RuntimeException e) {
                failures.add(new IllegalStateException("Failed to process " + type + " metadata id " + id.apply(record), e));
            }
        }
    }

    private void markFactoryCluster(Set<Long> clusters, Long clusterId, boolean changed) {
        // 失败重试会重放已处理的行；相同配置保留实例，缺失任务则允许重试创建。
        if (changed || !isKafkaCollector(this.executeingCollectMap.get(ClusterMetadata.class.getSimpleName() + "-" + clusterId))) {
            clusters.add(clusterId);
        }
    }

    private void refreshPrometheusExporter(Long clusterId, Long excludedNodeId) {
        ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
        String key = ClusterMetadata.class.getSimpleName() + "-" + clusterId;
        // 无法从集群级缓存识别停用节点时，选择仍有效的端点，避免继续访问已停用节点。
        RuntimeMetadata remaining = this.runtimeMetadataMap.getOrDefault(clusterId, Map.of()).values().stream()
            .filter(node -> Objects.equals(node.getClusterType(), ClusterType.PROMETHEUS_EXPORTER))
            .filter(node -> !Objects.equals(node.getId(), excludedNodeId))
            .findFirst().orElse(null);
        if (cluster != null && remaining != null) {
            this.register(remaining.getHost() + ":" + remaining.getPort(), cluster);
        } else {
            this.waitExecuteCollectMap.remove(key);
        }
        Collect collector = this.waitExecuteCollectMap.get(key);
        this.collectMetadataMap.values().stream()
            .filter(configuration -> Objects.equals(configuration.getClusterId(), clusterId))
            .filter(configuration -> Objects.equals(configuration.getCollectType(), CollectType.PROMETHEUS))
            .forEach(configuration -> {
                String collectKey = this.createCollectKey(configuration);
                if (collector == null) {
                    this.executeingCollectMap.remove(collectKey);
                } else {
                    this.executeingCollectMap.put(collectKey, collector);
                }
            });
    }

    private static boolean sameCluster(ClusterMetadata previous, ClusterMetadata current) {
        return sameBase(previous, current) && Objects.equals(previous.getClusterType(), current.getClusterType())
            && Objects.equals(previous.getName(), current.getName());
    }

    private static boolean sameRuntime(RuntimeMetadata previous, RuntimeMetadata current) {
        return sameBase(previous, current) && Objects.equals(previous.getClusterId(), current.getClusterId())
            && Objects.equals(previous.getClusterType(), current.getClusterType())
            && Objects.equals(previous.getName(), current.getName()) && Objects.equals(previous.getHost(), current.getHost())
            && Objects.equals(previous.getPort(), current.getPort()) && Objects.equals(previous.getJmxPort(), current.getJmxPort());
    }

    private static boolean sameConfiguration(CollectMetadata previous, CollectMetadata current) {
        return sameBase(previous, current) && Objects.equals(previous.getClusterId(), current.getClusterId())
            && Objects.equals(previous.getClusterType(), current.getClusterType())
            && Objects.equals(previous.getCollectType(), current.getCollectType()) && Objects.equals(previous.getEnable(), current.getEnable())
            && Objects.equals(previous.getSaveTime(), current.getSaveTime())
            && Objects.equals(previous.getCollectInterval(), current.getCollectInterval())
            && Objects.equals(previous.getStorageClusterId(), current.getStorageClusterId())
            && Objects.equals(previous.getStorageClusterName(), current.getStorageClusterName())
            && Objects.equals(previous.getDefaultStorage(), current.getDefaultStorage());
    }

    private static boolean sameBase(BaseOrganizationBase previous, BaseOrganizationBase current) {
        // 调用方可能原地修改缓存对象，此时无法比较旧值，必须重新创建。
        return previous != null && previous != current && Objects.equals(previous.getId(), current.getId())
            && Objects.equals(previous.getOrganizationId(), current.getOrganizationId())
            && Objects.equals(previous.getStatus(), current.getStatus()) && Objects.equals(previous.getIsDelete(), current.getIsDelete())
            && Objects.equals(previous.getCreateTime(), current.getCreateTime())
            && Objects.equals(previous.getCreateUserId(), current.getCreateUserId())
            && Objects.equals(previous.getUpdateTime(), current.getUpdateTime())
            && Objects.equals(previous.getUpdateUserId(), current.getUpdateUserId())
            && Objects.equals(previous.getSyncStatus(), current.getSyncStatus());
    }

    private void detachLegacyConfiguration(CollectMetadata previous) {
        if (Objects.equals(previous.getCollectType(), CollectType.PROMETHEUS)) {
            this.deleteCollect(previous);
            return;
        }
        Long clusterId = previous.getClusterId();
        this.runtimeMetadataMap.getOrDefault(clusterId, Map.of()).values().forEach(this::deleteCollect);
        ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
        if (cluster != null) {
            this.executeingCollectMap.remove(this.createCollectKey(cluster));
        }
        // 节点任务按节点而非配置键保存；原集群若还有配置，需要为其恢复任务。
        this.collectMetadataMap.values().stream()
            .filter(configuration -> !Objects.equals(configuration.getId(), previous.getId()))
            .filter(configuration -> Objects.equals(configuration.getClusterId(), clusterId))
            .filter(configuration -> Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH))
            .forEach(this::buildCollect);
    }

    public synchronized void deleteByCollectMetadata(CollectMetadata collectMetadata) {
        // 配置缓存按配置 ID 存储；原先按集群 ID 删除可能误删另一个配置。
        this.collectMetadataMap.remove(collectMetadata.getId());
        if (isKafkaCluster(this.clusterMetadataMap.get(collectMetadata.getClusterId()))) {
            this.rebuildKafkaCollectors(collectMetadata.getClusterId());
            return;
        }
        if (Objects.equals(collectMetadata.getCollectType(), CollectType.PROMETHEUS)) {
            this.deleteCollect(collectMetadata);
            return;
        }
        Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataMap.get(collectMetadata.getClusterId());
        if (Objects.nonNull(nodes)) {
            nodes.values().forEach(this::deleteCollect);
            return;
        }
        this.deleteCollect(this.clusterMetadataMap.get(collectMetadata.getClusterId()));
    }

    private void buildCollect(CollectMetadata collectMetadata) {
        ClusterMetadata clusterMetadata = this.clusterMetadataMap.get(collectMetadata.getClusterId());
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterMetadata.getClusterType());
        Class<?> clazz = CLUSTER_TYPE_CLASS.get(clusterMetadata.getClusterType());
        if (clusterFramework.isCAP()) {
            this.buildCollect(collectMetadata, clusterMetadata, null, clazz);
            return;
        }
        // 其他集群可能先收到配置而尚无节点，不能因此阻断 Kafka 的同批同步。
        Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataMap.getOrDefault(clusterMetadata.getId(), Map.of());
        nodes.values().forEach(runtimeMetadata -> this.buildCollect(collectMetadata, clusterMetadata, runtimeMetadata, clazz));
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

    private void rebuildKafkaCollectors(Long clusterId) {
        String clusterKey = ClusterMetadata.class.getSimpleName() + "-" + clusterId;
        Map<Long, RuntimeMetadata> nodes = this.runtimeMetadataMap.getOrDefault(clusterId, Map.of());
        ClusterMetadata cluster = this.clusterMetadataMap.get(clusterId);
        if (!isKafkaCluster(cluster)) {
            this.removeKafkaCollector(clusterKey);
            nodes.values().forEach(node -> this.removeKafkaCollector(this.createCollectKey(node)));
            return;
        }
        // 集群类型可能刚切换；新任务发布前清理同一集群下原类型的任务和导出器。
        this.clearClusterTasks(clusterId);
        // 集群级与节点级任务的差异由工厂处理；元数据与任务表仍由 CollectManage 统一维护。
        // 全部配置创建成功后再发布，失败时保留缺失状态以便下一轮重试。
        Map<String, Collect> collectors = new HashMap<>();
        this.collectMetadataMap.values().stream()
            .filter(configuration -> Objects.equals(configuration.getClusterId(), clusterId))
            .filter(configuration -> Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH))
            .filter(configuration -> !configuration.isDelete() && !Boolean.FALSE.equals(configuration.getEnable()))
            .forEach(configuration -> collectors.putAll(
                this.kafkaCollectFactory.createCollectors(configuration, cluster, new ArrayList<>(nodes.values()))));
        this.executeingCollectMap.putAll(collectors);
    }

    private void clearClusterTasks(Long clusterId) {
        String clusterKey = ClusterMetadata.class.getSimpleName() + "-" + clusterId;
        this.executeingCollectMap.remove(clusterKey);
        this.waitExecuteCollectMap.remove(clusterKey);
        this.runtimeMetadataMap.getOrDefault(clusterId, Map.of()).values().forEach(node -> {
            String key = this.createCollectKey(node);
            Collect collector = this.executeingCollectMap.get(key);
            // 节点可能已迁移；已知采集器必须核对归属，未知实现保留原有按节点键清理行为。
            if (collector != null && (!(collector instanceof AbstractCollect task) || task.belongsToCluster(clusterId))) {
                this.executeingCollectMap.remove(key, collector);
            }
        });
        // 已停用节点不在元数据缓存中，但其旧采集任务仍需随所属集群移除。
        this.executeingCollectMap.forEach((key, collector) -> {
            if (collector instanceof AbstractCollect task && task.belongsToCluster(clusterId)) {
                this.executeingCollectMap.remove(key, collector);
            }
        });
        this.collectMetadataMap.values().stream()
            .filter(configuration -> Objects.equals(configuration.getClusterId(), clusterId))
            .forEach(configuration -> this.executeingCollectMap.remove(this.createCollectKey(configuration)));
    }

    private void removeKafkaCollector(String key) {
        Collect collector = this.executeingCollectMap.get(key);
        if (isKafkaCollector(collector)) {
            this.executeingCollectMap.remove(key, collector);
        }
    }

    private static boolean isKafkaCollector(Collect collector) {
        return collector instanceof KafkaAdminCollect || collector instanceof KafkaJmxCollect;
    }

    private static boolean isKafkaCluster(ClusterMetadata cluster) {
        return cluster != null && Objects.equals(cluster.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER);
    }

    private static boolean isKafkaRuntime(RuntimeMetadata runtime) {
        return Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_BROKER)
            || Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_RAFT);
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
            // 数据库更新时间按秒存储；保留一秒重叠以免漏掉同秒内稍后的更新。
            LocalDateTime nextSync = LocalDateTime.now().minusSeconds(1);
            List<Exception> failures = new ArrayList<>();
            try (Connection connection = dataSource.getConnection()) {
                this.readCluster(connection, failures);
                this.readRuntime(connection, failures);
                this.readCollect(connection, failures);
                try {
                    handlerData(this.clusterMetadataList, this.runtimeMetadataList, this.collectMetadataList);
                } catch (RuntimeException e) {
                    failures.add(e);
                }
                paddingService.setClusterMetadata(clusterMetadataList);
                paddingService.setRuntimeMetadata(runtimeMetadataList);
                if (!failures.isEmpty()) {
                    IllegalStateException failure = new IllegalStateException("Failed to synchronize collector metadata rows");
                    failures.forEach(failure::addSuppressed);
                    throw failure;
                }
                this.localDateTime = nextSync;
            } catch (Exception e) {
                // 三类数据任一读取或处理失败都保持游标，下一轮重试同一批次。
                log.error("Failed to synchronize collector metadata", e);
            }
        }

        private void readCluster(Connection connection, List<Exception> failures) throws SQLException {
            String sql = "select * from cluster where update_time >= ? ";
            this.clusterMetadataList = this.readRows(connection, sql, ClusterMetadata.class, failures);
        }

        private void readRuntime(Connection connection, List<Exception> failures) throws SQLException {
            String sql =
                "select `id`, `organization_id`, `cluster_id`, `cluster_type`, `name`, INET_NTOA(`host`) AS `host`, `port`, `runtime_index`, "
                + "`version`, case when `status` = 0 or `is_delete` <> 0 then null else nullif(trim(`jmx_port`), '') end as `jmx_port`, "
                + "`trusteeship_type`, `first_to_whom`, `first_sync_state`, `replication_type`, `sync_error_type`,"
                + " `deploy_status_type`, `kubernetes_cluster_id`, `create_script_content`, `resources_config_id`, `deploy_script_id`,"
                + " `deploy_script_name`, `deploy_script_version`, `auth_type`, `description`, `rack`, `status`, `online_timestamp`, "
                + "`offline_timestamp`, `create_time`, `update_time`, `endpoint_map`, `is_delete` from runtime where update_time >= ? ";
            this.runtimeMetadataList = this.readRows(connection, sql, RuntimeMetadata.class, failures);
        }

        private void readCollect(Connection connection, List<Exception> failures) throws SQLException {
            // 存储集群名称在表中为文本，元数据模型当前声明为数字且采集链不使用；仅读取采集所需列。
            String sql = "select id, cluster_id, collect_type, save_time, collect_interval, storage_cluster_id, enable as collect_enable, "
                + "status, create_time, update_time, is_delete from collect where update_time >= ? ";
            this.collectMetadataList = this.readRows(connection, sql, CollectMetadata.class, failures);
        }

        private <T> List<T> readRows(Connection connection, String sql, Class<T> type, List<Exception> failures) throws SQLException {
            List<T> result = new ArrayList<>();
            connection.setAutoCommit(true);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, this.localDateTime);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        Object rowId = rows.getObject("id");
                        try {
                            T row = RowMapperUtil.mapRow(rows, type);
                            // 采集表以整数保存启用状态，使用 JDBC 类型转换，不改变公共行映射规则。
                            if (row instanceof CollectMetadata collect) {
                                boolean enabled = rows.getBoolean("collect_enable");
                                collect.setEnable(rows.wasNull() ? null : enabled);
                            }
                            // Kafka JMX 使用端口建立连接，保留旧数字映射的同时避免截断并连接错误端口。
                            if (row instanceof RuntimeMetadata runtime && isKafkaRuntime(runtime)) {
                                String port = rows.getString("jmx_port");
                                if (port != null && !port.isBlank()) {
                                    Integer.parseInt(port.trim());
                                }
                            }
                            result.add(row);
                        } catch (Exception e) {
                            // 保留可读的记录并重试失败行，不能因一行格式错误阻断整个同步批次。
                            failures.add(new SQLException("Failed to map " + type.getSimpleName() + " metadata id " + rowId, e));
                        }
                    }
                }
            }
            return result;
        }


    }

}
