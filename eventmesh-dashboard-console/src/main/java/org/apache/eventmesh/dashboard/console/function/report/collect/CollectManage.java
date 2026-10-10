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
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.common.utils.RowMapperUtil;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.CollectExporter;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Setter
    private DruidDataSource dataSource;

    @Setter
    private ReportHandlerManage reportHandlerManage;


    public void init() {
        this.dataSyncHandler.setPaddingService(paddingService);
        this.dataSyncHandler.setReportEngine(this.reportHandlerManage.getReportEngine());
    }

    public void collect() {
        if (this.executeingCollectMap.isEmpty()) {
            log.info("Not executed because there is no collector to be executed ");
            return;
        }
        DataSyncHandlerWrapper wrapper = dataSyncHandler.getDataSyncHandlerWrapper(executeingCollectMap.size());
        this.executeingCollectMap.forEach(
            (clusterId, exporter) -> this.threadPoolExecutor.execute(() -> exporter.collect(wrapper.getIndex(), wrapper)));
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

    public void handlerData(List<ClusterMetadata> clusterMetadataList, List<RuntimeMetadata> runtimeMetadataList,
        List<CollectMetadata> collectMetadataList) {
        clusterMetadataList.forEach((clusterMetadata) -> {
            if (clusterMetadata.isDelete()) {
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
            if (runtimeMetadata.isDelete()) {
                runtimeMetadataMap.get(runtimeMetadata.getClusterId()).remove(runtimeMetadata.getName());
                return;
            }
            runtimeMetadataMap.computeIfAbsent(runtimeMetadata.getClusterId(), id -> new ConcurrentHashMap<>())
                .put(runtimeMetadata.getName(), runtimeMetadata);
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
            if (collectMetadata.isDelete() || collectMetadata.getStatus() == 2) {
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

    private String createCollectKey(BaseClusterIdBase base) {
        return base.getClass().getSimpleName() + "-" + base.getId();
    }

    private void deleteCollect(BaseClusterIdBase base) {
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
