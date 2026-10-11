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

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerGroupMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaPartitionOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaTopicMetrics;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKakfaConfig;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaCollectorsTest {

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
        when(latest.all()).thenReturn(KafkaFuture.completedFuture(Map.of(firstPartition, firstLatest, secondPartition, secondLatest)));
        when(earliest.all()).thenReturn(KafkaFuture.completedFuture(Map.of(firstPartition, firstEarliest, secondPartition, secondEarliest)));
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
            protected JMXConnector connect(JMXServiceURL url) throws IOException {
                assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", url.toString());
                return connector;
            }
        };
        collector.setClusterMetadata(cluster());
        collector.setRuntimeMetadata(broker());
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
            protected JMXConnector connect(JMXServiceURL url) {
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
        assertNull(traffic.getValueBytesInTotal());
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

    private CollectMetadata collect() {
        CollectMetadata collect = new CollectMetadata();
        collect.setClusterId(11L);
        return collect;
    }
}
