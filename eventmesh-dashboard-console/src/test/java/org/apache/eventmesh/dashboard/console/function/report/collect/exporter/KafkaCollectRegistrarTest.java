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
import org.apache.eventmesh.dashboard.console.function.report.collect.Collect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class KafkaCollectRegistrarTest {

    @Test
    public void removesOnlyKafkaTasksWhenCollectionModeChanges() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        cluster.setStatus(1L);
        CollectMetadata configuration = new CollectMetadata();
        configuration.setId(13L);
        configuration.setClusterId(11L);
        configuration.setCollectType(CollectType.EVENTMESH);
        configuration.setStatus(1L);
        Map<String, Collect> tasks = new HashMap<>();
        Collect unrelated = mock(Collect.class);
        tasks.put("RuntimeMetadata-99", unrelated);
        KafkaCollectRegistrar registrar = new KafkaCollectRegistrar(Map.of(11L, cluster), tasks);
        registrar.refresh(List.of(cluster), List.of(), List.of(configuration));
        assertEquals(2, tasks.size());
        assertTrue(tasks.get("ClusterMetadata-11") instanceof KafkaAdminCollect);

        configuration.setCollectType(CollectType.PROMETHEUS);
        registrar.refresh(List.of(), List.of(), List.of(configuration));
        assertEquals(Map.of("RuntimeMetadata-99", unrelated), tasks);
    }

    @Test
    public void ignoresNonKafkaConfigurationsWithoutTouchingTheirTasks() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(21L);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        cluster.setStatus(1L);
        CollectMetadata configuration = new CollectMetadata();
        configuration.setId(23L);
        configuration.setClusterId(21L);
        configuration.setCollectType(CollectType.EVENTMESH);
        configuration.setStatus(1L);
        Map<String, Collect> tasks = new HashMap<>();
        Collect unrelated = mock(Collect.class);
        tasks.put("RuntimeMetadata-22", unrelated);
        KafkaCollectRegistrar registrar = new KafkaCollectRegistrar(Map.of(21L, cluster), tasks);
        registrar.refresh(List.of(cluster), List.of(), List.of(configuration));
        assertEquals(Map.of("RuntimeMetadata-22", unrelated), tasks);
    }
}
