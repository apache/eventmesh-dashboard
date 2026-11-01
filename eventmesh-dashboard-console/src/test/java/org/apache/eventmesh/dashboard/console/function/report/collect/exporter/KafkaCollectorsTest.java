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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTopicTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerGroupMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaPartitionOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaTopicMetrics;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKakfaConfig;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.Test;

public class KafkaCollectorsTest {

    @Test
    public void stalledAttributeReadHasOneRoundDeadlineAndClosesTheConnection() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenAnswer(invocation -> {
                if (calls.getAndIncrement() == 0) {
                    // 远端请求不响应中断时，超时后的迟到值不能写入已完成的采集缓冲。
                    while (release.getCount() > 0) {
                        java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                        Thread.interrupted();
                    }
                }
                return 0L;
            });
        KafkaJmxCollect collector = jmxCollector(connector);
        try {
            long started = System.nanoTime();
            assertTrue(collect(collector).getDataMap().isEmpty());
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertTrue(elapsed >= 4500 && elapsed < 8500, "JMX read deadline: " + elapsed);
        } finally {
            release.countDown();
        }
        verify(connector, org.mockito.Mockito.timeout(2000)).close();
        RestoreData next = collectAfterRecovery(collector);
        assertEquals(0L, ((KafkaBrokerTrafficMetrics) next.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0)).getValueMessagesInTotal());
        verify(connector, times(2)).close();
    }

    @Test
    public void stalledNodeDoesNotStartAnotherRequestOrBlockHealthyNodes() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        MBeanServerConnection stalled = mock(MBeanServerConnection.class);
        JMXConnector stalledConnector = mock(JMXConnector.class);
        when(stalledConnector.getMBeanServerConnection()).thenReturn(stalled);
        when(stalled.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenAnswer(invocation -> {
                if (calls.getAndIncrement() == 0) {
                    while (release.getCount() > 0) {
                        java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                        Thread.interrupted();
                    }
                }
                return 0L;
            });
        KafkaJmxCollect collector = jmxCollector(stalledConnector);
        RuntimeMetadata stalledNode = broker();
        stalledNode.setId(101L);
        collector.setRuntimeMetadata(stalledNode);
        try {
            assertTrue(collect(collector).getDataMap().isEmpty());
            assertTrue(collect(collector).getDataMap().isEmpty());
            assertEquals(1, calls.get());
            MBeanServerConnection healthy = mock(MBeanServerConnection.class);
            JMXConnector healthyConnector = mock(JMXConnector.class);
            when(healthyConnector.getMBeanServerConnection()).thenReturn(healthy);
            when(healthy.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
                .thenReturn(1L);
            KafkaJmxCollect other = jmxCollector(healthyConnector);
            RuntimeMetadata otherNode = broker();
            otherNode.setId(102L);
            other.setRuntimeMetadata(otherNode);
            assertFalse(collect(other).getDataMap().isEmpty());
        } finally {
            release.countDown();
        }
    }

    @Test
    public void stalledCloseDoesNotBlockTheCollectionThread() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenReturn(1L);
        doAnswer(invocation -> {
            entered.countDown();
            while (release.getCount() > 0) {
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                Thread.interrupted();
            }
            return null;
        }).when(connector).close();
        KafkaJmxCollect collector = jmxCollector(connector);
        RuntimeMetadata runtime = broker();
        runtime.setId(201L);
        collector.setRuntimeMetadata(runtime);
        CompletableFuture<RestoreData> result = CompletableFuture.supplyAsync(() -> collect(collector));
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertTrue(result.get(7, TimeUnit.SECONDS).getDataMap().isEmpty());
        } finally {
            release.countDown();
        }
        verify(connector, org.mockito.Mockito.timeout(2000)).close();
    }

    @Test
    public void closeFailureDoesNotDiscardCollectedSamples() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenReturn(1L);
        org.mockito.Mockito.doThrow(new IOException("close failed")).when(connector).close();
        RestoreData result = collect(jmxCollector(connector));
        KafkaBrokerTrafficMetrics traffic = (KafkaBrokerTrafficMetrics) result.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0);
        assertEquals(1L, traffic.getValueMessagesInTotal());
    }

    @Test
    public void timedOutJmxConnectionsAreClosedWhenTheyArriveLateAndTheNextRoundRecovers() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        JMXConnector late = mock(JMXConnector.class);
        JMXConnector available = mock(JMXConnector.class);
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        when(available.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenReturn(0L);
        AtomicInteger calls = new AtomicInteger();
        KafkaJmxCollect collector = new KafkaJmxCollect() {
            @Override
            protected JMXConnector openConnector(JMXServiceURL url) throws IOException {
                if (calls.getAndIncrement() > 0) {
                    return available;
                }
                // 模拟不响应中断的 RMI 握手，超时后才到达的连接仍必须被关闭。
                while (release.getCount() > 0) {
                    java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                    Thread.interrupted();
                }
                return late;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setCollectMetadata(collect());
        collector.setRuntimeMetadata(broker());
        try {
            long started = System.nanoTime();
            assertTrue(collect(collector).getDataMap().isEmpty());
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertTrue(elapsed >= 4500 && elapsed < 8500, "JMX connection timeout: " + elapsed);
        } finally {
            release.countDown();
        }
        verify(late, org.mockito.Mockito.timeout(2000).times(1)).close();
        assertFalse(collectAfterRecovery(collector).getDataMap().isEmpty());
        verify(available).close();
    }

    @Test
    public void adminSamplesHaveClusterDimension() throws Exception {
        AdminClient client = mock(AdminClient.class);
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        ListTopicsResult topicsResult = mock(ListTopicsResult.class);
        DescribeTopicsResult descriptionsResult = mock(DescribeTopicsResult.class);
        TopicDescription description = mock(TopicDescription.class);
        TopicPartitionInfo online = mock(TopicPartitionInfo.class);
        TopicPartitionInfo offline = mock(TopicPartitionInfo.class);
        Node node = new Node(1, "localhost", 9092);
        when(client.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(node)));
        when(client.listTopics(any(ListTopicsOptions.class))).thenReturn(topicsResult);
        when(topicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of("orders")));
        when(client.describeTopics(Set.of("orders"))).thenReturn(descriptionsResult);
        when(descriptionsResult.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("orders", description)));
        when(description.partitions()).thenReturn(List.of(online, offline));
        when(online.partition()).thenReturn(0);
        when(offline.partition()).thenReturn(1);
        when(online.replicas()).thenReturn(List.of(node));
        when(offline.replicas()).thenReturn(List.of(node));
        when(online.isr()).thenReturn(List.of(node));
        when(offline.isr()).thenReturn(List.of());
        when(online.leader()).thenReturn(node);
        when(offline.leader()).thenReturn(null);
        TopicPartition firstPartition = new TopicPartition("orders", 0);
        TopicPartition secondPartition = new TopicPartition("orders", 1);
        ListOffsetsResult latest = mock(ListOffsetsResult.class);
        ListOffsetsResult earliest = mock(ListOffsetsResult.class);
        ListOffsetsResultInfo firstLatest = mock(ListOffsetsResultInfo.class);
        ListOffsetsResultInfo secondLatest = mock(ListOffsetsResultInfo.class);
        ListOffsetsResultInfo firstEarliest = mock(ListOffsetsResultInfo.class);
        ListOffsetsResultInfo secondEarliest = mock(ListOffsetsResultInfo.class);
        when(firstLatest.offset()).thenReturn(10L);
        when(secondLatest.offset()).thenReturn(20L);
        when(firstEarliest.offset()).thenReturn(0L);
        when(secondEarliest.offset()).thenReturn(5L);
        when(client.listOffsets(anyMap())).thenReturn(latest, earliest);
        when(latest.partitionResult(firstPartition)).thenReturn(KafkaFuture.completedFuture(firstLatest));
        when(latest.partitionResult(secondPartition)).thenReturn(KafkaFuture.completedFuture(secondLatest));
        when(earliest.partitionResult(firstPartition)).thenReturn(KafkaFuture.completedFuture(firstEarliest));
        when(earliest.partitionResult(secondPartition)).thenReturn(KafkaFuture.completedFuture(secondEarliest));
        ListConsumerGroupsResult groups = mock(ListConsumerGroupsResult.class);
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn("buyers");
        when(client.listConsumerGroups()).thenReturn(groups);
        when(groups.all()).thenReturn(KafkaFuture.completedFuture(List.of(listing)));
        DescribeConsumerGroupsResult describedGroups = mock(DescribeConsumerGroupsResult.class);
        ConsumerGroupDescription group = mock(ConsumerGroupDescription.class);
        when(client.describeConsumerGroups(List.of("buyers"))).thenReturn(describedGroups);
        when(describedGroups.all()).thenReturn(KafkaFuture.completedFuture(Map.of("buyers", group)));
        when(group.members()).thenReturn(List.of());
        ListConsumerGroupOffsetsResult groupOffsets = mock(ListConsumerGroupOffsetsResult.class);
        OffsetAndMetadata committed = mock(OffsetAndMetadata.class);
        when(committed.offset()).thenReturn(6L);
        when(client.listConsumerGroupOffsets("buyers")).thenReturn(groupOffsets);
        when(groupOffsets.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of(firstPartition, committed)));

        KafkaAdminCollect collector = new KafkaAdminCollect() {
            @Override
            protected AdminClient createClient(CreateKakfaConfig config) {
                assertEquals("localhost:9092", config.getNetAddresses()[0]);
                return client;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setCollectMetadata(collect());
        collector.setBrokers(List.of(broker()));

        RestoreData data = collect(collector);
        assertEquals(5, data.getDataMap().size());
        KafkaClusterMetrics metrics = (KafkaClusterMetrics) data.getDataMap().get(KafkaClusterMetrics.class).get(0);
        assertEquals(Long.valueOf(11), metrics.getClustersId());
        assertEquals(Long.valueOf(1), metrics.getValueBrokerCount());
        assertEquals(Long.valueOf(1), metrics.getValueTopicCount());
        assertEquals(Long.valueOf(2), metrics.getValuePartitionCount());
        assertEquals(Long.valueOf(1), metrics.getValueOfflinePartitionCount());
        assertEquals(Long.valueOf(1), metrics.getValueConsumerGroupCount());
        KafkaTopicMetrics topic = (KafkaTopicMetrics) data.getDataMap().get(KafkaTopicMetrics.class).get(0);
        assertEquals("orders", topic.getTopicName());
        assertEquals(Long.valueOf(2), topic.getValuePartitionCount());
        assertEquals(Long.valueOf(1), topic.getValueUnderReplicatedPartitionCount());
        assertEquals(2, data.getDataMap().get(KafkaPartitionOffsetMetrics.class).size());
        KafkaConsumerGroupMetrics consumer =
            (KafkaConsumerGroupMetrics) data.getDataMap().get(KafkaConsumerGroupMetrics.class).get(0);
        assertEquals("buyers", consumer.getGroupName());
        KafkaConsumerOffsetMetrics offset =
            (KafkaConsumerOffsetMetrics) data.getDataMap().get(KafkaConsumerOffsetMetrics.class).get(0);
        assertEquals("orders", offset.getTopicName());
        assertEquals("buyers", offset.getGroupName());
        assertEquals(Long.valueOf(6), offset.getValueCommittedOffset());
        assertEquals(Long.valueOf(10), offset.getValueEndOffset());
        assertEquals(Long.valueOf(4), offset.getValueLag());
    }

    @Test
    public void busyAdminRoundCompletesWithoutWaitingForActiveRound() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        KafkaAdminCollect collector = new KafkaAdminCollect() {
            @Override
            protected void doCollect() {
                entered.countDown();
                await(release);
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setCollectMetadata(collect());
        CompletableFuture<Void> active = CompletableFuture.runAsync(() -> collector.collect(0, recording(new ArrayList<>())));
        CompletableFuture<Void> busy = null;
        List<RestoreData> skipped = new ArrayList<>();
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            busy = CompletableFuture.runAsync(() -> collector.collect(0, recording(skipped)));
            busy.get(1, TimeUnit.SECONDS);
            assertEquals(1, skipped.size());
            assertTrue(skipped.get(0).getDataMap().isEmpty());
        } finally {
            release.countDown();
            active.get(5, TimeUnit.SECONDS);
            if (busy != null) {
                busy.get(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void failedPartitionKeepsHealthyPartitionOffsetsAndLag() throws Exception {
        AdminClient client = mock(AdminClient.class, RETURNS_DEEP_STUBS);
        Node node = new Node(1, "localhost", 9092);
        TopicPartition healthy = new TopicPartition("orders", 0);
        TopicPartition failed = new TopicPartition("orders", 1);
        TopicDescription topic = new TopicDescription("orders", false, List.of(
            new TopicPartitionInfo(0, node, List.of(node), List.of(node)),
            new TopicPartitionInfo(1, node, List.of(node), List.of(node))));
        ListOffsetsResultInfo latest = mock(ListOffsetsResultInfo.class);
        ListOffsetsResultInfo earliest = mock(ListOffsetsResultInfo.class);
        when(latest.offset()).thenReturn(10L);
        when(earliest.offset()).thenReturn(0L);
        KafkaFutureImpl<ListOffsetsResultInfo> failure = new KafkaFutureImpl<>();
        failure.completeExceptionally(new IllegalStateException("partition unavailable"));
        when(client.describeCluster().nodes().get(anyLong(), any(TimeUnit.class))).thenReturn(List.of(node));
        when(client.listTopics(any(ListTopicsOptions.class)).names().get(anyLong(), any(TimeUnit.class))).thenReturn(Set.of("orders"));
        when(client.describeTopics(Set.of("orders")).allTopicNames().get(anyLong(), any(TimeUnit.class)))
            .thenReturn(Map.of("orders", topic));
        when(client.listOffsets(anyMap())).thenReturn(
            new ListOffsetsResult(Map.of(healthy, KafkaFuture.completedFuture(latest), failed, failure)),
            new ListOffsetsResult(Map.of(healthy, KafkaFuture.completedFuture(earliest), failed, failure)));
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn("buyers");
        when(client.listConsumerGroups().all().get(anyLong(), any(TimeUnit.class))).thenReturn(List.of(listing));
        when(client.describeConsumerGroups(List.of("buyers")).all().get(anyLong(), any(TimeUnit.class))).thenReturn(Map.of());
        when(client.listConsumerGroupOffsets("buyers").partitionsToOffsetAndMetadata().get(anyLong(), any(TimeUnit.class)))
            .thenReturn(Map.of(healthy, new OffsetAndMetadata(6L)));
        KafkaAdminCollect collector = new KafkaAdminCollect() {
            @Override
            protected AdminClient createClient(CreateKakfaConfig config) {
                return client;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setCollectMetadata(collect());
        collector.setBrokers(List.of(broker()));

        RestoreData data = collect(collector);
        KafkaPartitionOffsetMetrics partition =
            (KafkaPartitionOffsetMetrics) data.getDataMap().get(KafkaPartitionOffsetMetrics.class).get(0);
        KafkaConsumerOffsetMetrics group =
            (KafkaConsumerOffsetMetrics) data.getDataMap().get(KafkaConsumerOffsetMetrics.class).get(0);
        assertEquals("0", partition.getPartitionIndex());
        assertEquals(10L, partition.getValueLatestOffset());
        assertEquals(4L, group.getValueLag());
    }

    @Test
    public void unavailableAdminValuesUseErrorMarkerInsteadOfNull() throws Exception {
        AdminClient client = mock(AdminClient.class, RETURNS_DEEP_STUBS);
        Node node = new Node(1, "localhost", 9092);
        TopicPartition partition = new TopicPartition("orders", 0);
        TopicDescription topic = new TopicDescription("orders", false,
            List.of(new TopicPartitionInfo(0, node, List.of(node), List.of(node))));
        ListOffsetsResultInfo latest = mock(ListOffsetsResultInfo.class);
        when(latest.offset()).thenReturn(10L);
        when(client.describeCluster().nodes().get(anyLong(), any(TimeUnit.class))).thenReturn(List.of(node));
        when(client.listTopics(any(ListTopicsOptions.class)).names().get(anyLong(), any(TimeUnit.class)))
            .thenReturn(Set.of("orders"));
        when(client.describeTopics(Set.of("orders")).allTopicNames().get(anyLong(), any(TimeUnit.class)))
            .thenReturn(Map.of("orders", topic));
        ListOffsetsResult latestResult = mock(ListOffsetsResult.class);
        ListOffsetsResult earliestResult = mock(ListOffsetsResult.class);
        KafkaFutureImpl<ListOffsetsResultInfo> unavailable = new KafkaFutureImpl<>();
        unavailable.completeExceptionally(new IllegalStateException("earliest offset unavailable"));
        when(client.listOffsets(anyMap())).thenReturn(latestResult, earliestResult);
        when(latestResult.partitionResult(partition)).thenReturn(KafkaFuture.completedFuture(latest));
        when(earliestResult.partitionResult(partition)).thenReturn(unavailable);
        when(client.listConsumerGroups().all().get(anyLong(), any(TimeUnit.class)))
            .thenThrow(new TimeoutException("consumer groups unavailable"));
        KafkaAdminCollect collector = new KafkaAdminCollect() {
            @Override
            protected AdminClient createClient(CreateKakfaConfig config) {
                return client;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setCollectMetadata(collect());
        collector.setBrokers(List.of(broker()));

        RestoreData data = collect(collector);

        KafkaClusterMetrics cluster = (KafkaClusterMetrics) data.getDataMap().get(KafkaClusterMetrics.class).get(0);
        KafkaPartitionOffsetMetrics offset = (KafkaPartitionOffsetMetrics) data.getDataMap().get(KafkaPartitionOffsetMetrics.class).get(0);
        assertEquals(Long.valueOf(-1), cluster.getValueConsumerGroupCount());
        assertEquals(Long.valueOf(-1), offset.getValueEarliestOffset());
        assertEquals(Long.valueOf(10), offset.getValueLatestOffset());
        verify(client).close();
    }

    @Test
    public void jmxSamplesHaveBrokerDimensionAndUseCumulativeCount() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of());
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenReturn(42L);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec"), "Count"))
            .thenReturn(1024L);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec"), "Count"))
            .thenReturn(2048L);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=FailedProduceRequestsPerSec"), "Count"))
            .thenReturn(2L);
        when(connection.getAttribute(new ObjectName("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions"), "Value"))
            .thenReturn(3L);
        when(connection.getAttribute(new ObjectName("kafka.server:type=ReplicaManager,name=LeaderCount"), "Value"))
            .thenReturn(5L);
        when(connection.getAttribute(new ObjectName("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce"), "Mean"))
            .thenReturn(12.5D);

        KafkaJmxCollect collector = new KafkaJmxCollect() {
            @Override
            protected JMXConnector openConnector(JMXServiceURL url) throws IOException {
                assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", url.toString());
                return connector;
            }
        };
        collector.setClusterMetadata(cluster());
        RuntimeMetadata runtime = broker();
        runtime.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
        collector.setRuntimeMetadata(runtime);
        collector.setCollectMetadata(collect());

        RestoreData data = collect(collector);
        assertEquals(2, data.getDataMap().size());
        KafkaBrokerTrafficMetrics traffic =
            (KafkaBrokerTrafficMetrics) data.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0);
        assertEquals(Long.valueOf(42), traffic.getValueMessagesInTotal());
        assertEquals(Long.valueOf(1024), traffic.getValueBytesInTotal());
        assertEquals(Long.valueOf(2048), traffic.getValueBytesOutTotal());
        assertEquals(Long.valueOf(2), traffic.getValueFailedProduceRequestsTotal());
        assertEquals(Long.valueOf(12), traffic.getRuntimeId());
        assertEquals(Long.valueOf(11), traffic.getClustersId());
        assertEquals(ClusterType.STORAGE_KAFKA_RAFT.name(), traffic.getRuntimeType());
        KafkaBrokerHealthMetrics health =
            (KafkaBrokerHealthMetrics) data.getDataMap().get(KafkaBrokerHealthMetrics.class).get(0);
        assertEquals(Long.valueOf(3), health.getValueUnderReplicatedPartitions());
        assertEquals(Long.valueOf(5), health.getValueLeaderCount());
        assertEquals(Float.valueOf(12.5F), health.getValueProduceRequestMeanMs());
    }

    @Test
    public void missingJmxPortDoesNotCreateSamples() {
        KafkaJmxCollect collector = new KafkaJmxCollect();
        RuntimeMetadata broker = broker();
        broker.setJmxPort(null);
        collector.setClusterMetadata(cluster());
        collector.setRuntimeMetadata(broker);
        collector.setCollectMetadata(collect());
        assertEquals(0, collect(collector).getDataMap().size());
    }

    @Test
    public void missingTrafficAttributeDoesNotReplaceAValidCountWithZero() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of());
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenReturn(42L);
        KafkaJmxCollect collector = new KafkaJmxCollect() {
            @Override
            protected JMXConnector openConnector(JMXServiceURL url) {
                return connector;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setRuntimeMetadata(broker());
        collector.setCollectMetadata(collect());

        RestoreData data = collect(collector);
        assertEquals(1, data.getDataMap().size());
        KafkaBrokerTrafficMetrics traffic =
            (KafkaBrokerTrafficMetrics) data.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0);
        assertEquals(Long.valueOf(42), traffic.getValueMessagesInTotal());
        // 缺失指标使用错误值，不能与真实零混淆。
        assertEquals(Long.valueOf(-1), traffic.getValueBytesInTotal());
    }

    @Test
    public void topicSamplesMergeCountersKeepDimensionsAndSkipAllFailures() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        ObjectName messages = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec,topic=orders");
        ObjectName bytes = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=orders");
        ObjectName payments = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=payments");
        ObjectName failed = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec,topic=failed");
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of());
        ObjectName pattern = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=*,topic=*");
        when(connection.queryNames(pattern, null)).thenReturn(Set.of(messages, bytes, payments, failed));
        when(connection.getAttribute(messages, "Count")).thenReturn(0L);
        when(connection.getAttribute(bytes, "Count")).thenReturn(20L);
        when(connection.getAttribute(payments, "Count")).thenReturn(30L);
        KafkaJmxCollect collector = jmxCollector(connector);

        RestoreData data = collect(collector);
        assertEquals(1, data.getDataMap().size());
        List<Object> samples = data.getDataMap().get(KafkaBrokerTopicTrafficMetrics.class);
        assertEquals(2, samples.size());
        for (Object sample : samples) {
            KafkaBrokerTopicTrafficMetrics topic = (KafkaBrokerTopicTrafficMetrics) sample;
            assertEquals(Long.valueOf(11), topic.getClustersId());
            assertEquals(Long.valueOf(12), topic.getRuntimeId());
            if ("orders".equals(topic.getTopicName())) {
                assertEquals(Long.valueOf(0), topic.getValueMessagesInTotal());
                assertEquals(Long.valueOf(20), topic.getValueBytesInTotal());
                assertEquals(Long.valueOf(-1), topic.getValueBytesOutTotal());
            } else {
                assertEquals("payments", topic.getTopicName());
                assertEquals(Long.valueOf(-1), topic.getValueMessagesInTotal());
                assertEquals(Long.valueOf(-1), topic.getValueBytesInTotal());
                assertEquals(Long.valueOf(30), topic.getValueBytesOutTotal());
            }
        }
        verify(connection, times(1)).queryNames(pattern, null);
        verify(connector).close();
    }

    @Test
    public void partialHealthUsesErrorValuesAndZeroRemainsValid() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=ReplicaManager,name=PartitionCount"), "Value"))
            .thenReturn(0L);
        RestoreData data = collect(jmxCollector(connector));
        KafkaBrokerHealthMetrics health = (KafkaBrokerHealthMetrics) data.getDataMap().get(KafkaBrokerHealthMetrics.class).get(0);
        assertEquals(Long.valueOf(0), health.getValuePartitionCount());
        assertEquals(Long.valueOf(-1), health.getValueLogSizeBytes());
        assertEquals(Long.valueOf(-1), health.getValueLeaderCount());
        assertEquals(Float.valueOf(-1F), health.getValueProduceRequestMeanMs());
        verify(connector).close();
    }

    @Test
    public void unexpectedReadFailureClosesConnectionAndNextRoundRecovers() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenReturn(connection);
        when(connection.getAttribute(new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"), "Count"))
            .thenThrow(new IllegalStateException("unexpected read failure")).thenReturn(0L);
        KafkaJmxCollect collector = jmxCollector(connector);
        DataSyncHandlerWrapper failed = mock(DataSyncHandlerWrapper.class);
        assertThrows(IllegalStateException.class, () -> collector.collect(0, failed));
        org.mockito.ArgumentCaptor<RestoreData> empty = org.mockito.ArgumentCaptor.forClass(RestoreData.class);
        verify(failed).sync(empty.capture());
        assertTrue(empty.getValue().getDataMap().isEmpty());
        RestoreData recovered = collect(collector);
        KafkaBrokerTrafficMetrics traffic = (KafkaBrokerTrafficMetrics) recovered.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0);
        assertEquals(Long.valueOf(0), traffic.getValueMessagesInTotal());
        verify(connector, times(2)).close();
    }

    @Test
    public void connectionFailureAndAllMissingAttributesProduceEmptyRounds() throws Exception {
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        JMXConnector connector = mock(JMXConnector.class);
        when(connector.getMBeanServerConnection()).thenThrow(new IOException("disconnected")).thenReturn(connection);
        KafkaJmxCollect collector = jmxCollector(connector);
        assertTrue(collect(collector).getDataMap().isEmpty());
        assertTrue(collect(collector).getDataMap().isEmpty());
        verify(connector, times(2)).close();
    }

    private KafkaJmxCollect jmxCollector(JMXConnector connector) {
        KafkaJmxCollect collector = new KafkaJmxCollect() {
            @Override
            protected JMXConnector openConnector(JMXServiceURL url) {
                return connector;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setRuntimeMetadata(broker());
        collector.setCollectMetadata(collect());
        return collector;
    }

    private RestoreData collect(org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect collector) {
        DataSyncHandlerWrapper wrapper = mock(DataSyncHandlerWrapper.class);
        AtomicReference<RestoreData> result = new AtomicReference<>();
        doAnswer(invocation -> {
            result.set(invocation.getArgument(0));
            return null;
        }).when(wrapper).sync(any(RestoreData.class));
        collector.collect(0, wrapper);
        assertNotNull(result.get());
        return result.get();
    }

    private RestoreData collectAfterRecovery(KafkaJmxCollect collector) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            RestoreData result = collect(collector);
            if (!result.getDataMap().isEmpty()) {
                return result;
            }
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        throw new AssertionError("JMX collector did not recover");
    }

    private CollectMetadata collect() {
        CollectMetadata collect = new CollectMetadata();
        collect.setClusterId(11L);
        return collect;
    }

    private ClusterMetadata cluster() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setName("kafka");
        cluster.setOrganizationId(1L);
        return cluster;
    }

    private RuntimeMetadata broker() {
        RuntimeMetadata broker = new RuntimeMetadata();
        broker.setId(12L);
        broker.setClusterId(11L);
        broker.setName("broker-1");
        broker.setHost("localhost");
        broker.setPort(9092);
        broker.setJmxPort(9999);
        return broker;
    }


    @Test
    public void numericReadsRejectMissingInvalidAndOverflowButKeepZero() throws Exception {
        final Probe collector = new Probe();
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        ObjectName name = new ObjectName("test:type=Metric");
        List<Object> invalid = List.of("not-a-number", Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            new BigInteger("9223372036854775808"), new BigDecimal("1.5"), -2L);
        for (Object value : invalid) {
            when(connection.getAttribute(name, "Value")).thenReturn(value);
            assertEquals(-1L, collector.readLong(connection, name.toString(), "Value"), value.toString());
        }
        when(connection.getAttribute(name, "Value")).thenReturn(null);
        assertEquals(-1L, collector.readLong(connection, name.toString(), "Value"));
        assertEquals(-1F, collector.readFloat(connection, name.toString(), "Value"));
        for (Object value : List.of("invalid", Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.MAX_VALUE, -2D)) {
            when(connection.getAttribute(name, "Value")).thenReturn(value);
            assertEquals(-1F, collector.readFloat(connection, name.toString(), "Value"), value.toString());
        }
        when(connection.getAttribute(name, "Value")).thenReturn(0L);
        assertEquals(0L, collector.readLong(connection, name.toString(), "Value"));
        assertEquals(0F, collector.readFloat(connection, name.toString(), "Value"));
        when(connection.getAttribute(name, "Value")).thenReturn(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, collector.readLong(connection, name.toString(), "Value"));
        when(connection.getAttribute(name, "Value")).thenThrow(new AttributeNotFoundException("missing"));
        assertEquals(-1L, collector.readLong(connection, name.toString(), "Value"));
        assertEquals(-1F, collector.readFloat(connection, name.toString(), "Value"));
    }

    @Test
    public void sumsRejectPartialReadsEmptyQueriesAndOverflow() throws Exception {
        final Probe collector = new Probe();
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        ObjectName first = new ObjectName("test:type=Metric,id=1");
        ObjectName second = new ObjectName("test:type=Metric,id=2");
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of(first, second));
        when(connection.getAttribute(first, "Value")).thenReturn(2L);
        when(connection.getAttribute(second, "Value")).thenReturn(3L);
        assertEquals(5L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
        when(connection.getAttribute(second, "Value")).thenReturn("bad");
        assertEquals(-1L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
        when(connection.getAttribute(first, "Value")).thenReturn(Long.MAX_VALUE);
        when(connection.getAttribute(second, "Value")).thenReturn(1L);
        assertEquals(-1L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
        when(connection.getAttribute(first, "Value")).thenReturn(0L);
        when(connection.getAttribute(second, "Value")).thenReturn(0L);
        assertEquals(0L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of());
        assertEquals(-1L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
        when(connection.queryNames(any(ObjectName.class), any())).thenThrow(new IOException("disconnected"));
        assertTrue(collector.queryNames(connection, "test:*").isEmpty());
        assertEquals(-1L, collector.sumLong(connection, "test:type=Metric,*", "Value"));
    }

    @Test
    public void groupedReadsMergeTopicsAndPreservePartialErrors() throws Exception {
        final Probe collector = new Probe();
        MBeanServerConnection connection = mock(MBeanServerConnection.class);
        ObjectName first = new ObjectName("test:type=Metric,name=Messages,topic=orders");
        ObjectName second = new ObjectName("test:type=Metric,name=Bytes,topic=orders");
        ObjectName third = new ObjectName("test:type=Metric,name=Messages,topic=payments");
        ObjectName quoted = new ObjectName("test:type=Metric,name=Messages,topic=" + ObjectName.quote("special,topic"));
        when(connection.queryNames(any(ObjectName.class), any())).thenReturn(Set.of(first, second, third, quoted));
        when(connection.getAttribute(first, "Count")).thenReturn(0L);
        when(connection.getAttribute(second, "Count")).thenThrow(new AttributeNotFoundException("missing"));
        when(connection.getAttribute(third, "Count")).thenReturn(7L);
        when(connection.getAttribute(quoted, "Count")).thenReturn(9L);
        Map<String, Map<String, Long>> groups = new HashMap<>();
        collector.collectGroupedLongs(connection, "test:type=Metric,*", "topic", "name", "Count",
            Set.of("Messages", "Bytes"), groups::put);
        assertEquals(Map.of("Messages", 0L, "Bytes", -1L), groups.get("orders"));
        assertEquals(Map.of("Messages", 7L, "Bytes", -1L), groups.get("payments"));
        assertEquals(Map.of("Messages", 9L, "Bytes", -1L), groups.get("special,topic"));
    }

    @Test
    public void unexpectedFailureCompletesOnceAndNextRoundRecovers() {
        Probe collector = configured();
        List<RestoreData> results = new ArrayList<>();
        ReportEngine engine = mock(ReportEngine.class);
        DataSyncHandler handler = new DataSyncHandler();
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(1);
        collector.action = () -> {
            collector.emit(8L);
            throw new IllegalStateException("failed after partial sample");
        };
        assertThrows(IllegalStateException.class, () -> collector.collect(0, wrapper));
        // 失败轮次只结算，不交付部分样本；仍使用真实汇总链路确认完成数归零。
        verify(engine).batchInsertByClass(Map.of());
        collector.action = () -> collector.emit(0L);
        collector.collect(4, recording(results));
        assertEquals(1, results.size());
        assertEquals(4, results.get(0).getIndex());
        assertEquals(0L, metric(results.get(0)).getValueMessagesInTotal());
    }

    @Test
    public void busyRoundCompletesRealWrapperAndCannotReplaceActiveBuffer() throws Exception {
        Probe collector = configured();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<RestoreData> runningResult = new AtomicReference<>();
        DataSyncHandlerWrapper running = mock(DataSyncHandlerWrapper.class);
        doAnswer(invocation -> {
            runningResult.set(invocation.getArgument(0));
            return null;
        }).when(running).sync(any(RestoreData.class));
        collector.action = () -> {
            collector.emit(8L);
            entered.countDown();
            await(release);
            collector.emit(9L);
        };
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> collector.collect(1, running));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            AtomicInteger completions = new AtomicInteger();
            DataSyncHandler handler = new DataSyncHandler() {
                @Override
                public void padding(DataSyncHandlerWrapper wrapper) {
                    completions.incrementAndGet();
                }
            };
            DataSyncHandlerWrapper skipped = handler.getDataSyncHandlerWrapper(1);
            collector.collect(0, skipped);
            assertEquals(1, completions.get());
            collector.collect(0, handler.getDataSyncHandlerWrapper(1));
            assertEquals(2, completions.get());
        } finally {
            release.countDown();
        }
        future.get(5, TimeUnit.SECONDS);
        assertEquals(2, runningResult.get().getDataMap().get(KafkaBrokerTrafficMetrics.class).size());
        collector.action = () -> collector.emit(10L);
        List<RestoreData> next = new ArrayList<>();
        collector.collect(4, recording(next));
        assertNotSame(runningResult.get(), next.get(0));
        runningResult.get().restore();
        assertEquals(10L, metric(next.get(0)).getValueMessagesInTotal());
        assertEquals(12L, metric(next.get(0)).getRuntimeId());
        assertEquals(11L, metric(next.get(0)).getClustersId());
    }

    @Test
    public void callbackFailureDoesNotRetryAndStateStillRecovers() {
        Probe collector = configured();
        AtomicInteger notifications = new AtomicInteger();
        DataSyncHandlerWrapper wrapper = mock(DataSyncHandlerWrapper.class);
        doAnswer(invocation -> {
            notifications.incrementAndGet();
            throw new IllegalStateException("write failed");
        }).when(wrapper).sync(any(RestoreData.class));
        assertThrows(IllegalStateException.class, () -> collector.collect(0, wrapper));
        List<RestoreData> next = new ArrayList<>();
        collector.action = () -> collector.emit(1L);
        collector.collect(1, recording(next));
        assertEquals(1, notifications.get());
        assertEquals(1L, metric(next.get(0)).getValueMessagesInTotal());
    }

    @Test
    public void fatalFailureStillCompletesWrapperAndReleasesState() {
        Probe collector = configured();
        AtomicInteger completions = new AtomicInteger();
        DataSyncHandler handler = new DataSyncHandler() {
            @Override
            public void padding(DataSyncHandlerWrapper wrapper) {
                completions.incrementAndGet();
            }
        };
        collector.action = () -> {
            collector.emit(8L);
            throw new AssertionError("fatal collection failure");
        };
        DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(1);
        assertThrows(AssertionError.class, () -> collector.collect(0, wrapper));
        assertEquals(1, completions.get());
        collector.action = () -> collector.emit(2L);
        List<RestoreData> next = new ArrayList<>();
        collector.collect(1, recording(next));
        assertEquals(2L, metric(next.get(0)).getValueMessagesInTotal());
    }

    private static DataSyncHandlerWrapper recording(List<RestoreData> results) {
        DataSyncHandlerWrapper wrapper = mock(DataSyncHandlerWrapper.class);
        doAnswer(invocation -> {
            results.add(invocation.getArgument(0));
            return null;
        }).when(wrapper).sync(any(RestoreData.class));
        return wrapper;
    }

    private static KafkaBrokerTrafficMetrics metric(RestoreData data) {
        return (KafkaBrokerTrafficMetrics) data.getDataMap().get(KafkaBrokerTrafficMetrics.class).get(0);
    }

    private static Probe configured() {
        final Probe collector = new Probe();
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setOrganizationId(1L);
        collector.setClusterMetadata(cluster);
        CollectMetadata collect = new CollectMetadata();
        collect.setClusterId(11L);
        collector.setCollectMetadata(collect);
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(12L);
        collector.setRuntimeMetadata(runtime);
        return collector;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for collection", e);
        }
    }

    private static final class Probe extends JMXCollect {

        private Runnable action = () -> { };

        @Override
        protected void doJmxCollect() {
            action.run();
        }

        private void emit(long value) {
            KafkaBrokerTrafficMetrics metric = new KafkaBrokerTrafficMetrics();
            metric.setValueMessagesInTotal(value);
            setData(metric);
        }
    }
}
