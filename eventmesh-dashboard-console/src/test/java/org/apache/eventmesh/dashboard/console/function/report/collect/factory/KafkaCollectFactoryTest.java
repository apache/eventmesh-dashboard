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

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaCollectFactoryTest {

    @Test
    public void createsClusterTaskAndJmxTasksOnlyForEligibleKafkaNodes() throws IllegalAccessException {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        // Factory 按真实集群类型分派，配置对象无需提供类型。
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        RuntimeMetadata broker = runtime(12L, ClusterType.STORAGE_KAFKA_BROKER, 9999);
        RuntimeMetadata raft = runtime(13L, ClusterType.STORAGE_KAFKA_RAFT, 10000);
        RuntimeMetadata withoutJmx = runtime(14L, ClusterType.STORAGE_KAFKA_BROKER, null);
        RuntimeMetadata deleted = runtime(15L, ClusterType.STORAGE_KAFKA_BROKER, 9999);
        // 独立覆盖上游分离后的逻辑删除与业务停用，不能只用 status 代表删除。
        deleted.setIsDelete(1);
        RuntimeMetadata stopped = runtime(18L, ClusterType.STORAGE_KAFKA_BROKER, 9999);
        stopped.setStatus(0L);
        RuntimeMetadata unrelated = runtime(16L, ClusterType.STORAGE_KAFKA_ZK, 9999);
        RuntimeMetadata foreign = runtime(17L, ClusterType.STORAGE_KAFKA_BROKER, 9999);
        foreign.setClusterId(21L);

        Map<String, Collect> tasks = new KafkaCollectFactory().createCollectors(new CollectMetadata(), cluster,
            List.of(broker, raft, withoutJmx, deleted, stopped, unrelated, foreign));

        assertEquals(3, tasks.size());
        assertTrue(tasks.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
        assertTrue(tasks.get("RuntimeMetadata-13") instanceof KafkaJmxCollect);
        assertEquals(List.of(broker, raft, withoutJmx), FieldUtils.readField(tasks.get("ClusterMetadata-11"), "brokers", true));
    }

    @Test
    public void createsExactlyOneAdminForEmptyKafkaCluster() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        Map<String, Collect> tasks = new KafkaCollectFactory().createCollectors(new CollectMetadata(), cluster, List.of());
        assertEquals(1, tasks.size());
        assertTrue(tasks.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
    }

    @Test
    public void ignoresNonKafkaClusters() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        Map<String, Collect> tasks = new KafkaCollectFactory().createCollectors(new CollectMetadata(), cluster,
            List.of(runtime(12L, ClusterType.STORAGE_KAFKA_BROKER, 9999)));
        assertTrue(tasks.isEmpty());
    }

    private RuntimeMetadata runtime(long id, ClusterType clusterType, Integer jmxPort) {
        RuntimeMetadata metadata = new RuntimeMetadata();
        metadata.setId(id);
        metadata.setClusterId(11L);
        metadata.setStatus(1L);
        metadata.setClusterType(clusterType);
        metadata.setJmxPort(jmxPort);
        return metadata;
    }
}
