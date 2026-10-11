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
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.RocketMQCollect;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaCollectManageTest {

    private CollectManage manage;

    private ClusterMetadata cluster;

    private RuntimeMetadata broker;

    private CollectMetadata configuration;

    private Map<String, Collect> collectors;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws IllegalAccessException {
        manage = new CollectManage();
        cluster = cluster(11L);
        broker = broker(12L, 11L);
        configuration = configuration(13L, 11L);
        collectors = (Map<String, Collect>) FieldUtils.readField(manage, "executeingCollectMap", true);
    }

    @Test
    public void registersClusterAndBrokerCollectorsAndRemovesThemWhenDisabled() {
        load();
        assertEquals(2, collectors.size());
        assertTrue(collectors.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);

        broker.setStatus(0L);
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("ClusterMetadata-11") instanceof KafkaAdminCollect);

        configuration.setStatus(2L);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(), List.of(broker(12L, 11L)), List.of());
        assertTrue(collectors.isEmpty());
    }

    @Test
    public void registersWhenConfigurationArrivesBeforeClusterAndNodes() {
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(cluster), List.of(), List.of());
        assertTrue(collectors.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void refreshesBrokerAddressAndNameUsingCachedConfiguration() throws IllegalAccessException {
        load();
        Collect previous = collectors.get("RuntimeMetadata-12");
        broker.setHost("new-host");
        broker.setName("renamed-broker");
        broker.setJmxPort(10000);
        manage.handlerData(List.of(), List.of(broker), List.of());

        assertEquals(2, collectors.size());
        assertNotSame(previous, collectors.get("RuntimeMetadata-12"));
        assertSame(broker, FieldUtils.readField(collectors.get("RuntimeMetadata-12"), "runtimeMetadata", true));
        assertEquals(List.of(broker), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
    }

    @Test
    public void removesOnlyJmxTaskWhenJmxIsDisabled() throws IllegalAccessException {
        load();
        broker.setJmxPort(0);
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertFalse(collectors.containsKey("RuntimeMetadata-12"));
        assertEquals(List.of(broker), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
    }

    @Test
    public void keepsInstancesAcrossEmptyRefreshes() {
        load();
        Collect admin = collectors.get("ClusterMetadata-11");
        Collect jmx = collectors.get("RuntimeMetadata-12");
        manage.handlerData(List.of(), List.of(), List.of());
        assertSame(admin, collectors.get("ClusterMetadata-11"));
        assertSame(jmx, collectors.get("RuntimeMetadata-12"));
    }

    @Test
    public void usesAllCachedBrokersWhenOnlyOneNodeChanges() throws IllegalAccessException {
        load();
        RuntimeMetadata second = broker(14L, 11L);
        manage.handlerData(List.of(), List.of(second), List.of());
        assertEquals(List.of(broker, second), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
        assertEquals(3, collectors.size());

        second.setStatus(0L);
        manage.handlerData(List.of(), List.of(second), List.of());
        assertFalse(collectors.containsKey("RuntimeMetadata-14"));
        assertEquals(List.of(broker), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
    }

    @Test
    public void removesAllKafkaTasksWhenClusterIsDeleted() {
        load();
        cluster.setStatus(0L);
        manage.handlerData(List.of(cluster), List.of(), List.of());
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertTrue(collectors.isEmpty());
    }

    @Test
    public void deletesTasksThroughPublicConfigurationRemoval() {
        load();
        manage.deleteByCollectMetadata(configuration);
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertTrue(collectors.isEmpty());
    }

    @Test
    public void preservesOtherClustersWhenOneConfigurationIsDeleted() {
        load();
        manage.handlerData(List.of(cluster(21L)), List.of(broker(22L, 21L)), List.of(configuration(23L, 21L)));
        Collect otherAdmin = collectors.get("ClusterMetadata-21");
        Collect otherJmx = collectors.get("RuntimeMetadata-22");
        configuration.setStatus(0L);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(2, collectors.size());
        assertSame(otherAdmin, collectors.get("ClusterMetadata-21"));
        assertSame(otherJmx, collectors.get("RuntimeMetadata-22"));
    }

    @Test
    public void keepsRocketMqOnTheExistingCollectorPath() {
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        broker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        configuration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        load();
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof RocketMQCollect);
        broker.setHost("updated-host");
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof RocketMQCollect);
    }

    private void load() {
        manage.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
    }

    private ClusterMetadata cluster(long id) {
        ClusterMetadata metadata = new ClusterMetadata();
        metadata.setId(id);
        metadata.setName("kafka-" + id);
        metadata.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        metadata.setStatus(1L);
        return metadata;
    }

    private RuntimeMetadata broker(long id, long clusterId) {
        RuntimeMetadata metadata = new RuntimeMetadata();
        metadata.setId(id);
        metadata.setClusterId(clusterId);
        metadata.setName("broker-" + id);
        metadata.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        metadata.setHost("localhost");
        metadata.setPort(9092);
        metadata.setJmxPort(9999);
        metadata.setStatus(1L);
        return metadata;
    }

    private CollectMetadata configuration(long id, long clusterId) {
        CollectMetadata metadata = new CollectMetadata();
        metadata.setId(id);
        metadata.setClusterId(clusterId);
        metadata.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        metadata.setCollectType(CollectType.EVENTMESH);
        metadata.setStatus(1L);
        return metadata;
    }
}
