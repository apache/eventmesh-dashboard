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
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.RocketMQCollect;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaCollectManageTest {

    @Test
    public void disabledEnableFlagDoesNotRegisterCollectors() {
        configuration.setEnable(false);
        load();
        assertTrue(collectors.isEmpty());
        configuration.setEnable(true);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(2, collectors.size());
    }

    @Test
    public void distinctNodesWithTheSameNameRemainInTheSharedCache() {
        RuntimeMetadata second = broker(13L, 11L);
        second.setName(broker.getName());
        second.setPort(9094);
        manage.handlerData(List.of(cluster), List.of(broker, second), List.of(configuration));
        assertEquals(3, collectors.size());
        second.setStatus(0L);
        manage.handlerData(List.of(), List.of(second), List.of());
        assertEquals(2, collectors.size());
        assertTrue(collectors.containsKey("RuntimeMetadata-12"));
    }

    @Test
    public void mapsHistoricalNumericEnableAndEmptyJmxPort() throws Exception {
        java.sql.ResultSet rows = mock(java.sql.ResultSet.class);
        java.sql.ResultSetMetaData metadata = mock(java.sql.ResultSetMetaData.class);
        when(rows.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(metadata.getColumnLabel(1)).thenReturn("enable");
        when(rows.getObject(1)).thenReturn(1);
        CollectMetadata enabled = org.apache.eventmesh.dashboard.common.utils.RowMapperUtil.mapRow(rows, CollectMetadata.class);
        assertEquals(Boolean.TRUE, enabled.getEnable());
        when(rows.getObject(1)).thenReturn(0);
        assertEquals(Boolean.FALSE, org.apache.eventmesh.dashboard.common.utils.RowMapperUtil.mapRow(rows, CollectMetadata.class).getEnable());
        when(metadata.getColumnLabel(1)).thenReturn("jmx_port");
        when(rows.getObject(1)).thenReturn("");
        assertNull(org.apache.eventmesh.dashboard.common.utils.RowMapperUtil.mapRow(rows, RuntimeMetadata.class).getJmxPort());
        when(rows.getObject(1)).thenReturn("9999");
        assertEquals(Integer.valueOf(9999),
            org.apache.eventmesh.dashboard.common.utils.RowMapperUtil.mapRow(rows, RuntimeMetadata.class).getJmxPort());
    }

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
    @SuppressWarnings("unchecked")
    public void submitsEachCollectorOnceWithADistinctIndex() throws Exception {
        CollectManage subject = new CollectManage();
        ThreadPoolExecutor workers = (ThreadPoolExecutor) FieldUtils.readField(subject, "threadPoolExecutor", true);
        DataSyncHandler handler = (DataSyncHandler) FieldUtils.readField(subject, "dataSyncHandler", true);
        ReportEngine engine = mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
        java.util.Set<Integer> indexes = java.util.concurrent.ConcurrentHashMap.newKeySet();
        doAnswer(invocation -> {
            completed.countDown();
            return null;
        }).when(engine).batchInsertByClass(Map.of());
        Map<String, Collect> tasks = (Map<String, Collect>) FieldUtils.readField(subject, "executeingCollectMap", true);
        for (int i = 0; i < 64; i++) {
            tasks.put("task-" + i, (index, wrapper) -> {
                indexes.add(index);
                RestoreData result = mock(RestoreData.class);
                when(result.getDataMap()).thenReturn(Map.of());
                wrapper.sync(result);
            });
        }
        try {
            subject.collect();
            assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(64, indexes.size());
            verify(engine, times(1)).batchInsertByClass(Map.of());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void registrationChangesDuringSubmissionKeepTheOriginalBatch() throws IllegalAccessException {
        DataSyncHandler handler = (DataSyncHandler) FieldUtils.readField(manage, "dataSyncHandler", true);
        ReportEngine engine = mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        java.util.Set<String> invoked = new java.util.HashSet<>();
        for (String name : List.of("admin", "jmx")) {
            collectors.put(name, (index, wrapper) -> {
                invoked.add(name);
                RestoreData result = mock(RestoreData.class);
                when(result.getDataMap()).thenReturn(Map.of());
                wrapper.sync(result);
            });
        }
        // 任务注册表在首次提交时变化，模拟元数据刷新与本轮采集交错。
        ThreadPoolExecutor workers = new ThreadPoolExecutor(1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(2), task -> new Thread(task, "submission-contract-test")) {
            private boolean first = true;

            @Override
            public void execute(Runnable task) {
                if (first) {
                    first = false;
                    collectors.clear();
                    collectors.put("next-round", (index, wrapper) -> invoked.add("next-round"));
                }
                task.run();
            }
        };
        FieldUtils.writeField(manage, "threadPoolExecutor", workers, true);
        try {
            manage.collect();
            assertEquals(java.util.Set.of("admin", "jmx"), invoked);
            verify(engine, times(1)).batchInsertByClass(Map.of());
        } finally {
            workers.shutdownNow();
        }
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
        // collect 表没有 cluster_type，必须覆盖真实数据库配置而非依赖人工补齐的类型。
        assertNull(configuration.getClusterType());
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(2, collectors.size());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void registersWhenClusterAndNodesArriveBeforeUntypedConfiguration() {
        manage.handlerData(List.of(cluster), List.of(broker), List.of());
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(2, collectors.size());
        assertTrue(collectors.get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
    }

    @Test
    public void registersWhenNodesAndConfigurationArriveBeforeCluster() {
        manage.handlerData(List.of(), List.of(broker), List.of(configuration));
        assertTrue(collectors.isEmpty());
        manage.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(2, collectors.size());
    }

    @Test
    public void reusesUnifiedCachesAndKeepsNodesForConfigurationReenable() throws IllegalAccessException {
        load();
        Map<?, ?> configurations = (Map<?, ?>) FieldUtils.readField(manage, "collectMetadataMap", true);
        Map<?, ?> runtimes = (Map<?, ?>) FieldUtils.readField(manage, "runtimeMetadataMap", true);
        assertEquals(Map.of(11L, configuration), configurations);
        assertEquals(1, ((Map<?, ?>) runtimes.get(11L)).size());

        configuration.setStatus(2L);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(configurations.isEmpty());
        assertTrue(collectors.isEmpty());
        assertEquals(1, ((Map<?, ?>) runtimes.get(11L)).size());

        configuration.setStatus(1L);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(2, collectors.size());
    }

    @Test
    public void removesRenamedNodeByIdAndDoesNotResurrectItOnReenable() throws IllegalAccessException {
        load();
        RuntimeMetadata renamed = broker(12L, 11L);
        renamed.setName("renamed");
        manage.handlerData(List.of(), List.of(renamed), List.of());
        RuntimeMetadata deleted = broker(12L, 11L);
        deleted.setName("renamed");
        deleted.setStatus(0L);
        manage.handlerData(List.of(), List.of(deleted), List.of());
        assertEquals(1, collectors.size());
        assertEquals(List.of(), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
        manage.deleteByCollectMetadata(configuration);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(1, collectors.size());
    }

    @Test
    public void refreshesTasksWhenBrokerChangesToMetadataNode() throws IllegalAccessException {
        load();
        RuntimeMetadata changed = broker(12L, 11L);
        changed.setClusterType(ClusterType.STORAGE_KAFKA_ZK);
        manage.handlerData(List.of(), List.of(changed), List.of());
        assertEquals(1, collectors.size());
        assertEquals(List.of(), FieldUtils.readField(collectors.get("ClusterMetadata-11"), "brokers", true));
    }

    @Test
    public void keepsOneAdminAcrossBrokerAndRaftUpdates() {
        load();
        RuntimeMetadata raft = broker(14L, 11L);
        raft.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
        manage.handlerData(List.of(), List.of(raft), List.of());
        manage.handlerData(List.of(), List.of(raft, broker), List.of(configuration));
        assertEquals(3, collectors.size());
        assertEquals(1, collectors.values().stream().filter(KafkaAdminCollect.class::isInstance).count());
    }

    @Test
    public void removesOnlyKafkaTasksWhenCollectionModeChanges() {
        load();
        Collect unrelated = mock(Collect.class);
        collectors.put("RuntimeMetadata-99", unrelated);
        configuration.setCollectType(CollectType.PROMETHEUS);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(Map.of("RuntimeMetadata-99", unrelated), collectors);
        configuration.setCollectType(CollectType.EVENTMESH);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(3, collectors.size());
        assertSame(unrelated, collectors.get("RuntimeMetadata-99"));
    }

    @Test
    public void clearsConfigurationAndNodesWhenClusterIsDeleted() throws IllegalAccessException {
        load();
        cluster.setStatus(0L);
        manage.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
        assertTrue(collectors.isEmpty());
        assertTrue(((Map<?, ?>) FieldUtils.readField(manage, "collectMetadataMap", true)).isEmpty());
        assertTrue(((Map<?, ?>) FieldUtils.readField(manage, "runtimeMetadataMap", true)).isEmpty());
        manage.handlerData(List.of(cluster(11L)), List.of(), List.of());
        assertTrue(collectors.isEmpty());
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
        // 类型只来自真实集群，配置无类型时其他采集路径也必须正常工作。
        configuration.setClusterType(null);
        load();
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof RocketMQCollect);
        broker.setHost("updated-host");
        manage.handlerData(List.of(), List.of(broker), List.of());
        assertEquals(1, collectors.size());
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof RocketMQCollect);
    }

    @Test
    public void leavesRocketMqTasksUnchangedWhenKafkaConfigurationChanges() {
        load();
        ClusterMetadata rocket = cluster(21L);
        rocket.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        RuntimeMetadata rocketBroker = broker(22L, 21L);
        rocketBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        manage.handlerData(List.of(rocket), List.of(rocketBroker), List.of(configuration(23L, 21L)));
        Collect existing = collectors.get("RuntimeMetadata-22");
        assertTrue(existing instanceof RocketMQCollect);
        configuration.setStatus(2L);
        manage.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(Map.of("RuntimeMetadata-22", existing), collectors);
    }

    @Test
    public void rocketMqRuntimeUpdateKeepsTheOtherBrokerInstance() {
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        broker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeMetadata second = broker(14L, 11L);
        second.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        manage.handlerData(List.of(cluster), List.of(broker, second), List.of(configuration));
        Collect unchanged = collectors.get("RuntimeMetadata-14");

        broker.setHost("updated-host");
        manage.handlerData(List.of(), List.of(broker), List.of());

        assertEquals(2, collectors.size());
        assertSame(unchanged, collectors.get("RuntimeMetadata-14"));
        assertTrue(collectors.get("RuntimeMetadata-12") instanceof RocketMQCollect);
    }

    @Test
    public void removesKafkaTasksWhenActualClusterTypeChanges() {
        load();
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        // 配置即使带旧类型，也必须按真实集群分派，缓存的 Kafka 节点不能创建新 JMX。
        configuration.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        manage.handlerData(List.of(cluster), List.of(), List.of(configuration));
        assertTrue(collectors.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void preservesPrometheusClusterKeysAcrossEndpointUpdatesAndModeChanges() throws IllegalAccessException {
        CollectManage subject = spy(new CollectManage());
        Map<String, Collect> waiting = (Map<String, Collect>) FieldUtils.readField(subject, "waitExecuteCollectMap", true);
        Map<String, Collect> running = (Map<String, Collect>) FieldUtils.readField(subject, "executeingCollectMap", true);
        // 隔离 HTTP exporter 初始化，仅验证管理器的集群任务键和元数据生命周期。
        doAnswer(invocation -> {
            ClusterMetadata metadata = invocation.getArgument(1);
            waiting.put("ClusterMetadata-" + metadata.getId(), mock(Collect.class));
            return null;
        }).when(subject).register(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(ClusterMetadata.class));
        cluster.setClusterType(ClusterType.STORAGE_JVM_BROKER);
        broker.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        configuration.setCollectType(CollectType.PROMETHEUS);
        subject.handlerData(List.of(), List.of(broker), List.of(configuration));
        subject.handlerData(List.of(cluster), List.of(), List.of());
        assertEquals(1, running.size());
        Collect previous = running.get("ClusterMetadata-11");
        assertSame(waiting.get("ClusterMetadata-11"), previous);
        broker.setPort(9093);
        subject.handlerData(List.of(), List.of(broker), List.of());
        assertNotSame(previous, running.get("ClusterMetadata-11"));
        configuration.setStatus(2L);
        subject.handlerData(List.of(), List.of(), List.of(configuration));
        assertTrue(running.isEmpty());
        configuration.setStatus(1L);
        subject.handlerData(List.of(), List.of(), List.of(configuration));
        assertEquals(1, running.size());
        broker.setStatus(0L);
        subject.handlerData(List.of(), List.of(broker), List.of());
        assertTrue(running.isEmpty());
        assertTrue(waiting.isEmpty());
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
        // 与数据库行一致，采集配置没有 cluster_type。
        metadata.setClusterType(null);
        metadata.setCollectType(CollectType.EVENTMESH);
        metadata.setStatus(1L);
        return metadata;
    }
}
