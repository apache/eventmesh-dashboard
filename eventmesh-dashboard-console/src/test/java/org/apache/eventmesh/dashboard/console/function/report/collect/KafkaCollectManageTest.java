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
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.factory.KafkaCollectFactory;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    public void changingClusterTypeUsesPreviouslyCachedConfiguration() throws Exception {
        ClusterMetadata previous = cluster(11L);
        previous.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        manager.handlerData(List.of(previous), List.of(), List.of(configuration));
        manager.handlerData(List.of(cluster), List.of(broker), List.of());
        assertEquals(2, tasks(11L).size());
    }

    @Test
    public void replayedMetadataKeepsCollectorsButSameSecondConnectionChangesRebuildThem() throws IllegalAccessException {
        LocalDateTime updated = LocalDateTime.of(2026, 1, 1, 12, 0);
        cluster.setUpdateTime(updated);
        broker.setUpdateTime(updated);
        configuration.setUpdateTime(updated);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        Map<String, Collect> original = tasks(11L);

        ClusterMetadata replayedCluster = cluster(11L);
        RuntimeMetadata replayedBroker = broker(12L, 11L);
        CollectMetadata replayedConfiguration = configuration(13L, 11L);
        replayedCluster.setUpdateTime(updated);
        replayedBroker.setUpdateTime(updated);
        replayedConfiguration.setUpdateTime(updated);
        manager.handlerData(List.of(replayedCluster), List.of(replayedBroker), List.of(replayedConfiguration));
        assertSame(original.get("ClusterMetadata-11"), tasks(11L).get("ClusterMetadata-11"));
        assertSame(original.get("RuntimeMetadata-12"), tasks(11L).get("RuntimeMetadata-12"));

        RuntimeMetadata changed = broker(12L, 11L);
        changed.setUpdateTime(updated);
        changed.setHost("127.0.0.2");
        changed.setPort(9093);
        changed.setJmxPort(9998);
        manager.handlerData(List.of(), List.of(changed), List.of());
        assertNotSame(original.get("ClusterMetadata-11"), tasks(11L).get("ClusterMetadata-11"));
        assertNotSame(original.get("RuntimeMetadata-12"), tasks(11L).get("RuntimeMetadata-12"));
        KafkaAdminCollect admin = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-11");
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> nodes = (List<RuntimeMetadata>) FieldUtils.readField(admin, "brokers", true);
        assertEquals("127.0.0.2", nodes.get(0).getHost());
        assertEquals(9093, nodes.get(0).getPort());
        assertEquals(9998, nodes.get(0).getJmxPort());
    }

    @Test
    public void sameSecondStateAndDimensionChangesUpdateCollectors() throws IllegalAccessException {
        LocalDateTime updated = LocalDateTime.of(2026, 1, 1, 12, 0);
        cluster.setUpdateTime(updated);
        broker.setUpdateTime(updated);
        configuration.setUpdateTime(updated);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        Collect original = tasks(11L).get("ClusterMetadata-11");

        ClusterMetadata renamed = cluster(11L);
        renamed.setUpdateTime(updated);
        renamed.setName("renamed-cluster");
        renamed.setOrganizationId(2L);
        manager.handlerData(List.of(renamed), List.of(), List.of());
        assertNotSame(original, tasks(11L).get("ClusterMetadata-11"));

        CollectMetadata disabled = configuration(13L, 11L);
        disabled.setUpdateTime(updated);
        disabled.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(disabled));
        assertTrue(tasks(11L).isEmpty());

        CollectMetadata enabled = configuration(13L, 11L);
        enabled.setUpdateTime(updated);
        manager.handlerData(List.of(), List.of(), List.of(enabled));
        assertEquals(2, tasks(11L).size());

        RuntimeMetadata stopped = broker(12L, 11L);
        stopped.setUpdateTime(updated);
        stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        assertFalse(tasks(11L).containsKey("RuntimeMetadata-12"));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
    }

    @Test
    public void failedCollectorCreationRetriesUnchangedMetadata() throws IllegalAccessException {
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("test failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);

        RuntimeMetadata changed = broker(12L, 11L);
        changed.setPort(9093);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(changed), List.of()));
        assertTrue(tasks(11L).isEmpty());

        RuntimeMetadata replayed = broker(12L, 11L);
        replayed.setPort(9093);
        manager.handlerData(List.of(), List.of(replayed), List.of());
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
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
        waiting.put("ClusterMetadata-11", replacement);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        broker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        configuration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        configuration.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        assertTrue(tasks(11L).isEmpty());
        assertSame(replacement, waiting.get("ClusterMetadata-11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingToKafkaWhileStoppingLegacyNodeRemovesOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 11L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        ClusterMetadata other = cluster(21L);
        other.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata otherNode = broker(22L, 21L);
        otherNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy, other), List.of(legacyNode, otherNode), List.of(configuration, configuration(23L, 21L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertNotNull(executing.get("RuntimeMetadata-12"));
        Collect unrelated = executing.get("RuntimeMetadata-22");
        RuntimeMetadata stopped = broker(12L, 11L);
        stopped.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(stopped), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertSame(unrelated, executing.get("RuntimeMetadata-22"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void retryAfterSwitchAndStopDoesNotLeaveOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 11L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyNode), List.of(configuration));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("test failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        RuntimeMetadata stopped = broker(12L, 11L);
        stopped.setStatus(0L);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(cluster), List.of(stopped), List.of()));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertFalse(executing.containsKey("ClusterMetadata-11"));
        // 同步重放可能不包含任何变更行，仍需从已有配置恢复失败任务。
        manager.handlerData(List.of(), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingAfterLegacyNodeAlreadyStoppedRemovesItsOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 11L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyNode), List.of(configuration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect original = executing.get("RuntimeMetadata-12");
        RuntimeMetadata stoppedLegacy = broker(12L, 11L);
        stoppedLegacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        stoppedLegacy.setStatus(0L);
        manager.handlerData(List.of(), List.of(stoppedLegacy), List.of());
        assertSame(original, executing.get("RuntimeMetadata-12"));
        // 集群类型变更的批次可能没有重复发送此前已经停用的节点。
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingToStoppedKafkaRemovesPreviouslyStoppedLegacyTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 11L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyNode), List.of(configuration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        RuntimeMetadata stoppedNode = broker(12L, 11L);
        stoppedNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        stoppedNode.setStatus(0L);
        manager.handlerData(List.of(), List.of(stoppedNode), List.of());
        assertNotNull(executing.get("RuntimeMetadata-12"));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void kafkaClusterCleanupKeepsMovedTaskDespiteStaleNodeCache() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 21L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(cluster, legacy), List.of(legacyNode), List.of(configuration, configuration(23L, 21L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect legacyTask = executing.get("RuntimeMetadata-12");
        assertNotNull(legacyTask);
        Map<Long, Map<Long, RuntimeMetadata>> nodes =
            (Map<Long, Map<Long, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        // 模拟原地迁移后旧集群缓存尚未清理，任务实际已经属于另一个集群。
        nodes.computeIfAbsent(11L, ignored -> new java.util.concurrent.ConcurrentHashMap<>()).put(12L, legacyNode);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertSame(legacyTask, executing.get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void stoppingKafkaNodeDoesNotRemoveOtherClustersUncachedTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(14L, 21L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(cluster, legacy), List.of(legacyNode), List.of(configuration, configuration(23L, 21L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect legacyTask = executing.get("RuntimeMetadata-14");
        assertNotNull(legacyTask);
        RuntimeMetadata stoppedLegacy = broker(14L, 21L);
        stoppedLegacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        stoppedLegacy.setStatus(0L);
        manager.handlerData(List.of(), List.of(stoppedLegacy), List.of());
        assertSame(legacyTask, executing.get("RuntimeMetadata-14"));
        RuntimeMetadata stoppedKafka = broker(12L, 11L);
        stoppedKafka.setStatus(0L);
        manager.handlerData(List.of(), List.of(stoppedKafka), List.of());
        assertSame(legacyTask, executing.get("RuntimeMetadata-14"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void legacyClusterPreservesNodeStopBehavior() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyNode = broker(12L, 11L);
        legacyNode.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyNode), List.of(configuration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect original = executing.get("RuntimeMetadata-12");
        RuntimeMetadata stopped = broker(12L, 11L);
        stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        assertSame(original, executing.get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingLegacyClusterToKafkaRemovesOldNodeTasks() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyBroker = broker(12L, 11L);
        legacyBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        RuntimeMetadata legacySecond = broker(14L, 11L);
        legacySecond.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        ClusterMetadata unrelated = cluster(21L);
        unrelated.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata unrelatedBroker = broker(22L, 21L);
        unrelatedBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy, unrelated), List.of(legacyBroker, legacySecond, unrelatedBroker),
            List.of(configuration, configuration(23L, 21L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertTrue(executing.containsKey("RuntimeMetadata-12"));
        assertTrue(executing.containsKey("RuntimeMetadata-14"));
        Collect unrelatedTask = executing.get("RuntimeMetadata-22");

        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertFalse(executing.containsKey("RuntimeMetadata-14"));
        assertSame(unrelatedTask, executing.get("RuntimeMetadata-22"));
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        RuntimeMetadata withoutJmx = broker(12L, 11L);
        withoutJmx.setJmxPort(null);
        manager.handlerData(List.of(), List.of(withoutJmx), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingLegacyClusterToKafkaRemovesOldExporter() throws IllegalAccessException {
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        when(reports.getReportMetaDataMap()).thenReturn(Map.of());
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        manager.setReportHandlerManage(reports);
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata exporter = broker(14L, 11L);
        exporter.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        CollectMetadata prometheus = configuration(13L, 11L);
        prometheus.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(legacy), List.of(exporter), List.of(prometheus));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Map<String, Collect> waiting = (Map<String, Collect>) FieldUtils.readField(manager, "waitExecuteCollectMap", true);
        // 模拟已运行的旧任务，专门验证切换到 Kafka 时的清理。
        Collect oldExporter = waiting.get("ClusterMetadata-11");
        assertNotNull(oldExporter);
        executing.put("CollectMetadata-13", oldExporter);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("CollectMetadata-13"));
        assertFalse(waiting.containsKey("ClusterMetadata-11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingToKafkaWithDisabledConfigurationRemovesOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyBroker = broker(12L, 11L);
        legacyBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        configuration.setEnable(false);
        manager.handlerData(List.of(legacy), List.of(legacyBroker), List.of(configuration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertTrue(executing.containsKey("RuntimeMetadata-12"));
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertFalse(executing.containsKey("ClusterMetadata-11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingLegacyClusterToStoppedKafkaRemovesOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyBroker = broker(12L, 11L);
        legacyBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyBroker), List.of(configuration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertTrue(executing.containsKey("RuntimeMetadata-12"));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void switchingToKafkaWhileDisablingPrometheusRemovesOldTask() throws IllegalAccessException {
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        when(reports.getReportMetaDataMap()).thenReturn(Map.of());
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        manager.setReportHandlerManage(reports);
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata exporter = broker(14L, 11L);
        exporter.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        CollectMetadata prometheus = configuration(13L, 11L);
        prometheus.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(legacy), List.of(exporter), List.of(prometheus));
        CollectMetadata disabled = configuration(13L, 11L);
        disabled.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of(disabled));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("CollectMetadata-13"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void migratingNodeAfterKafkaClusterStopsDoesNotDeleteNewClusterTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        manager.handlerData(List.of(cluster, legacy), List.of(broker), List.of(configuration, configuration(23L, 21L)));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        // 原集群缓存持有的对象也可能由调用方原地迁移。
        RuntimeMetadata moved = broker;
        moved.setClusterId(21L);
        moved.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(), List.of(moved), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect newTask = executing.get("RuntimeMetadata-12");
        assertTrue(newTask != null && !(newTask instanceof KafkaJmxCollect));
        Map<Long, Map<Long, RuntimeMetadata>> nodes =
            (Map<Long, Map<Long, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        assertFalse(nodes.get(11L).containsKey(12L));
        manager.handlerData(List.of(cluster), List.of(), List.of());
        assertSame(newTask, executing.get("RuntimeMetadata-12"));
        manager.deleteByCollectMetadata(configuration);
        assertSame(newTask, executing.get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void stoppingLegacyClusterAndConfigurationPreservesCleanupOrder() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyBroker = broker(31L, 21L);
        legacyBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata config = configuration(23L, 21L);
        manager.handlerData(List.of(legacy), List.of(legacyBroker), List.of(config));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        legacy.setStatus(0L);
        manager.handlerData(List.of(legacy), List.of(), List.of());
        assertTrue(executing.containsKey("RuntimeMetadata-31"));
        config.setStatus(0L);
        manager.handlerData(List.of(), List.of(), List.of(config));
        assertFalse(executing.containsKey("RuntimeMetadata-31"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void retryingMovedNodeAfterStoppedKafkaClusterKeepsNewTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        manager.handlerData(List.of(cluster, legacy), List.of(broker), List.of(configuration, configuration(23L, 21L)));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        RuntimeMetadata moved = broker(12L, 21L);
        moved.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        Map<ClusterType, Class<?>> classes =
            (Map<ClusterType, Class<?>>) FieldUtils.readStaticField(CollectManage.class, "CLUSTER_TYPE_CLASS", true);
        Class<?> previousClass = classes.remove(moved.getClusterType());
        try {
            assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(moved), List.of()));
        } finally {
            classes.put(moved.getClusterType(), previousClass);
        }
        RuntimeMetadata retry = broker(12L, 21L);
        retry.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(), List.of(retry), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect newTask = executing.get("RuntimeMetadata-12");
        assertTrue(newTask != null && !(newTask instanceof KafkaJmxCollect));
        manager.deleteByCollectMetadata(configuration);
        assertSame(newTask, executing.get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void retryingFailedKafkaCreationAfterTypeSwitchDoesNotRestoreOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(11L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata legacyBroker = broker(12L, 11L);
        legacyBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(legacy), List.of(legacyBroker), List.of(configuration));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("test failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(cluster), List.of(broker), List.of()));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        manager.handlerData(List.of(cluster(11L)), List.of(broker(12L, 11L)), List.of());
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(executing.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
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
    public void otherClusterWithoutNodesDoesNotBlockKafkaRegistration() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        CollectMetadata rocketConfiguration = configuration(23L, 21L);
        rocketConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        manager.handlerData(List.of(cluster, rocket), List.of(broker), List.of(rocketConfiguration, configuration));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void replayingStoppedNodeKeepsActiveKafkaCollectors() throws IllegalAccessException {
        RuntimeMetadata activeSecond = broker(14L, 11L);
        RuntimeMetadata stopping = broker(15L, 11L);
        manager.handlerData(List.of(cluster), List.of(broker, activeSecond, stopping), List.of(configuration));
        RuntimeMetadata stopped = broker(15L, 11L);
        stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        Map<String, Collect> afterStop = tasks(11L);
        assertEquals(3, afterStop.size());
        RuntimeMetadata replay = broker(15L, 11L);
        replay.setStatus(0L);
        manager.handlerData(List.of(), List.of(replay), List.of());
        Map<String, Collect> afterReplay = tasks(11L);
        assertSame(afterStop.get("ClusterMetadata-11"), afterReplay.get("ClusterMetadata-11"));
        assertSame(afterStop.get("RuntimeMetadata-12"), afterReplay.get("RuntimeMetadata-12"));
        assertSame(afterStop.get("RuntimeMetadata-14"), afterReplay.get("RuntimeMetadata-14"));
        RuntimeMetadata resumed = broker(15L, 11L);
        manager.handlerData(List.of(), List.of(resumed), List.of());
        assertEquals(4, tasks(11L).size());
        assertTrue(tasks(11L).get("RuntimeMetadata-15") instanceof KafkaJmxCollect);
    }

    @Test
    public void replayingStoppedConfigurationKeepsOtherActiveConfiguration() throws IllegalAccessException {
        CollectMetadata second = configuration(14L, 11L);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration, second));
        CollectMetadata stopped = configuration(14L, 11L);
        stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(), List.of(stopped));
        Map<String, Collect> afterStop = tasks(11L);
        assertEquals(2, afterStop.size());
        CollectMetadata replay = configuration(14L, 11L);
        replay.setStatus(0L);
        manager.handlerData(List.of(), List.of(), List.of(replay));
        Map<String, Collect> afterReplay = tasks(11L);
        assertSame(afterStop.get("ClusterMetadata-11"), afterReplay.get("ClusterMetadata-11"));
        assertSame(afterStop.get("RuntimeMetadata-12"), afterReplay.get("RuntimeMetadata-12"));
    }

    @Test
    public void movingKafkaConfigurationRemovesOnlyFormerClusterTasks() throws IllegalAccessException {
        ClusterMetadata otherCluster = cluster(21L);
        RuntimeMetadata otherBroker = broker(22L, 21L);
        manager.handlerData(List.of(cluster, otherCluster), List.of(broker, otherBroker), List.of(configuration));
        assertEquals(Set.of("ClusterMetadata-11", "RuntimeMetadata-12"), tasks(11L).keySet());
        CollectMetadata moved = configuration(13L, 21L);
        manager.handlerData(List.of(), List.of(), List.of(moved));
        assertEquals(Set.of("ClusterMetadata-21", "RuntimeMetadata-22"), tasks(11L).keySet());
        moved.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(moved));
        assertTrue(tasks(11L).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingLegacyConfigurationToKafkaRemovesOldTasks() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata oldBroker = broker(31L, 21L);
        oldBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata oldConfig = configuration(23L, 21L);
        manager.handlerData(List.of(cluster, legacy), List.of(broker, oldBroker), List.of(oldConfig));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertTrue(executing.containsKey("RuntimeMetadata-31"));
        manager.handlerData(List.of(), List.of(), List.of(configuration(23L, 11L)));
        assertFalse(executing.containsKey("RuntimeMetadata-31"));
        assertTrue(executing.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(executing.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingLegacyConfigurationKeepsOtherLegacyConfiguration() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata oldBroker = broker(31L, 21L);
        oldBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata moving = configuration(23L, 21L);
        CollectMetadata remaining = configuration(24L, 21L);
        manager.handlerData(List.of(cluster, legacy), List.of(broker, oldBroker), List.of(moving, remaining));
        manager.handlerData(List.of(), List.of(), List.of(moving));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertSame(moving, FieldUtils.readField(executing.get("RuntimeMetadata-31"), "collectMetadata", true));
        manager.handlerData(List.of(), List.of(), List.of(configuration(23L, 11L)));
        assertSame(remaining, FieldUtils.readField(executing.get("RuntimeMetadata-31"), "collectMetadata", true));
        assertTrue(executing.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingKafkaConfigurationToLegacyRemovesOnlyKafkaTasks() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata other = broker(31L, 21L);
        other.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(cluster, legacy), List.of(broker, other), List.of(configuration));
        manager.handlerData(List.of(), List.of(), List.of(configuration(13L, 21L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("ClusterMetadata-11"));
        assertFalse(executing.containsKey("RuntimeMetadata-12"));
        assertTrue(executing.containsKey("RuntimeMetadata-31"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingLegacyNodeToKafkaReplacesOldTask() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata oldBroker = broker(31L, 21L);
        oldBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(cluster, legacy), List.of(oldBroker), List.of(configuration, configuration(23L, 21L)));
        RuntimeMetadata moved = broker(31L, 11L);
        manager.handlerData(List.of(), List.of(moved), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertTrue(executing.get("RuntimeMetadata-31") instanceof KafkaJmxCollect);
        Map<Long, Map<Long, RuntimeMetadata>> nodes =
            (Map<Long, Map<Long, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        assertFalse(nodes.get(21L).containsKey(31L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingKafkaNodeToLegacySurvivesFormerClusterStop() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        manager.handlerData(List.of(cluster, legacy), List.of(broker), List.of(configuration, configuration(23L, 21L)));
        RuntimeMetadata moved = broker(12L, 21L);
        moved.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        manager.handlerData(List.of(), List.of(moved), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect legacyCollector = executing.get("RuntimeMetadata-12");
        assertTrue(legacyCollector != null && !(legacyCollector instanceof KafkaJmxCollect));
        configuration.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        assertSame(legacyCollector, executing.get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void movingPrometheusNodeToKafkaRemovesOldExporter() throws IllegalAccessException {
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        when(reports.getReportMetaDataMap()).thenReturn(Map.of());
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        manager.setReportHandlerManage(reports);
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata exporter = broker(31L, 21L);
        exporter.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        CollectMetadata oldConfig = configuration(23L, 21L);
        oldConfig.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(cluster, legacy), List.of(exporter), List.of(configuration, oldConfig));
        manager.handlerData(List.of(), List.of(broker(31L, 11L)), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(executing.containsKey("CollectMetadata-23"));
        assertTrue(executing.get("RuntimeMetadata-31") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void retryingConfigurationMoveRestoresLegacyAndKafkaTasks() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata oldBroker = broker(31L, 21L);
        oldBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata moving = configuration(23L, 21L);
        CollectMetadata remaining = configuration(24L, 21L);
        manager.handlerData(List.of(cluster, legacy), List.of(broker, oldBroker), List.of(moving, remaining));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("test failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        CollectMetadata moved = configuration(23L, 11L);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(), List.of(moved)));
        manager.handlerData(List.of(), List.of(), List.of(configuration(23L, 11L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertSame(remaining, FieldUtils.readField(executing.get("RuntimeMetadata-31"), "collectMetadata", true));
        assertTrue(executing.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedLegacyRebuildRetainsOldConfigurationForRetry() throws IllegalAccessException {
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata oldBroker = broker(31L, 21L);
        oldBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata moving = configuration(23L, 21L);
        CollectMetadata remaining = configuration(24L, 21L);
        manager.handlerData(List.of(cluster, legacy), List.of(broker, oldBroker), List.of(moving, remaining));
        Map<ClusterType, Class<?>> classes = (Map<ClusterType, Class<?>>) FieldUtils.readStaticField(CollectManage.class, "CLUSTER_TYPE_CLASS", true);
        Class<?> previousClass = classes.remove(legacy.getClusterType());
        try {
            assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(), List.of(configuration(23L, 11L))));
        } finally {
            classes.put(legacy.getClusterType(), previousClass);
        }
        manager.handlerData(List.of(), List.of(), List.of(configuration(23L, 11L)));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertSame(remaining, FieldUtils.readField(executing.get("RuntimeMetadata-31"), "collectMetadata", true));
        assertTrue(executing.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedExporterFallbackRetainsOldNodeForRetry() throws IllegalAccessException {
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        when(reports.getReportMetaDataMap()).thenReturn(Map.of());
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        manager.setReportHandlerManage(reports);
        ClusterMetadata legacy = cluster(21L);
        legacy.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata exporter = broker(31L, 21L);
        exporter.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        RuntimeMetadata remaining = broker(32L, 21L);
        remaining.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        CollectMetadata oldConfig = configuration(23L, 21L);
        oldConfig.setCollectType(CollectType.PROMETHEUS);
        manager.handlerData(List.of(cluster, legacy), List.of(exporter, remaining), List.of(configuration, oldConfig));
        doThrow(new IllegalStateException("test failure")).doReturn(Map.of()).when(reports).getReportMetaDataMap();
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(broker(31L, 11L)), List.of()));
        Map<Long, Map<Long, RuntimeMetadata>> nodes =
            (Map<Long, Map<Long, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        assertTrue(nodes.get(21L).containsKey(31L));
        manager.handlerData(List.of(), List.of(broker(31L, 11L)), List.of());
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        assertFalse(nodes.get(21L).containsKey(31L));
        assertTrue(executing.get("RuntimeMetadata-31") instanceof KafkaJmxCollect);
        assertTrue(executing.containsKey("CollectMetadata-23"));
    }

    @Test
    public void failedFormerClusterRebuildIsRetriedAfterConfigurationMoves() throws IllegalAccessException {
        ClusterMetadata otherCluster = cluster(21L);
        CollectMetadata remaining = configuration(14L, 11L);
        manager.handlerData(List.of(cluster, otherCluster), List.of(broker), List.of(configuration, remaining));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        AtomicBoolean failOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            ClusterMetadata target = invocation.getArgument(1);
            if (Objects.equals(target.getId(), 11L) && failOnce.getAndSet(false)) {
                throw new IllegalStateException("test failure");
            }
            return invocation.callRealMethod();
        }).when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        CollectMetadata moved = configuration(13L, 21L);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(), List.of(moved)));
        manager.handlerData(List.of(), List.of(), List.of(configuration(13L, 21L)));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
        assertTrue(tasks(11L).get("ClusterMetadata-21") instanceof KafkaAdminCollect);
    }

    @Test
    public void failedSecondConfigurationDoesNotPublishPartialCollectors() throws IllegalAccessException {
        CollectMetadata second = configuration(14L, 11L);
        manager.handlerData(List.of(cluster), List.of(broker), List.of(configuration, second));
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("test failure");
            }
            return invocation.callRealMethod();
        }).when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        RuntimeMetadata changed = broker(12L, 11L);
        changed.setPort(9093);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(changed), List.of()));
        assertTrue(tasks(11L).isEmpty());
        RuntimeMetadata replay = broker(12L, 11L);
        replay.setPort(9093);
        manager.handlerData(List.of(), List.of(replay), List.of());
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void movingKafkaNodeDoesNotLeaveItInFormerCluster() throws IllegalAccessException {
        ClusterMetadata otherCluster = cluster(21L);
        CollectMetadata otherConfig = configuration(23L, 21L);
        manager.handlerData(List.of(cluster, otherCluster), List.of(broker), List.of(configuration, otherConfig));
        RuntimeMetadata moved = broker(12L, 21L);
        manager.handlerData(List.of(), List.of(moved), List.of());
        KafkaAdminCollect first = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-11");
        KafkaAdminCollect second = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-21");
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> firstNodes = (List<RuntimeMetadata>) FieldUtils.readField(first, "brokers", true);
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> secondNodes = (List<RuntimeMetadata>) FieldUtils.readField(second, "brokers", true);
        assertTrue(firstNodes.isEmpty());
        assertEquals(List.of(12L), secondNodes.stream().map(RuntimeMetadata::getId).toList());
        CollectMetadata stopped = configuration(13L, 11L);
        stopped.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(stopped));
        assertTrue(tasks(11L).get("ClusterMetadata-21") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void movingMutableKafkaNodeCleansFormerCluster() throws IllegalAccessException {
        ClusterMetadata otherCluster = cluster(21L);
        CollectMetadata otherConfig = configuration(23L, 21L);
        manager.handlerData(List.of(cluster, otherCluster), List.of(broker), List.of(configuration, otherConfig));

        broker.setClusterId(21L);
        manager.handlerData(List.of(), List.of(broker), List.of());
        KafkaAdminCollect first = (KafkaAdminCollect) tasks(11L).get("ClusterMetadata-11");
        @SuppressWarnings("unchecked")
        List<RuntimeMetadata> firstNodes = (List<RuntimeMetadata>) FieldUtils.readField(first, "brokers", true);
        assertTrue(firstNodes.isEmpty());
        configuration.setEnable(false);
        manager.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);

        CollectMetadata movedConfig = configuration(23L, 11L);
        manager.handlerData(List.of(), List.of(), List.of(movedConfig));
        assertEquals(Set.of("ClusterMetadata-11"), tasks(11L).keySet());
    }

    @Test
    public void missingExporterDoesNotBlockKafkaRegistration() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        CollectMetadata rocketConfiguration = configuration(23L, 21L);
        rocketConfiguration.setCollectType(CollectType.PROMETHEUS);
        rocketConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);

        manager.handlerData(List.of(cluster, rocket), List.of(broker), List.of(rocketConfiguration, configuration));

        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void invalidExporterDoesNotBlockOtherClusters() throws IllegalAccessException {
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        when(reports.getReportMetaDataMap()).thenReturn(Map.of());
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        manager.setReportHandlerManage(reports);
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata exporter = broker(22L, 21L);
        exporter.setClusterType(ClusterType.PROMETHEUS_EXPORTER);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> manager.handlerData(List.of(cluster, rocket), List.of(exporter, broker), List.of(configuration)));

        assertTrue(failure.getSuppressed()[0].getMessage().contains("runtime metadata id 22"));
        assertTrue(tasks(11L).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(tasks(11L).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);

        Map<String, Collect> original = tasks(11L);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(cluster(11L), rocket),
            List.of(exporter, broker(12L, 11L)), List.of(configuration(13L, 11L))));
        assertSame(original.get("ClusterMetadata-11"), tasks(11L).get("ClusterMetadata-11"));
        assertSame(original.get("RuntimeMetadata-12"), tasks(11L).get("RuntimeMetadata-12"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updatingOneLegacyNodeKeepsOtherCollectorsAndLegacyEnableBehavior() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata first = broker(31L, 21L);
        first.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        RuntimeMetadata second = broker(32L, 21L);
        second.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        CollectMetadata rocketConfiguration = configuration(23L, 21L);
        rocketConfiguration.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        rocketConfiguration.setEnable(false);
        manager.handlerData(List.of(rocket), List.of(first, second), List.of(rocketConfiguration));
        Map<String, Collect> executing = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Collect firstCollector = executing.get("RuntimeMetadata-31");
        Collect secondCollector = executing.get("RuntimeMetadata-32");
        assertTrue(firstCollector != null && secondCollector != null);

        first.setPort(9095);
        manager.handlerData(List.of(), List.of(first), List.of());
        assertNotSame(firstCollector, executing.get("RuntimeMetadata-31"));
        assertSame(secondCollector, executing.get("RuntimeMetadata-32"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sharedCachesRetainDistinctNodeAndConfigurationIds() throws IllegalAccessException {
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata first = broker(31L, 21L);
        first.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        RuntimeMetadata second = broker(32L, 21L);
        second.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        second.setName(first.getName());
        manager.handlerData(List.of(rocket), List.of(first, second), List.of());
        Map<Long, Map<Long, RuntimeMetadata>> nodes =
            (Map<Long, Map<Long, RuntimeMetadata>>) FieldUtils.readField(manager, "runtimeMetadataMap", true);
        assertEquals(Map.of(first.getId(), first, second.getId(), second), nodes.get(21L));

        Map<String, Collect> waiting = (Map<String, Collect>) FieldUtils.readField(manager, "waitExecuteCollectMap", true);
        waiting.put("ClusterMetadata-21", (index, wrapper) -> { });
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
