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

package org.apache.eventmesh.dashboard.console.function.report.collect.factory;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.Collect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 每个 Kafka 集群创建一个 Admin 采集任务，每个已配置 JMX 端口的有效 Broker 创建一个 JMX 采集任务。
 */
public class KafkaCollectFactory {

    /**
     * 根据统一缓存的完整节点列表创建任务，任务表由 CollectManage 维护。
     */
    public Map<String, Collect> createCollectors(CollectMetadata collectMetadata, ClusterMetadata clusterMetadata,
        List<RuntimeMetadata> runtimeMetadataList) {
        Map<String, Collect> collectors = new HashMap<>();
        // collect 配置不提供可靠的类型，按真实集群分派，避免为其他类型创建 Kafka 任务。
        if (!Objects.equals(clusterMetadata.getClusterType(), ClusterType.STORAGE_KAFKA_CLUSTER)) {
            return collectors;
        }
        List<RuntimeMetadata> brokers = new ArrayList<>();
        for (RuntimeMetadata runtime : runtimeMetadataList) {
            // 上游将 status 与 isDelete 分离，停用节点仍不能注册 JMX 或进入 Admin 的地址列表。
            if (!Objects.equals(runtime.getClusterId(), clusterMetadata.getId()) || runtime.isDelete() || Objects.equals(runtime.getStatus(), 0L)
                || (!Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_BROKER)
                && !Objects.equals(runtime.getClusterType(), ClusterType.STORAGE_KAFKA_RAFT))) {
                continue;
            }
            brokers.add(runtime);
            if (runtime.getJmxPort() == null || runtime.getJmxPort() <= 0) {
                continue;
            }
            KafkaJmxCollect jmx = new KafkaJmxCollect();
            jmx.setCollectMetadata(collectMetadata);
            jmx.setClusterMetadata(clusterMetadata);
            jmx.setRuntimeMetadata(runtime);
            collectors.put(RuntimeMetadata.class.getSimpleName() + "-" + runtime.getId(), jmx);
        }
        // 缓存遍历顺序不固定，使用稳定节点顺序生成集群 bootstrap 列表。
        brokers.sort(Comparator.comparing(RuntimeMetadata::getId));
        KafkaAdminCollect admin = new KafkaAdminCollect();
        admin.setCollectMetadata(collectMetadata);
        admin.setClusterMetadata(clusterMetadata);
        admin.setBrokers(brokers);
        collectors.put(ClusterMetadata.class.getSimpleName() + "-" + clusterMetadata.getId(), admin);
        return collectors;
    }
}
