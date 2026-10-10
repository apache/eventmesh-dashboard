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


package org.apache.eventmesh.dashboard.core.remoting.kafka.mock;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.DeleteConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.UpdateConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResponse.Status;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaConfigRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaGroupRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaOffsetRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTestLog;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@ExtendWith(KafkaTestLog.class)
class KafkaAdminServicesTest {

    private AdminClient client;
    private KafkaGroupRemotingService groups;
    private KafkaOffsetRemotingService offsets;
    private KafkaConfigRemotingService configs;
    private final TopicPartition partition = new TopicPartition("topic-a", 0);

    @BeforeEach
    void setUp() {
        client = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        groups = new KafkaGroupRemotingService();
        groups.setClientWrapper(wrapper);
        offsets = new KafkaOffsetRemotingService();
        offsets.setClientWrapper(wrapper);
        configs = new KafkaConfigRemotingService();
        configs.setClientWrapper(wrapper);
    }

    /** 消费组完整描述转换为名称和成员数。 */
    @Test
    @DisplayName("模拟响应：消费组完整描述转换为名称和成员数")
    void groupQueryConvertsCompleteDescriptions() throws Exception {
        Mockito.when(client.listConsumerGroups(Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(List.of(
            new ConsumerGroupListing("group-a", false))));
        Mockito.when(client.describeConsumerGroups(Mockito.anyCollection(), Mockito.any()).all())
            .thenReturn(KafkaFuture.completedFuture(Map.of("group-a", group(ConsumerGroupState.EMPTY))));
        var result = groups.getAllGroups(null);
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals("group-a", result.getData().get(0).getName());
        Assertions.assertEquals(0, result.getData().get(0).getMemberCount());
        Assertions.assertNull(result.getData().get(0).getRetryQueueNums());
    }

    /** 消费组查询失败不能返回部分成功。 */
    @Test
    @DisplayName("模拟响应：消费组查询失败不能返回部分成功")
    void groupQueryDoesNotReturnPartialListOnFailure() {
        Mockito.when(client.listConsumerGroups(Mockito.any()).all()).thenReturn(failed(new GroupAuthorizationException("group-a")));
        Assertions.assertThrows(ExecutionException.class, () -> groups.getAllGroups(null));
    }

    /** 消费组描述缺失时明确失败。 */
    @Test
    @DisplayName("模拟响应：消费组描述缺失时明确失败")
    void groupQueryRejectsMissingDescription() {
        Mockito.when(client.listConsumerGroups(Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(List.of(
            new ConsumerGroupListing("group-a", false))));
        Mockito.when(client.describeConsumerGroups(Mockito.anyCollection(), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        Assertions.assertThrows(IllegalStateException.class, () -> groups.getAllGroups(null));
    }

    /** 删除消费组必须指定名称。 */
    @Test
    @DisplayName("模拟响应：删除消费组必须指定名称")
    void groupDeletionRequiresExplicitName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> groups.deleteGroup(null));
        Mockito.verifyNoInteractions(client);
    }

    /** 未提交位点保持空且不伪造消息时间戳。 */
    @Test
    @DisplayName("模拟响应：未提交位点保持空且不伪造消息时间戳")
    void offsetQueryPreservesUncommittedAndDoesNotInventTimestamp() throws Exception {
        topic();
        Mockito.when(client.listConsumerGroupOffsets(Mockito.anyString(), Mockito.any()).partitionsToOffsetAndMetadata())
            .thenReturn(KafkaFuture.completedFuture(Map.of()));
        logOffsets();
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName("group-a");
        request.setTopic("topic-a");
        var row = offsets.getOffsets(request).getData().get(0);
        Assertions.assertNull(row.getOffset());
        Assertions.assertEquals(10L, row.getBrokerOffset());
        Assertions.assertNull(row.getLastTimestamp());
        Assertions.assertNull(row.getBrokerName());
    }

    /** 没有提交位点时不扩大为全 Topic 查询。 */
    @Test
    @DisplayName("模拟响应：没有提交位点时不扩大为全 Topic 查询")
    void emptyCommittedOffsetsDoNotQueryAllTopics() throws Exception {
        Mockito.when(client.listConsumerGroupOffsets(Mockito.anyString(), Mockito.any()).partitionsToOffsetAndMetadata())
            .thenReturn(KafkaFuture.completedFuture(Map.of()));
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName("group-a");
        Assertions.assertTrue(offsets.getOffsets(request).getData().isEmpty());
        Mockito.verify(client, Mockito.never()).describeTopics(Mockito.anyCollection(), Mockito.any());
        Mockito.verify(client, Mockito.never()).listOffsets(Mockito.anyMap(), Mockito.any());
    }

    /** 位点查询保留原始失败。 */
    @Test
    @DisplayName("模拟响应：位点查询保留原始失败")
    void queryDoesNotHideOffsetFailure() {
        Mockito.when(client.listConsumerGroupOffsets(Mockito.anyString(), Mockito.any()).partitionsToOffsetAndMetadata())
            .thenReturn(failed(new GroupAuthorizationException("group-a")));
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName("group-a");
        Assertions.assertThrows(ExecutionException.class, () -> offsets.getOffsets(request));
    }

    /** 操作参数不能覆盖托管客户端地址。 */
    @Test
    @DisplayName("模拟响应：操作参数不能覆盖托管客户端地址")
    void operationCannotOverrideManagedAddress() {
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName("group-a");
        request.setBootstrapServers("different-host:9092");
        Assertions.assertThrows(IllegalArgumentException.class, () -> offsets.getOffsets(request));
        Mockito.verifyNoInteractions(client);
    }

    /** 活跃消费组在重置前被拒绝。 */
    @Test
    @DisplayName("模拟响应：活跃消费组在重置前被拒绝")
    void activeGroupIsRejectedBeforeAlteration() {
        prepareReset(ConsumerGroupState.STABLE);
        Assertions.assertThrows(IllegalStateException.class, () -> offsets.resetOffsets(reset()));
        Mockito.verify(client, Mockito.never()).alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    }

    /** 非法分区和越界位点不产生修改。 */
    @Test
    @DisplayName("模拟响应：非法分区和越界位点不产生修改")
    void invalidPartitionAndOutOfRangeOffsetNeverAlter() {
        prepareReset(ConsumerGroupState.EMPTY);
        ResetOffsetRequest request = reset();
        request.setPartitionId(8);
        Assertions.assertThrows(IllegalArgumentException.class, () -> offsets.resetOffsets(request));
        request.setPartitionId(0);
        request.setOffset(11L);
        Assertions.assertThrows(IllegalArgumentException.class, () -> offsets.resetOffsets(request));
        Mockito.verify(client, Mockito.never()).alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    }

    /** 没有匹配时间戳时不能自动重置到末尾。 */
    @Test
    @DisplayName("模拟响应：没有匹配时间戳时不能自动重置到末尾")
    void unmatchedTimestampDoesNotSilentlyResetToEnd() {
        prepareReset(ConsumerGroupState.EMPTY);
        ResetOffsetRequest request = reset();
        request.setOffset(null);
        request.setTimestamp(100L);
        request.setResetOffsetMode(ResetOffsetMode.CONSUME_FROM_TIMESTAMP);
        Assertions.assertThrows(IllegalArgumentException.class, () -> offsets.resetOffsets(request));
        Mockito.verify(client, Mockito.never()).alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    }

    /** 分区部分成功时返回各分区状态和原始错误。 */
    @Test
    @DisplayName("模拟响应：分区部分成功时返回各分区状态和原始错误")
    @SuppressWarnings("unchecked")
    void resetReportsPartialSuccessAndPreservesCause() throws Exception {
        prepareReset(ConsumerGroupState.EMPTY);
        AlterConsumerGroupOffsetsResult changed = Mockito.mock(AlterConsumerGroupOffsetsResult.class);
        Mockito.when(client.alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any())).thenReturn(changed);
        Mockito.when(changed.partitionResult(partition)).thenReturn(KafkaFuture.completedFuture(null));
        GroupAuthorizationException denied = new GroupAuthorizationException("group-a");
        Mockito.when(changed.partitionResult(new TopicPartition("topic-a", 1))).thenReturn(failed(denied));
        ResetOffsetRequest request = reset();
        request.setPartitionId(null);
        var result = offsets.resetOffsets(request);
        Assertions.assertEquals(207, result.getCode());
        Assertions.assertEquals(Status.SUCCESS, result.getData().get(0).getStatus());
        Assertions.assertEquals(Status.FAILED, result.getData().get(1).getStatus());
        Assertions.assertSame(denied, result.getData().get(1).getThrowable());
        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> targets = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client).alterConsumerGroupOffsets(Mockito.eq("group-a"), targets.capture(), Mockito.any());
        Assertions.assertEquals(2, targets.getValue().size());
        Assertions.assertEquals(4, targets.getValue().get(partition).offset());
    }

    /** 重置超时返回结果未知。 */
    @Test
    @DisplayName("模拟响应：重置超时返回结果未知")
    @SuppressWarnings("unchecked")
    void timeoutIsUnknownNotFailed() throws Exception {
        prepareReset(ConsumerGroupState.EMPTY);
        KafkaFuture<Void> future = Mockito.mock(KafkaFuture.class);
        Mockito.when(future.get(Mockito.anyLong(), Mockito.eq(TimeUnit.NANOSECONDS))).thenThrow(new TimeoutException("timeout"));
        Mockito.when(client.alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any()).partitionResult(partition))
            .thenReturn(future);
        var result = offsets.resetOffsets(reset());
        Assertions.assertEquals(207, result.getCode());
        Assertions.assertEquals(Status.UNKNOWN, result.getData().get(0).getStatus());
    }

    /** 重置中断保留中断标记并返回结果未知。 */
    @Test
    @DisplayName("模拟响应：重置中断保留中断标记并返回结果未知")
    @SuppressWarnings("unchecked")
    void interruptedResetPreservesFlagAndUnknownOutcome() throws Exception {
        prepareReset(ConsumerGroupState.EMPTY);
        KafkaFuture<Void> future = Mockito.mock(KafkaFuture.class);
        Mockito.when(future.get(Mockito.anyLong(), Mockito.eq(TimeUnit.NANOSECONDS))).thenThrow(new InterruptedException());
        Mockito.when(client.alterConsumerGroupOffsets(Mockito.anyString(), Mockito.anyMap(), Mockito.any()).partitionResult(partition))
            .thenReturn(future);
        try {
            Assertions.assertEquals(Status.UNKNOWN, offsets.resetOffsets(reset()).getData().get(0).getStatus());
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    /** 配置请求拒绝无效或冲突的目标。 */
    @Test
    @DisplayName("模拟响应：配置请求拒绝无效或冲突的目标")
    void configRejectsInvalidScopeAndAmbiguousResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.getConfigs(null));
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.TOPIC);
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.getConfigs(request));
        request.setConfigType(ConfigType.NODE);
        request.setNode("");
        request.setConfigObjectName("topic-a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.getConfigs(request));
        request.setConfigObjectName(null);
        request.setConfigType(ConfigType.NODE);
        request.setNode("-1");
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.getConfigs(request));
        Mockito.verifyNoInteractions(client);
    }

    /** 配置查询不返回敏感值。 */
    @Test
    @DisplayName("模拟响应：配置查询不返回敏感值")
    void configQuerySuppressesSensitiveValues() throws Exception {
        ConfigEntry secret = Mockito.mock(ConfigEntry.class);
        Mockito.when(secret.name()).thenReturn("password");
        Mockito.when(secret.value()).thenReturn("must-not-leak");
        Mockito.when(secret.isSensitive()).thenReturn(true);
        Mockito.when(secret.source()).thenReturn(ConfigEntry.ConfigSource.DYNAMIC_BROKER_CONFIG);
        Config config = new Config(List.of(secret));
        Mockito.when(client.describeConfigs(Mockito.anyCollection(), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(
            Map.of(new ConfigResource(ConfigResource.Type.BROKER, ""), config)));
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.NODE);
        request.setNode("");
        var row = configs.getConfigs(request).getData().get(0);
        Assertions.assertTrue(row.getSensitive());
        Assertions.assertNull(row.getConfigValue());
        Assertions.assertEquals("DYNAMIC_BROKER_CONFIG", row.getSource());
    }

    /** 配置修改只设置显式传入的键和目标。 */
    @Test
    @DisplayName("模拟响应：配置修改只设置显式传入的键和目标")
    @SuppressWarnings("unchecked")
    void configUpdateOnlySetsExplicitKeysAndTarget() throws Exception {
        Mockito.when(client.incrementalAlterConfigs(Mockito.anyMap(), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(null));
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.NODE);
        request.setNode("1");
        UpdateConfigRequest update = updateConfig(request, "log.retention.ms", "60000");
        Mockito.clearInvocations(client);
        Assertions.assertEquals(200, configs.updateConfigs(update).getCode());
        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> changes = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client).incrementalAlterConfigs(changes.capture(), Mockito.any());
        Assertions.assertEquals(1, changes.getValue().size());
        var ops = changes.getValue().get(new ConfigResource(ConfigResource.Type.BROKER, "1"));
        Assertions.assertEquals(1, ops.size());
        Assertions.assertEquals(AlterConfigOp.OpType.SET, ops.iterator().next().opType());
        Assertions.assertEquals("60000", ops.iterator().next().configEntry().value());
    }

    /** 公共配置请求不允许混合单条与批量内容，也不能静默执行全量替换。 */
    @Test
    @DisplayName("模拟响应：公共配置写入拒绝含糊和错误类型")
    void sharedConfigRejectsAmbiguousPayload() {
        GetConfigRequest query = new GetConfigRequest();
        query.setNode("1");
        UpdateConfigRequest request = updateConfig(query, "log.retention.ms", "60000");
        request.setFullConfig(List.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        request.setFullConfig(null);
        request.setIncrementConfig(List.of(request.getMetaData()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        request.setMetaData(null);
        request.setIncrementConfig(List.of("invalid"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        Mockito.verifyNoInteractions(client);
    }

    /** 框架只填 Metadata 时仍能定位 Topic、Broker、默认 Broker，实例数据库 ID 不当作 Broker ID。 */
    @Test
    @DisplayName("模拟响应：Config 仅凭 Metadata 定位三类资源")
    @SuppressWarnings("unchecked")
    void configTargetsComeFromMetadata() throws Exception {
        Mockito.when(client.incrementalAlterConfigs(Mockito.anyMap(), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(null));
        for (MetadataType type : List.of(MetadataType.TOPIC, MetadataType.RUNTIME, MetadataType.CLUSTER)) {
            ConfigMetadata entry = configMetadata(type, type == MetadataType.TOPIC ? "topic-a" : type == MetadataType.RUNTIME ? "1" : "");

            UpdateConfigRequest request = new UpdateConfigRequest();
            request.setMetaData(entry);
            Mockito.clearInvocations(client);
            Assertions.assertEquals(200, configs.updateConfigs(request).getCode());
            ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> changes = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(client).incrementalAlterConfigs(changes.capture(), Mockito.any());
            ConfigResource resource = new ConfigResource(type == MetadataType.TOPIC ? ConfigResource.Type.TOPIC : ConfigResource.Type.BROKER,
                entry.getInstanceName());
            Assertions.assertEquals(Set.of(resource), changes.getValue().keySet());
        }
    }

    /** 目标冲突、未解析实例 ID 和不支持的资源类型均在 RPC 前失败。 */
    @Test
    @DisplayName("模拟响应：Config 拒绝目标冲突和缺失")
    void configRejectsConflictingOrUnresolvedTargets() {
        UpdateConfigRequest request = new UpdateConfigRequest();
        ConfigMetadata entry = configMetadata(MetadataType.TOPIC, "topic-a");
        request.setMetaData(entry);
        request.setNode("1");
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        request.setNode(null);
        entry.setInstanceName(null);
        entry.setInstanceId(1L);
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        entry.setInstanceType(MetadataType.CLUSTER);
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        entry.setInstanceType(MetadataType.GROUP);
        entry.setInstanceName("group-a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> configs.updateConfigs(request));
        Mockito.verifyNoInteractions(client);
    }

    /** DELETE 使用 Kafka 原生删除覆盖操作，不能伪造为设置空字符串。 */
    @Test
    @DisplayName("模拟响应：Config DELETE 删除动态覆盖")
    @SuppressWarnings("unchecked")
    void configDeleteUsesNativeDeleteOperation() throws Exception {
        Mockito.when(client.incrementalAlterConfigs(Mockito.anyMap(), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(null));
        DeleteConfigRequest request = new DeleteConfigRequest();
        request.setMetaData(configMetadata(MetadataType.TOPIC, "topic-a"));
        request.getMetaData().setConfigValue(null);
        Mockito.clearInvocations(client);
        Assertions.assertEquals(200, configs.deleteConfigs(request).getCode());
        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> changes = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client).incrementalAlterConfigs(changes.capture(), Mockito.any());
        var op = changes.getValue().get(new ConfigResource(ConfigResource.Type.TOPIC, "topic-a")).iterator().next();
        Assertions.assertEquals(AlterConfigOp.OpType.DELETE, op.opType());
        Assertions.assertNull(op.configEntry().value());
    }

    /** 全量查询任一资源缺失时应失败，不返回看似完整的部分列表。 */
    @Test
    @DisplayName("模拟响应：Config 全量查询拒绝不完整响应")
    void configQueryAllRejectsIncompleteResponse() {
        Mockito.when(client.describeCluster(Mockito.any()).nodes())
            .thenReturn(KafkaFuture.completedFuture(List.of(new Node(1, "localhost", 9092))));
        Mockito.when(client.listTopics(Mockito.any()).names()).thenReturn(KafkaFuture.completedFuture(Set.of("topic-a")));
        Mockito.when(client.describeConfigs(Mockito.anyCollection(), Mockito.any()).all())
            .thenReturn(KafkaFuture.completedFuture(Map.of(new ConfigResource(ConfigResource.Type.BROKER, ""), new Config(List.of()))));
        Assertions.assertThrows(IllegalStateException.class, () -> configs.getConfigs(new GetConfigRequest()));
    }

    /** 同名配置按资源分别返回，默认 Broker 没有覆盖时允许空列表。 */
    @Test
    @DisplayName("模拟响应：Config 全量查询保留目标且不伪造默认覆盖")
    @SuppressWarnings("unchecked")
    void configQueryAllPreservesTargetsAndEmptyDefaults() throws Exception {
        Mockito.when(client.describeCluster(Mockito.any()).nodes())
            .thenReturn(KafkaFuture.completedFuture(List.of(new Node(1, "localhost", 9092))));
        Mockito.when(client.listTopics(Mockito.any()).names()).thenReturn(KafkaFuture.completedFuture(Set.of("topic-a", "topic-b")));
        var defaults = new ConfigResource(ConfigResource.Type.BROKER, "");
        var broker = new ConfigResource(ConfigResource.Type.BROKER, "1");
        var topicA = new ConfigResource(ConfigResource.Type.TOPIC, "topic-a");
        var topicB = new ConfigResource(ConfigResource.Type.TOPIC, "topic-b");
        Config entry = new Config(List.of(new ConfigEntry("retention.ms", "60000")));
        Mockito.when(client.describeConfigs(Mockito.anyCollection(), Mockito.any()).all())
            .thenReturn(KafkaFuture.completedFuture(Map.of(defaults, new Config(List.of()), broker, entry, topicA, entry, topicB, entry)));
        Mockito.clearInvocations(client);
        var result = configs.getConfigs(new GetConfigRequest());
        Assertions.assertEquals(3, result.getData().size());
        Assertions.assertEquals(3, result.getData().stream().map(ConfigMetadata::nodeUnique).distinct().count());
        Assertions.assertTrue(result.getData().stream().anyMatch(row -> row.getInstanceType() == MetadataType.RUNTIME
            && "1".equals(row.getInstanceName())));
        ArgumentCaptor<Collection<ConfigResource>> targets = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(client).describeConfigs(targets.capture(), Mockito.any());
        Assertions.assertEquals(Set.of(defaults, broker, topicA, topicB), new java.util.HashSet<>(targets.getValue()));
    }

    private ConfigMetadata configMetadata(MetadataType type, String target) {
        ConfigMetadata entry = new ConfigMetadata();
        entry.setInstanceType(type);
        entry.setInstanceName(target);
        entry.setConfigName("retention.ms");
        entry.setConfigValue("60000");
        return entry;
    }

    private UpdateConfigRequest updateConfig(GetConfigRequest query, String name, String value) {
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setConfigType(query.getConfigType());
        request.setNode(query.getNode());
        request.setConfigObjectName(query.getConfigObjectName());
        ConfigMetadata metadata = new ConfigMetadata();
        metadata.setConfigName(name);
        metadata.setConfigValue(value);
        request.setMetaData(metadata);
        return request;
    }

    private ResetOffsetRequest reset() {
        ResetOffsetRequest request = new ResetOffsetRequest();
        request.setGroupName("group-a");
        request.setTopic("topic-a");
        request.setPartitionId(0);
        request.setResetOffsetMode(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET);
        request.setOffset(4L);
        return request;
    }

    private void prepareReset(ConsumerGroupState state) {
        topic();
        logOffsets();
        Mockito.when(client.describeConsumerGroups(Mockito.anyCollection(), Mockito.any()).all())
            .thenReturn(KafkaFuture.completedFuture(Map.of("group-a", group(state))));
    }

    private void topic() {
        Node node = new Node(1, "localhost", 9092);
        TopicDescription description = new TopicDescription("topic-a", false, List.of(
            new TopicPartitionInfo(0, node, List.of(node), List.of(node)), new TopicPartitionInfo(1, node, List.of(node), List.of(node))));
        Mockito.when(client.describeTopics(Mockito.anyCollection(), Mockito.any()).allTopicNames())
            .thenReturn(KafkaFuture.completedFuture(Map.of("topic-a", description)));
    }

    private void logOffsets() {
        Mockito.when(client.listOffsets(Mockito.anyMap(), Mockito.any())).thenAnswer(call -> {
            Map<TopicPartition, OffsetSpec> query = call.getArgument(0);
            Map<TopicPartition, ListOffsetsResultInfo> result = new java.util.LinkedHashMap<>();
            query.forEach((key, spec) -> result.put(key, new ListOffsetsResultInfo(
                spec instanceof OffsetSpec.EarliestSpec ? 0 : spec instanceof OffsetSpec.LatestSpec ? 10 : -1, -1, Optional.empty())));
            var response = Mockito.mock(org.apache.kafka.clients.admin.ListOffsetsResult.class);
            Mockito.when(response.all()).thenReturn(KafkaFuture.completedFuture(result));
            return response;
        });
    }

    private ConsumerGroupDescription group(ConsumerGroupState state) {
        return new ConsumerGroupDescription("group-a", false, List.of(), "range", state, new Node(1, "localhost", 9092));
    }

    private <T> KafkaFuture<T> failed(Throwable cause) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.completeExceptionally(cause);
        return future;
    }
}
