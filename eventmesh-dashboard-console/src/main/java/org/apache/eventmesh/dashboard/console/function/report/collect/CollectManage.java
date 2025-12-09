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
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.active.AbstractMetadataCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.CollectExporter;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import java.util.concurrent.TimeUnit;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * 一个集群只允许一个模式，因为 PROMETHEUS是集群采集. 因为 prometheus 有两个模式 1. rocketmq 是多注册中心，可以采集多集群 。只能绑定到一个集群里面。 <p> 1. TODO 如果多个集群共享 多个注册中心  然后有多个
 * prometheus，会出现重复采集问题。 <p> 所以共享 meta 集群不允许 使用 PROMETHEUS 模式。只允许使用 EVENTMESH  <p> 2. meta 集群 创建的时候可以默认创建 PROMETHEUS   <p> 3. meta 集群节点发生更新的时候，也需要更新
 * PROMETHEUS  <p> 4. 提供重启功能  <p> 5. 只允许手动 与 私有 meta 集群被差集   <p> 2. kafka 单注册单集群，可以绑定 kafka 集群上面  <p> 3. 查询 collect 表信息，创建 cluster 的时候，会默认有 collect 配置
 * <p> 4. 定时同步 collect ， runtime 信息。 collect 用于触发 collect 行为， <p> runtime信息 用于触发 runtime 差集
 */
@Slf4j
public class CollectManage {

    private static Map<ClusterType, String> CLUSTER_TYPE_URL = new HashMap<>();

    private static Map<ClusterType, Class<?>> CLUSTER_TYPE_CLASS = new HashMap<>();

    static {
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(CollectManage.class).subPath("/active/**").annotationSet(Set.of(ClusterTypeMark.class)).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(CollectManage::handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private DruidDataSource dataSource;

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 100, 5, TimeUnit.HOURS, new LinkedBlockingQueue<>());

    private Map<Long, CollectExporter> collectExporterMap = new ConcurrentHashMap<>();

    private Map<Long, Collect> collectMap = new ConcurrentHashMap<>();

    private Map<Long, AbstractMetadataCollect<Object>> collectCacheMap = new ConcurrentHashMap<>();

    private Map<Long, AbstractMetadataCollect<Object>> collectExecuteMap = new ConcurrentHashMap<>();


    @Setter
    private ReportHandlerManage reportHandlerManage;

    private static void handler(Class<?> clazz) {
        CLUSTER_TYPE_CLASS.put(clazz.getAnnotation(ClusterTypeMark.class).clusterType(), clazz);
    }

    public void request() {
        this.collectExporterMap.forEach((clusterId, exporter) -> {
            this.threadPoolExecutor.execute(exporter::request);
        });
    }


    public List<Time> getData() {
        List<Time> timeList = new ArrayList<>();
        this.collectExporterMap.forEach((clusterId, exporter) -> {
            timeList.addAll(exporter.collect());
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
        collectExporterMap.put(clusterMetadata.getId(), collectExporter);

    }


    public void unregister(String url, ClusterMetadata clusterMetadata) {
        this.collectExporterMap.remove(clusterMetadata.getId());
    }


    private class SyncAndHandlerData {

        private LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);

        private List<ClusterMetadata> clusterMetadataList = new ArrayList<>();

        private List<RuntimeMetadata> runtimeMetadataList = new ArrayList<>();

        private List<CollectMetadata> collectMetadataList = new ArrayList<>();

        private Map<Long, Map<Long, RuntimeMetadata>> runtimeMetadataMap = new ConcurrentHashMap<>();

        private Map<Long, ClusterMetadata> clusterMetadataMap = new ConcurrentHashMap<>();

        public void syncData() {
            LocalDateTime localDateTime = LocalDateTime.now();

            try (Connection connection = dataSource.getConnection()) {
                this.readCluster(connection);
                this.readRuntime(connection);
                this.readCollect(connection);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            this.localDateTime = localDateTime;
            this.handlerData();
            this.clusterMetadataList.clear();
            this.runtimeMetadataList.clear();
            this.collectMetadataList.clear();
            try (Connection connection = dataSource.getConnection()) {
                connection.isReadOnly();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }

        /**
         *
         */
        public void handlerData() {
            this.clusterMetadataList.forEach((clusterMetadata) -> {
                if (clusterMetadata.isDelete()) {
                    collectCacheMap.remove(clusterMetadata.getId());
                    collectExecuteMap.remove(clusterMetadata.getId());
                    clusterMetadataMap.remove(clusterMetadata.getId());
                    collectExporterMap.remove(clusterMetadata.getId());
                    return;
                }
                clusterMetadataMap.put(clusterMetadata.getId(), clusterMetadata);
            });
            // clusterId , runtimeMetadata list
            // 需要全部缓存，因为 collect 可以从 普罗米修斯修改成 eventmesh
            this.runtimeMetadataList.forEach(runtimeMetadata -> {
                if (Objects.equals(runtimeMetadata.getClusterType(), ClusterType.PROMETHEUS_EXPORTER)) {
                    if (runtimeMetadata.isInsert() || runtimeMetadata.isUpdate()) {
                        String url = runtimeMetadata.getHost() + ":" + runtimeMetadata.getPort();
                        register(url, clusterMetadataMap.get(runtimeMetadata.getClusterId()));
                    } else if (runtimeMetadata.isDelete()) {
                        collectExporterMap.remove(runtimeMetadata.getClusterId());
                    }
                    return;
                }
                AbstractMetadataCollect<Object> abstractMetadataCollect = collectCacheMap.get(runtimeMetadata.getClusterId());
                Map<Long, RuntimeMetadata> runtimeMetadataHashMap =
                    runtimeMetadataMap.computeIfAbsent(runtimeMetadata.getClusterId(), k -> new HashMap<>());
                if (runtimeMetadata.isInsert() || runtimeMetadata.isUpdate()) {
                    runtimeMetadataHashMap.put(runtimeMetadata.getId(), runtimeMetadata);
                } else if (runtimeMetadata.isDelete()) {
                    runtimeMetadataHashMap.remove(runtimeMetadata.getId());
                }
                abstractMetadataCollect.setRuntimeMetadataList(new ArrayList<>(runtimeMetadataHashMap.values()));
            });

            this.collectMetadataList.forEach(collectMetadata -> {
                if (Objects.equals(CollectType.EVENTMESH, collectMetadata.getCollectType())) {
                    collectExecuteMap.put(collectMetadata.getClusterId(), collectCacheMap.get(collectMetadata.getClusterId()));
                } else if (Objects.equals(CollectType.PROMETHEUS, collectMetadata.getCollectType())
                           || Objects.equals(CollectType.NONE, collectMetadata.getCollectType())) {
                    collectExecuteMap.remove(collectMetadata.getClusterId());
                }
            });
        }

        public void readCluster(Connection connection) {
            String sql = "select * from cluster where update_time >= ? ";
            try (PreparedStatement p = connection.prepareStatement(sql)) {
                p.setObject(1, localDateTime);
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        ClusterMetadata clusterMetadata = new ClusterMetadata();
                        this.clusterMetadataList.add(clusterMetadata);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        public void readRuntime(Connection connection) {
            String sql = "select * from runtime where update_time >= ? ";
            try (PreparedStatement p = connection.prepareStatement(sql)) {
                p.setObject(1, localDateTime);
                try (ResultSet rs = p.executeQuery()) {
                    rs.getMetaData();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        public void readCollect(Connection connection) {
            String sql = "select * from collect where update_time >= ? ";
            try (PreparedStatement p = connection.prepareStatement(sql)) {
                p.setObject(1, localDateTime);
                try (ResultSet rs = p.executeQuery()) {
                    rs.getMetaData();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

}
