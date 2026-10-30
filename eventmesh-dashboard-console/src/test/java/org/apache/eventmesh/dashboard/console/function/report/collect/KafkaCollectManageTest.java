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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class KafkaCollectManageTest {

    private CollectManage manager;
    private ClusterMetadata cluster;
    private RuntimeMetadata broker;
    private CollectMetadata configuration;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        manager = new CollectManage();
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
    public void recognizesConfigurationWithoutClusterTypeWhenBrokerArrivesFirst() throws IllegalAccessException {
        configuration.setClusterType(null);
        manager.handlerData(List.of(), List.of(broker), List.of());
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(2, tasks(11L).size());
    }

    @Test
    public void newlyKafkaClusterReadsItsExistingConfiguration() throws Exception {
        DruidDataSource source = mock(DruidDataSource.class);
        DruidPooledConnection connection = mock(DruidPooledConnection.class);
        when(source.getConnection()).thenReturn(connection);
        manager.setKafkaDataSource(source);
        FieldUtils.writeField(manager, "kafkaLastSync", LocalDateTime.of(2025, 1, 1, 0, 0), true);
        try (MockedStatic<KafkaMetadataReader> reader = mockStatic(KafkaMetadataReader.class)) {
            reader.when(() -> KafkaMetadataReader.readClusters(any(), any(), any())).thenReturn(List.of(cluster));
            reader.when(() -> KafkaMetadataReader.readRuntimes(any(), any(), any())).thenReturn(List.of(broker));
            reader.when(() -> KafkaMetadataReader.readConfigurations(any(), any(), any())).thenAnswer(call -> {
                LocalDateTime since = call.getArgument(1);
                return since.isBefore(LocalDateTime.of(2020, 1, 1, 0, 0)) ? List.of(configuration) : List.of();
            });
            manager.syncData();
        }
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
    public void brokerNameCannotCollideWithAnotherBrokerIdentity() throws IllegalAccessException {
        RuntimeMetadata second = broker(13L, 11L);
        broker.setName("RuntimeMetadata-13");
        manager.handlerData(List.of(cluster), List.of(broker, second), List.of(configuration));
        KafkaAdminCollect admin = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-11");
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> nodes = (List<RuntimeMetadata>) FieldUtils.readField(admin, "brokers", true);
        assertEquals(2, nodes.size());
        assertEquals(3, tasks(11L).size());
        configuration.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(tasks(11L).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changingClusterTypeDoesNotRecreateKafkaTasks() throws IllegalAccessException {
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        Map<String, Collect> waiting = (Map<String, Collect>) FieldUtils.readField(manager, "waitExecuteCollectMap", true);
        Collect replacement = (index, wrapper) -> { };
        waiting.put("CollectMetadata-13", replacement);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        broker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        configuration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        configuration.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        assertTrue(tasks(11L).isEmpty());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertEquals(replacement, executing.get("CollectMetadata-13"));
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
    public void legacyFailureDoesNotBlockKafkaRegistration() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        CollectMetadata rocketConfiguration = configuration(23L, 21L);
        rocketConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        assertThrows(NullPointerException.class, () -> manager.handlerData(List.of(cluster, rocket), List.of(broker),
            List.of(rocketConfiguration, configuration)));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void legacyCachesRetainTheirOriginalKeys() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata first = broker(31L, 21L);
        first.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        RuntimeMetadata second = broker(32L, 21L);
        second.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        second.setName(first.getName());
        manager.handlerData(List.of(rocket), List.of(first, second), List.of());
        Map<Long, Map<String, RuntimeMetadata>> nodes =
            (Map<Long, Map<String, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        assertEquals(Map.of(first.getName(), second), nodes.get(21L));

        Map<String, Collect> waiting = (Map<String, Collect>) FieldUtils.readField(manager, "waitExecuteCollectMap", true);
        waiting.put("CollectMetadata-41", (index, wrapper) -> { });
        waiting.put("CollectMetadata-42", (index, wrapper) -> { });
        CollectMetadata firstConfiguration = configuration(41L, 21L);
        firstConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        firstConfiguration.setCollectType(CollectType.PROMETHEUS);
        CollectMetadata secondConfiguration = configuration(42L, 21L);
        secondConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        secondConfiguration.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(), List.of(), List.of(firstConfiguration, secondConfiguration));
        Map<Long, CollectMetadata> configurations =
            (Map<Long, CollectMetadata>) FieldUtils.readField(manager, "collectMetadataMap", true);
        assertEquals(Set.of(41L, 42L), configurations.keySet());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void kafkaConfigurationIdDoesNotMatchAnotherClusterId() throws IllegalAccessException {
        configuration.setId(100L);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        ClusterMetadata rocket = cluster(100L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata rocketBroker = broker(301L, 100L);
        rocketBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(rocket), List.of(rocketBroker), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("RuntimeMetadata-301"));
        assertEquals(2, tasks(11L).size());
    }

    @Test
    public void disablingAnotherClusterDoesNotRemoveKafkaConfiguration() throws IllegalAccessException {
        configuration.setId(100L);
        ClusterMetadata rocket = cluster(100L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        manager.handlerData(List.of(cluster, rocket), List.of(broker), List.of(configuration));
        CollectMetadata rocketConfiguration = configuration(101L, 100L);
        rocketConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        rocketConfiguration.setStatus(0L);
        manager.handlerData(List.of(), List.of(), List.of(rocketConfiguration));
        broker.setPort(9095);
        manager.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(2, tasks(11L).size());
    }

    @Test
    public void metadataReaderMapsKafkaEnableAndEmptyJmxPort() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        ResultSetMetaData nodeColumns = mock(ResultSetMetaData.class);
        when(rows.getMetaData()).thenReturn(nodeColumns);
        when(nodeColumns.getColumnCount()).thenReturn(4);
        when(nodeColumns.getColumnLabel(1)).thenReturn("id");
        when(nodeColumns.getColumnLabel(2)).thenReturn("cluster_type");
        when(nodeColumns.getColumnLabel(3)).thenReturn("cluster_id");
        when(nodeColumns.getColumnLabel(4)).thenReturn("kafka_jmx_port");
        when(rows.getObject(1)).thenReturn(12L);
        when(rows.getObject(2)).thenReturn(ClusterType.STORAGE_KAFKA_RAFT.name());
        when(rows.getObject(3)).thenReturn(11L);
        when(rows.getString("kafka_jmx_port")).thenReturn("\t");
        List<RuntimeMetadata> nodes = KafkaMetadataReader.readRuntimes(connection, LocalDateTime.now(), Set.of());
        assertEquals(1, nodes.size());
        assertNull(nodes.get(0).getJmxPort());
        when(rows.next()).thenReturn(true, false);
        when(rows.getString("kafka_jmx_port")).thenReturn("9.5");
        assertThrows(SQLException.class, () -> KafkaMetadataReader.readRuntimes(connection, LocalDateTime.now(), Set.of()));

        Connection second = mock(Connection.class);
        PreparedStatement configurations = mock(PreparedStatement.class);
        ResultSet configurationRows = mock(ResultSet.class);
        when(second.prepareStatement(anyString())).thenReturn(configurations);
        when(configurations.executeQuery()).thenReturn(configurationRows);
        when(configurationRows.next()).thenReturn(true, false);
        ResultSetMetaData configurationColumns = mock(ResultSetMetaData.class);
        when(configurationRows.getMetaData()).thenReturn(configurationColumns);
        when(configurationColumns.getColumnCount()).thenReturn(3);
        when(configurationColumns.getColumnLabel(1)).thenReturn("cluster_type");
        when(configurationColumns.getColumnLabel(2)).thenReturn("collect_type");
        when(configurationColumns.getColumnLabel(3)).thenReturn("cluster_id");
        when(configurationRows.getObject(1)).thenReturn(ClusterType.STORAGE_KAFKA_CLUSTER.name());
        when(configurationRows.getObject(2)).thenReturn(CollectType.EVENTMESH.name());
        when(configurationRows.getObject(3)).thenReturn(11L);
        when(configurationRows.getBoolean("kafka_enable")).thenReturn(true);
        CollectMetadata mapped = KafkaMetadataReader.readConfigurations(second, LocalDateTime.now(), Set.of()).get(0);
        assertEquals(Boolean.TRUE, mapped.getEnable());
        assertEquals(ClusterType.STORAGE_KAFKA_CLUSTER, mapped.getClusterType());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Collect> tasks(long clusterId) throws IllegalAccessException {
        Map<String, Collect> all = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Map<String, Collect> kafka = new HashMap<>();
        all.forEach((key, value) -> {
            if (value instanceof KafkaAdminCollect || value instanceof KafkaJmxCollect) {
                kafka.put(key, value);
            }
        });
        return kafka;
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
