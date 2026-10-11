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
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConfigConvertMetaData;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.kafka.AbstractKafkaRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaConfigRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaGroupRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class KafkaGroupConfigSyncTest {

    @org.junit.jupiter.api.Test
    @DisplayName("边界诊断：当前 Config 持久化转换会丢失 Kafka 目标名称")
    void configEntityRoundTripCurrentlyLosesRemoteTargetName() {
        ConfigMetadata metadata = new ConfigMetadata();
        metadata.setInstanceType(MetadataType.TOPIC);
        metadata.setInstanceName("console-config-target-diagnostic");
        metadata.setConfigName("retention.ms");
        metadata.setConfigValue("180000");

        ConfigMetadata restored = ConfigConvertMetaData.INSTANCE.toMetaData(ConfigConvertMetaData.INSTANCE.toEntity(metadata));

        Assertions.assertEquals(MetadataType.TOPIC, restored.getInstanceType());
        Assertions.assertNull(restored.getInstanceName());
        log.info("【已知边界诊断】ConfigEntity 转换保留 instanceType={}，但丢失 Kafka instanceName；该测试记录当前限制，不代表控制台持久化链路通过",
            restored.getInstanceType());
    }

    @ParameterizedTest
    @EnumSource(value = ClusterType.class, names = {"STORAGE_KAFKA_BROKER", "STORAGE_KAFKA_RAFT"})
    @DisplayName("模拟响应：控制台 Group 映射支持查询和删除")
    void groupMappingQueriesAndDeletes(ClusterType clusterType) {
        AdminClient client = Mockito.mock(AdminClient.class);
        DataMetadataHandler<BaseClusterIdBase> handler = createHandler(DatabaseAndMetadataType.GROUP, clusterType, client);
        ListConsumerGroupsResult listed = Mockito.mock(ListConsumerGroupsResult.class);
        Mockito.when(client.listConsumerGroups(ArgumentMatchers.any(ListConsumerGroupsOptions.class))).thenReturn(listed);
        Mockito.when(listed.all()).thenReturn(KafkaFuture.completedFuture(List.of(new ConsumerGroupListing("sync-group", false))));
        ConsumerGroupDescription description = Mockito.mock(ConsumerGroupDescription.class);
        Mockito.when(description.groupId()).thenReturn("sync-group");
        Mockito.when(description.members()).thenReturn(List.of());
        DescribeConsumerGroupsResult described = Mockito.mock(DescribeConsumerGroupsResult.class);
        Mockito.when(client.describeConsumerGroups(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeConsumerGroupsOptions.class)))
            .thenReturn(described);
        Mockito.when(described.all()).thenReturn(KafkaFuture.completedFuture(Map.of("sync-group", description)));
        DeleteConsumerGroupsResult deleted = Mockito.mock(DeleteConsumerGroupsResult.class);
        Mockito.when(client.deleteConsumerGroups(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DeleteConsumerGroupsOptions.class)))
            .thenReturn(deleted);
        Mockito.when(deleted.all()).thenReturn(KafkaFuture.completedFuture(null));

        List<BaseClusterIdBase> groups = handler.getData();
        Assertions.assertNotNull(groups);
        Assertions.assertEquals(1, groups.size());
        GroupMetadata group = Assertions.assertInstanceOf(GroupMetadata.class, groups.get(0));
        Assertions.assertEquals("sync-group", group.getName());
        handler.handleAll(List.of(), List.of(), List.of(), List.of(group));

        Mockito.verify(client).describeConsumerGroups(ArgumentMatchers.eq(List.of("sync-group")),
            ArgumentMatchers.any(DescribeConsumerGroupsOptions.class));
        Mockito.verify(client).deleteConsumerGroups(ArgumentMatchers.eq(List.of("sync-group")),
            ArgumentMatchers.any(DeleteConsumerGroupsOptions.class));
        log.info("【模拟验证通过】{} 控制台 Group 查询和删除分派正确，未连接真实 Broker", clusterType);
    }

    @ParameterizedTest
    @EnumSource(value = ClusterType.class, names = {"STORAGE_KAFKA_BROKER", "STORAGE_KAFKA_RAFT"})
    @DisplayName("模拟响应：控制台 Config 映射支持查询、设置和删除")
    @SuppressWarnings("unchecked")
    void configMappingQueriesSetsAndDeletes(ClusterType clusterType) {
        AdminClient client = Mockito.mock(AdminClient.class);
        DataMetadataHandler<BaseClusterIdBase> handler = createHandler(DatabaseAndMetadataType.CONFIG, clusterType, client);
        DescribeClusterResult cluster = Mockito.mock(DescribeClusterResult.class);
        Mockito.when(client.describeCluster(ArgumentMatchers.any(DescribeClusterOptions.class))).thenReturn(cluster);
        Mockito.when(cluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of()));
        ListTopicsResult topics = Mockito.mock(ListTopicsResult.class);
        Mockito.when(client.listTopics(ArgumentMatchers.any(ListTopicsOptions.class))).thenReturn(topics);
        Mockito.when(topics.names()).thenReturn(KafkaFuture.completedFuture(Set.of("sync-topic")));
        ConfigResource target = new ConfigResource(ConfigResource.Type.TOPIC, "sync-topic");
        DescribeConfigsResult described = Mockito.mock(DescribeConfigsResult.class);
        Mockito.when(client.describeConfigs(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeConfigsOptions.class)))
            .thenReturn(described);
        Mockito.when(described.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
            new ConfigResource(ConfigResource.Type.BROKER, ""), new Config(List.of()),
            target, new Config(List.of(new ConfigEntry("retention.ms", "60000"))))));
        AlterConfigsResult altered = Mockito.mock(AlterConfigsResult.class);
        Mockito.when(client.incrementalAlterConfigs(ArgumentMatchers.anyMap(), ArgumentMatchers.any(AlterConfigsOptions.class)))
            .thenReturn(altered);
        Mockito.when(altered.all()).thenReturn(KafkaFuture.completedFuture(null));

        List<BaseClusterIdBase> configs = handler.getData();
        Assertions.assertNotNull(configs);
        Assertions.assertEquals(1, configs.size());
        ConfigMetadata config = Assertions.assertInstanceOf(ConfigMetadata.class, configs.get(0));
        Assertions.assertEquals(MetadataType.TOPIC, config.getInstanceType());
        Assertions.assertEquals("sync-topic", config.getInstanceName());
        Assertions.assertEquals("retention.ms", config.getConfigName());
        Assertions.assertEquals("60000", config.getConfigValue());
        handler.handleAll(List.of(), List.of(config), List.of(), List.of());
        config.setConfigValue("90000");
        handler.handleAll(List.of(), List.of(), List.of(config), List.of());
        handler.handleAll(List.of(), List.of(), List.of(), List.of(config));

        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> changes = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client, Mockito.times(3)).incrementalAlterConfigs(changes.capture(), ArgumentMatchers.any(AlterConfigsOptions.class));
        List<Map<ConfigResource, Collection<AlterConfigOp>>> calls = changes.getAllValues();
        for (int index = 0; index < calls.size(); index++) {
            Map<ConfigResource, Collection<AlterConfigOp>> call = calls.get(index);
            Assertions.assertEquals(Set.of(target), call.keySet());
            Assertions.assertEquals(1, call.get(target).size());
            AlterConfigOp operation = call.get(target).iterator().next();
            Assertions.assertEquals("retention.ms", operation.configEntry().name());
            Assertions.assertEquals(index == 2 ? AlterConfigOp.OpType.DELETE : AlterConfigOp.OpType.SET, operation.opType());
            Assertions.assertEquals(index == 2 ? null : index == 0 ? "60000" : "90000", operation.configEntry().value());
        }
        log.info("【模拟验证通过】{} 控制台 Config 查询、ADD/UPDATE 设置及 DELETE 分派正确，未连接真实 Broker", clusterType);
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseAndMetadataType.class, names = {"GROUP", "CONFIG"})
    @DisplayName("映射回归：Group 和 Config 的非 Kafka 目标保持原接口")
    void nonKafkaTargetsKeepDefaultContract(DatabaseAndMetadataType type) {
        DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
        for (ClusterType clusterType : ClusterType.values()) {
            if (clusterType != ClusterType.STORAGE_KAFKA_BROKER && clusterType != ClusterType.STORAGE_KAFKA_RAFT) {
                Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(clusterType));
            }
        }
        Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(null));
        log.info("【验证通过】{} 非 Kafka 集群仍使用默认接口", type);
    }

    private DataMetadataHandler<BaseClusterIdBase> createHandler(DatabaseAndMetadataType type, ClusterType clusterType, AdminClient client) {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(860000L + clusterType.ordinal());
        runtime.setClusterId(870000L + clusterType.ordinal());
        runtime.setClusterType(clusterType);
        DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        factory.setDatabaseAndMetadataMapper(mapper);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.getSyncMetadataCreateFactoryMap().put(mapper.getMetaType(), factory);
        MetadataSyncManage.MetadataSyncConfig config = ReflectionTestUtils.invokeMethod(manager, "createMetadataSyncConfig",
            mapper.getMetaType(), new MetadataSyncResult(), runtime);
        Assertions.assertNotNull(config);
        Remoting2Manage.RemotingService<?> remote = Assertions.assertInstanceOf(Remoting2Manage.RemotingService.class, config.getClusterService());
        Class<? extends AbstractKafkaRemotingService> expected = type == DatabaseAndMetadataType.GROUP
            ? KafkaGroupRemotingService.class : KafkaConfigRemotingService.class;
        AbstractKafkaRemotingService service = Assertions.assertInstanceOf(expected, remote.getExecution());
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service.setClientWrapper(wrapper);
        return config.getClusterService();
    }
}
