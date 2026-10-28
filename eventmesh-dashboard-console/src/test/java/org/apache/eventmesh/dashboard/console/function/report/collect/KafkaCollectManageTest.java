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

import org.apache.commons.lang3.reflect.FieldUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaCollectManageTest {

    private CollectManage manager;
    private KafkaCollectRegistrar registrar;
    private ClusterMetadata cluster;
    private RuntimeMetadata broker;
    private CollectMetadata configuration;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        manager = new CollectManage();
        registrar = (KafkaCollectRegistrar) FieldUtils.readField(manager, "kafkaCollectRegistrar", true);
        cluster = cluster(11L);
        broker = broker(12L, 11L);
        configuration = configuration(13L, 11L);
    }

    @Test
    public void registersClusterAndBrokerAndRemovesDisabledTasks() throws IllegalAccessException {
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
        broker.setStatus(0L);
        manager.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(1, tasks(11L).size());
        configuration.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(tasks(11L).isEmpty());
        configuration.setEnable(true);
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(1, tasks(11L).size());
    }

    @Test
    public void registersWhenConfigurationAndNodesArriveBeforeCluster() throws IllegalAccessException {
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        manager.handlerData(List.of(), List.of(broker), List.of());
        assertTrue(tasks(11L).isEmpty());
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(2, tasks(11L).size());
    }

    @Test
    public void retainsDistinctNodesAndUsesUpdatedBootstrapAddresses() throws IllegalAccessException {
        RuntimeMetadata second = broker(14L, 11L);
        second.setName(broker.getName());
        second.setPort(9094);
        manager.handlerData(List.of(cluster), List.of(broker, second), List.of(configuration));
        assertEquals(3, tasks(11L).size());
        broker.setPort(9095);
        manager.handlerData(List.of(), List.of(broker), List.of());
        KafkaAdminCollect admin = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-11");
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> nodes = (List<RuntimeMetadata>) FieldUtils.readField(admin, "brokers", true);
        assertEquals(List.of(9095, 9094), nodes.stream().map(RuntimeMetadata::getPort).toList());
        broker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(), List.of(broker), List.of());
        assertFalse(tasks(11L).containsKey("RuntimeMetadata-12"));
    }

    @Test
    public void stoppingClusterAndDeletingConfigurationRemoveOnlyKafkaTasks() throws IllegalAccessException {
        @SuppressWarnings("unchecked")
        Map<String, Collect> existing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect legacy = (index, wrapper) -> { };
        existing.put("existing", legacy);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertTrue(tasks(11L).isEmpty());
        cluster.setStatus(1L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(2, tasks(11L).size());
        manager.deleteByCollectMetadata(configuration);
        assertTrue(tasks(11L).isEmpty());
        assertEquals(Map.of("existing", legacy), existing);
    }

    @Test
    public void metadataReaderMapsKafkaEnableAndEmptyJmxPort() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getString("cluster_type")).thenReturn(ClusterType.STORAGE_KAFKA_RAFT.name());
        when(rows.getLong("id")).thenReturn(12L);
        when(rows.getLong("cluster_id")).thenReturn(11L);
        when(rows.getString("jmx_port")).thenReturn("");
        List<RuntimeMetadata> nodes = KafkaMetadataReader.readRuntimes(connection, LocalDateTime.now(), Set.of());
        assertEquals(1, nodes.size());
        assertNull(nodes.get(0).getJmxPort());

        Connection second = mock(Connection.class);
        PreparedStatement configurations = mock(PreparedStatement.class);
        ResultSet configurationRows = mock(ResultSet.class);
        when(second.prepareStatement(anyString())).thenReturn(configurations);
        when(configurations.executeQuery()).thenReturn(configurationRows);
        when(configurationRows.next()).thenReturn(true, false);
        when(configurationRows.getString("kafka_cluster_type")).thenReturn(ClusterType.STORAGE_KAFKA_CLUSTER.name());
        when(configurationRows.getString("collect_type")).thenReturn(CollectType.EVENTMESH.name());
        when(configurationRows.getLong("cluster_id")).thenReturn(11L);
        when(configurationRows.getBoolean("enable")).thenReturn(true);
        CollectMetadata mapped = KafkaMetadataReader.readConfigurations(second, LocalDateTime.now(), Set.of()).get(0);
        assertEquals(Boolean.TRUE, mapped.getEnable());
        assertEquals(ClusterType.STORAGE_KAFKA_CLUSTER, mapped.getClusterType());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Collect> tasks(long clusterId) throws IllegalAccessException {
        Map<Long, Map<String, Collect>> all = (Map<Long, Map<String, Collect>>) FieldUtils.readField(registrar, "collectors", true);
        return all.getOrDefault(clusterId, Map.of());
    }

    private ClusterMetadata cluster(long id) {
        ClusterMetadata result = new ClusterMetadata();
        result.setId(id);
        result.setOrganizationId(1L);
        result.setName("cluster-" + id);
        result.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        result.setStatus(1L);
        result.setIsDelete(0);
        return result;
    }

    private RuntimeMetadata broker(long id, long clusterId) {
        RuntimeMetadata result = new RuntimeMetadata();
        result.setId(id);
        result.setClusterId(clusterId);
        result.setName("broker-" + id);
        result.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
        result.setHost("127.0.0.1");
        result.setPort(9092);
        result.setJmxPort(9999);
        result.setStatus(1L);
        result.setIsDelete(0);
        return result;
    }

    private CollectMetadata configuration(long id, long clusterId) {
        CollectMetadata result = new CollectMetadata();
        result.setId(id);
        result.setClusterId(clusterId);
        result.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        result.setCollectType(CollectType.EVENTMESH);
        result.setEnable(true);
        result.setStatus(1L);
        result.setIsDelete(0);
        return result;
    }
}
