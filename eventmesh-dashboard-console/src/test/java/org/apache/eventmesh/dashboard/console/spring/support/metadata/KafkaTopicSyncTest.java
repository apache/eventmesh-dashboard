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


package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTopicRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class KafkaTopicSyncTest {

    @ParameterizedTest
    @EnumSource(value = ClusterType.class, names = {"STORAGE_KAFKA_BROKER", "STORAGE_KAFKA_RAFT"})
    @DisplayName("模拟响应：控制台 Topic 映射经同步管理器查询 Kafka")
    void consoleMappingQueriesKafkaThroughSyncManager(ClusterType clusterType) {
        AdminClient client = Mockito.mock(AdminClient.class);
        DataMetadataHandler<BaseClusterIdBase> handler = createHandler(clusterType, client);
        ListTopicsResult listed = Mockito.mock(ListTopicsResult.class);
        Mockito.when(client.listTopics(ArgumentMatchers.any(ListTopicsOptions.class))).thenReturn(listed);
        Mockito.when(listed.names()).thenReturn(KafkaFuture.completedFuture(Set.of("sync-topic")));
        stubDescription(client);

        List<BaseClusterIdBase> topics = handler.getData();

        Assertions.assertNotNull(topics);
        Assertions.assertEquals(1, topics.size());
        TopicMetadata topic = Assertions.assertInstanceOf(TopicMetadata.class, topics.get(0));
        Assertions.assertEquals("sync-topic", topic.getTopicName());
        Assertions.assertEquals(1, topic.getWriteQueueNum());
        Mockito.verify(client).listTopics(ArgumentMatchers.any(ListTopicsOptions.class));
        log.info("【模拟验证通过】控制台映射完成 {} Topic 查询，未连接真实 Broker", clusterType);
    }

    @ParameterizedTest
    @EnumSource(value = ClusterType.class, names = {"STORAGE_KAFKA_BROKER", "STORAGE_KAFKA_RAFT"})
    @DisplayName("模拟响应：控制台 Topic 同步分别分派创建、更新与删除")
    void consoleMappingPreservesSeparateCreateAndUpdate(ClusterType clusterType) {
        AdminClient client = Mockito.mock(AdminClient.class);
        DataMetadataHandler<BaseClusterIdBase> handler = createHandler(clusterType, client);
        CreateTopicsResult created = Mockito.mock(CreateTopicsResult.class);
        Mockito.when(client.createTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(CreateTopicsOptions.class))).thenReturn(created);
        Mockito.when(created.all()).thenReturn(KafkaFuture.completedFuture(null));
        CreatePartitionsResult expanded = Mockito.mock(CreatePartitionsResult.class);
        Mockito.when(client.createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.any(CreatePartitionsOptions.class))).thenReturn(expanded);
        Mockito.when(expanded.all()).thenReturn(KafkaFuture.completedFuture(null));
        DeleteTopicsResult deleted = Mockito.mock(DeleteTopicsResult.class);
        Mockito.when(client.deleteTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DeleteTopicsOptions.class))).thenReturn(deleted);
        Mockito.when(deleted.all()).thenReturn(KafkaFuture.completedFuture(null));
        stubDescription(client);
        TopicMetadata topic = new TopicMetadata();
        topic.setId(1L);
        topic.setTopicName("sync-topic");
        topic.setWriteQueueNum(2);

        handler.handleAll(List.of(), List.of(topic), List.of(topic), List.of(topic));

        Mockito.verify(client, Mockito.times(1)).createTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(CreateTopicsOptions.class));
        Mockito.verify(client).describeTopics(ArgumentMatchers.eq(List.of("sync-topic")), ArgumentMatchers.any(DescribeTopicsOptions.class));
        Mockito.verify(client).createPartitions(ArgumentMatchers.argThat(partitions -> partitions.get("sync-topic").totalCount() == 2),
            ArgumentMatchers.argThat(CreatePartitionsOptions::validateOnly));
        Mockito.verify(client).createPartitions(ArgumentMatchers.argThat(partitions -> partitions.get("sync-topic").totalCount() == 2),
            ArgumentMatchers.argThat(options -> !options.validateOnly()));
        Mockito.verify(client).deleteTopics(ArgumentMatchers.eq(List.of("sync-topic")), ArgumentMatchers.any(DeleteTopicsOptions.class));
        log.info("【模拟验证通过】{} 创建、更新扩分区、删除分派正确，未连接真实 Broker", clusterType);
    }

    @Test
    @DisplayName("映射回归：其他集群仍使用通用 Topic 接口")
    void otherClusterTypesKeepGenericTopicContract() {
        DatabaseAndMetadataMapper mapper = DatabaseAndMetadataType.TOPIC.getDatabaseAndMetadataMapper();
        for (ClusterType clusterType : ClusterType.values()) {
            if (clusterType != ClusterType.STORAGE_KAFKA_BROKER && clusterType != ClusterType.STORAGE_KAFKA_RAFT) {
                Assertions.assertSame(TopicRemotingService.class, mapper.resolveMetadataHandlerClass(clusterType));
            }
        }
        Assertions.assertSame(TopicRemotingService.class, mapper.getMetadataHandlerClass());
        Assertions.assertSame(TopicRemotingService.class, mapper.resolveMetadataHandlerClass(null));
        log.info("【验证通过】非 Kafka 类型保留通用 Topic 接口");
    }

    @Test
    @DisplayName("映射回归：未配置集群覆盖时保留原有接口")
    void absentOverridesKeepDefaultContract() {
        for (DatabaseAndMetadataType type : DatabaseAndMetadataType.values()) {
            if (type != DatabaseAndMetadataType.TOPIC && type != DatabaseAndMetadataType.GROUP && type != DatabaseAndMetadataType.CONFIG) {
                DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
                Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(ClusterType.STORAGE_KAFKA_BROKER));
                Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(ClusterType.STORAGE_KAFKA_RAFT));
            }
        }
        log.info("【验证通过】其他元数据类型保持原有映射");
    }

    @Test
    @DisplayName("元数据同步配置保留同步范围、结果和数据库处理器")
    void metadataSyncConfigPreservesConfiguredFields() {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(840001L);
        runtime.setClusterId(850001L);
        runtime.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        runtime.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        runtime.setFirstToWhom(FirstToWhom.DASHBOARD);

        SyncMetadataCreateFactory factory = Mockito.spy(new SyncMetadataCreateFactory());
        factory.setDatabaseAndMetadataMapper(DatabaseAndMetadataType.TOPIC.getDatabaseAndMetadataMapper());
        MetadataHandler<BaseClusterIdBase> databaseHandler = Mockito.mock(MetadataHandler.class);
        Mockito.doReturn(databaseHandler).when(factory).createDataMetadataHandler(runtime);

        MetadataSyncManage manager = new MetadataSyncManage();
        manager.getSyncMetadataCreateFactoryMap().put(MetadataType.TOPIC, factory);
        MetadataSyncResult syncResult = new MetadataSyncResult();
        MetadataSyncManage.MetadataSyncConfig config = ReflectionTestUtils.invokeMethod(manager, "createMetadataSyncConfig",
            MetadataType.TOPIC, syncResult, runtime);

        Assertions.assertNotNull(config);
        Assertions.assertSame(syncResult, config.getMetadataSyncResult());
        Assertions.assertSame(ClusterTrusteeshipType.SELF, config.getClusterServiceType());
        Assertions.assertSame(FirstToWhom.DASHBOARD, config.getFirstToWhom());
        Assertions.assertSame(databaseHandler, config.getDataBasesHandler());
        Assertions.assertSame(runtime, config.getBaseSyncBase());
        Assertions.assertSame(MetadataType.TOPIC, config.getMetadataType());
        Remoting2Manage.RemotingService<?> remote = Assertions.assertInstanceOf(Remoting2Manage.RemotingService.class,
            config.getClusterService());
        Assertions.assertInstanceOf(KafkaTopicRemotingService.class, remote.getExecution());
    }

    private DataMetadataHandler<BaseClusterIdBase> createHandler(ClusterType clusterType, AdminClient client) {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(840000L + clusterType.ordinal());
        runtime.setClusterId(850000L + clusterType.ordinal());
        runtime.setClusterType(clusterType);
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        factory.setDatabaseAndMetadataMapper(DatabaseAndMetadataType.TOPIC.getDatabaseAndMetadataMapper());
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.getSyncMetadataCreateFactoryMap().put(MetadataType.TOPIC, factory);
        // Exercise the production selection point without starting the scheduler or accessing a database.
        MetadataSyncManage.MetadataSyncConfig config = ReflectionTestUtils.invokeMethod(manager, "createMetadataSyncConfig",
            MetadataType.TOPIC, new MetadataSyncResult(), runtime);
        Assertions.assertNotNull(config);
        Remoting2Manage.RemotingService<?> remote = Assertions.assertInstanceOf(Remoting2Manage.RemotingService.class, config.getClusterService());
        KafkaTopicRemotingService service = Assertions.assertInstanceOf(KafkaTopicRemotingService.class, remote.getExecution());
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service.setClientWrapper(wrapper);
        return config.getClusterService();
    }

    private void stubDescription(AdminClient client) {
        Node node = new Node(1, "localhost", 9092);
        TopicPartitionInfo partition = new TopicPartitionInfo(0, node, List.of(node), List.of(node));
        DescribeTopicsResult described = Mockito.mock(DescribeTopicsResult.class);
        Mockito.when(client.describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class)))
            .thenReturn(described);
        Mockito.when(described.allTopicNames()).thenReturn(KafkaFuture.completedFuture(
            Map.of("sync-topic", new TopicDescription("sync-topic", false, List.of(partition)))));
    }
}
