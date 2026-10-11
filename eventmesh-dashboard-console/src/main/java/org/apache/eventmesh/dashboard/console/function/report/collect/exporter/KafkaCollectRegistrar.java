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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.Collect;
import org.apache.eventmesh.dashboard.console.function.report.collect.factory.KafkaCollectFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 维护 Kafka 主动采集的配置、节点缓存和任务，公共管理器只负责传入元数据增量。
 */
public class KafkaCollectRegistrar {

    private final Map<Long, ClusterMetadata> clusters;

    private final Map<String, Collect> collectors;

    private final Map<Long, Map<Long, RuntimeMetadata>> runtimes = new HashMap<>();

    private final Map<Long, CollectMetadata> configurations = new HashMap<>();

    private final Map<Long, Set<String>> registeredKeys = new HashMap<>();

    private final KafkaCollectFactory factory = new KafkaCollectFactory();

    public KafkaCollectRegistrar(Map<Long, ClusterMetadata> clusters, Map<String, Collect> collectors) {
        this.clusters = clusters;
        this.collectors = collectors;
    }

    /**
     * 判断元数据类型是否属于 Kafka，避免进入原有的逐节点注册流程。
     */
    public boolean supports(ClusterType clusterType) {
        return Objects.equals(clusterType, ClusterType.STORAGE_KAFKA_CLUSTER)
            || Objects.equals(clusterType, ClusterType.STORAGE_KAFKA_BROKER)
            || Objects.equals(clusterType, ClusterType.STORAGE_KAFKA_RAFT)
            || Objects.equals(clusterType, ClusterType.STORAGE_KAFKA_ZK);
    }

    /**
     * collect 表没有 cluster_type 列，优先通过集群缓存确定配置所属类型。
     */
    public synchronized boolean supportsConfiguration(CollectMetadata configuration) {
        ClusterMetadata cluster = clusters.get(configuration.getClusterId());
        return (cluster != null && supports(cluster.getClusterType())) || supports(configuration.getClusterType())
            || registeredKeys.containsKey(configuration.getClusterId());
    }

    /**
     * 合并 Kafka 节点和配置增量，仅重建受影响集群的任务。
     */
    public synchronized void refresh(List<ClusterMetadata> clusterUpdates, List<RuntimeMetadata> runtimeUpdates,
        List<CollectMetadata> configurationUpdates) {
        Set<Long> changedClusterIds = new HashSet<>();
        clusterUpdates.forEach(cluster -> {
            if (!supports(cluster.getClusterType())) {
                return;
            }
            changedClusterIds.add(cluster.getId());
            if (cluster.isDelete()) {
                remove(cluster.getId());
                runtimes.remove(cluster.getId());
            }
        });
        runtimeUpdates.forEach(runtime -> {
            if (!supports(runtime.getClusterType())) {
                return;
            }
            Long clusterId = runtime.getClusterId();
            changedClusterIds.add(clusterId);
            // 按稳定的节点 ID 缓存，节点改名不会产生重复采集任务。
            Map<Long, RuntimeMetadata> nodes = runtimes.computeIfAbsent(clusterId, id -> new HashMap<>());
            if (runtime.isDelete()) {
                nodes.remove(runtime.getId());
            } else {
                nodes.put(runtime.getId(), runtime);
            }
        });
        configurationUpdates.forEach(configuration -> {
            Long clusterId = configuration.getClusterId();
            // 保存配置增量以兼容配置先于集群到达；创建任务时仍严格检查 Kafka 集群类型。
            if (configuration.isDelete() || configuration.getStatus() == 2
                || !Objects.equals(configuration.getCollectType(), CollectType.EVENTMESH)) {
                configurations.remove(clusterId);
            } else {
                configurations.put(clusterId, configuration);
            }
            changedClusterIds.add(clusterId);
        });
        changedClusterIds.forEach(this::refreshCluster);
    }

    private void refreshCluster(Long clusterId) {
        ClusterMetadata cluster = clusters.get(clusterId);
        CollectMetadata configuration = configurations.get(clusterId);
        if (cluster == null || !Objects.equals(cluster.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER) || configuration == null) {
            clearCollectors(clusterId);
            return;
        }
        List<RuntimeMetadata> brokers = new ArrayList<>(runtimes.getOrDefault(clusterId, Map.of()).values());
        brokers.sort(Comparator.comparing(RuntimeMetadata::getId));
        Map<String, Collect> tasks = factory.createCollectors(configuration, cluster, brokers);
        clearCollectors(clusterId);
        collectors.putAll(tasks);
        registeredKeys.put(clusterId, new HashSet<>(tasks.keySet()));
    }

    /**
     * 删除 Kafka 采集配置及其任务，保留节点缓存供重新启用时使用。
     */
    public synchronized void remove(Long clusterId) {
        configurations.remove(clusterId);
        clearCollectors(clusterId);
    }

    private void clearCollectors(Long clusterId) {
        Set<String> keys = registeredKeys.remove(clusterId);
        if (keys != null) {
            keys.forEach(collectors::remove);
        }
    }
}
