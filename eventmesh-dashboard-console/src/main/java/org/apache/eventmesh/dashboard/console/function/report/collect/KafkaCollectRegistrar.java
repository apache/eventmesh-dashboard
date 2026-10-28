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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.factory.KafkaCollectFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/** 根据现有元数据增量维护 Kafka 集群和节点的采集任务。 */
final class KafkaCollectRegistrar {

    private final Map<Long, ClusterMetadata> clusters = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, RuntimeMetadata>> runtimes = new ConcurrentHashMap<>();
    private final Map<Long, CollectMetadata> configurations = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Collect>> collectors = new ConcurrentHashMap<>();
    private final KafkaCollectFactory factory = new KafkaCollectFactory();
    private LocalDateTime lastSync = LocalDateTime.of(2000, 1, 1, 0, 0);

    synchronized void syncData(Connection connection) throws SQLException {
        final LocalDateTime nextSync = LocalDateTime.now().minusSeconds(1);
        Set<Long> knownIds = new HashSet<>(clusters.keySet());
        knownIds.addAll(runtimes.keySet());
        knownIds.addAll(configurations.keySet());
        List<ClusterMetadata> clusterUpdates = KafkaMetadataReader.readClusters(connection, lastSync, knownIds);
        clusterUpdates.stream().filter(KafkaCollectRegistrar::isKafkaCluster).map(ClusterMetadata::getId).forEach(knownIds::add);
        List<RuntimeMetadata> runtimeUpdates = KafkaMetadataReader.readRuntimes(connection, lastSync, knownIds);
        List<CollectMetadata> configurationUpdates = KafkaMetadataReader.readConfigurations(connection, lastSync, knownIds);
        handlerData(clusterUpdates, runtimeUpdates, configurationUpdates);
        lastSync = nextSync;
    }

    static boolean isKafkaCluster(ClusterMetadata cluster) {
        return Objects.equals(cluster.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER);
    }

    static boolean isKafkaRuntime(RuntimeMetadata runtime) {
        return Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_BROKER)
            || Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_RAFT);
    }

    boolean handlesRuntime(RuntimeMetadata runtime) {
        return isKafkaRuntime(runtime) || clusters.containsKey(runtime.getClusterId()) || runtimes.containsKey(runtime.getClusterId());
    }

    boolean isKafkaConfiguration(CollectMetadata configuration) {
        Long clusterId = configuration.getClusterId();
        return Objects.equals(configuration.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER)
            || clusters.containsKey(clusterId) || runtimes.containsKey(clusterId) || configurations.containsKey(clusterId);
    }

    synchronized void handlerData(List<ClusterMetadata> clusterUpdates, List<RuntimeMetadata> runtimeUpdates,
        List<CollectMetadata> configurationUpdates) {
        Set<Long> changed = new HashSet<>();
        for (ClusterMetadata cluster : clusterUpdates) {
            Long clusterId = cluster.getId();
            if (isKafkaCluster(cluster)) {
                clusters.put(clusterId, cluster);
                changed.add(clusterId);
            } else if (clusters.remove(clusterId) != null) {
                runtimes.remove(clusterId);
                configurations.remove(clusterId);
                collectors.remove(clusterId);
            }
        }
        for (RuntimeMetadata runtime : runtimeUpdates) {
            if (!isKafkaRuntime(runtime)) {
                Map<Long, RuntimeMetadata> nodes = runtimes.get(runtime.getClusterId());
                if (nodes != null && nodes.remove(runtime.getId()) != null) {
                    changed.add(runtime.getClusterId());
                }
                continue;
            }
            Long clusterId = runtime.getClusterId();
            Map<Long, RuntimeMetadata> nodes = runtimes.computeIfAbsent(clusterId, key -> new ConcurrentHashMap<>());
            if (runtime.isDelete() || Objects.equals(runtime.getStatus(), 0L)) {
                nodes.remove(runtime.getId());
            } else {
                nodes.put(runtime.getId(), runtime);
            }
            changed.add(clusterId);
        }
        for (CollectMetadata configuration : configurationUpdates) {
            if (isKafkaConfiguration(configuration)) {
                configurations.put(configuration.getClusterId(), configuration);
                changed.add(configuration.getClusterId());
            }
        }
        for (Long clusterId : changed) {
            ClusterMetadata cluster = clusters.get(clusterId);
            CollectMetadata configuration = configurations.get(clusterId);
            if (cluster == null || cluster.isDelete() || Objects.equals(cluster.getStatus(), 0L)
                || configuration == null || configuration.isDelete() || Objects.equals(configuration.getStatus(), 0L)
                || Objects.equals(configuration.getStatus(), 2L) || Boolean.FALSE.equals(configuration.getEnable())
                || !Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH)) {
                collectors.remove(clusterId);
                continue;
            }
            collectors.put(clusterId, factory.createCollectors(configuration, cluster,
                new ArrayList<>(runtimes.getOrDefault(clusterId, Map.of()).values())));
        }
    }

    synchronized void deleteConfiguration(CollectMetadata configuration) {
        configurations.remove(configuration.getClusterId());
        collectors.remove(configuration.getClusterId());
    }

    void collect(Executor executor, KafkaDataSyncHandler handler) {
        for (Map<String, Collect> clusterCollectors : collectors.values()) {
            for (Collect collector : clusterCollectors.values()) {
                // 每个 Kafka 任务独立交付，现有异步批次不会受到节点完成顺序影响。
                DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(1);
                executor.execute(() -> collector.collect(0, wrapper));
            }
        }
    }
}
