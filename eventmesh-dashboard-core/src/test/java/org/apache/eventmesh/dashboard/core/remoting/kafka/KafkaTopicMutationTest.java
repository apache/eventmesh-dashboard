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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import org.apache.eventmesh.dashboard.common.model.metadata.KafkaTopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.kafka.topic.TopicRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class KafkaTopicMutationTest {

    private AdminClient client;
    private KafkaTopicRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(AdminClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new KafkaTopicRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createUsesExplicitPartitionsReplicasAndConfigs() throws Exception {
        log.info("【模拟创建】主题=topic-a，分区=3，副本=2，配置=retention.ms:60000");
        createResult(KafkaFuture.completedFuture(null));
        TopicRequest request = request(3, 2, Map.of("retention.ms", "60000"));
        Assertions.assertEquals(200, service.createTopic(request).getCode());
        ArgumentCaptor<Collection<NewTopic>> topics = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<CreateTopicsOptions> options = ArgumentCaptor.forClass(CreateTopicsOptions.class);
        Mockito.verify(client).createTopics(topics.capture(), options.capture());
        NewTopic topic = topics.getValue().iterator().next();
        Assertions.assertEquals("topic-a", topic.name());
        Assertions.assertEquals(3, topic.numPartitions());
        Assertions.assertEquals(2, topic.replicationFactor());
        Assertions.assertEquals(request.getMetaData().getConfigs(), topic.configs());
        Assertions.assertEquals(10000, options.getValue().timeoutMs());
    }

    @Test
    void createRejectsInvalidInputBeforeRpc() {
        log.info("【创建校验】缺少名称、非法名称、非正分区、副本越界和空配置值均拒绝");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(new TopicRequest()));
        for (String name : List.of("", " ", ".", "..", "bad/name", "x".repeat(250))) {
            TopicRequest request = request(1, 1, null);
            request.getMetaData().setTopicName(name);
            Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(request));
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(request(null, 1, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(request(0, 1, null)));
        for (Integer replicas : new Integer[]{null, 0, -1, 32768}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(request(1, replicas, null)));
        }
        Map<String, String> configs = new HashMap<>();
        configs.put("retention.ms", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createTopic(request(1, 1, configs)));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void createFailurePreservesCause() {
        log.info("【创建异常】Broker 拒绝时不返回成功");
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-a"));
        createResult(failed(denied));
        Assertions.assertSame(denied, Assertions.assertThrows(ExecutionException.class,
            () -> service.createTopic(request(1, 1, null))).getCause());
    }

    @Test
    void deleteTargetsOnlyRequestedTopic() throws Exception {
        log.info("【模拟删除】只删除 topic-a");
        deleteResult(KafkaFuture.completedFuture(null));
        Assertions.assertEquals(200, service.deleteTopic(request(null, null, null)).getCode());
        Mockito.verify(client).deleteTopics(ArgumentMatchers.eq(List.of("topic-a")), ArgumentMatchers.any(DeleteTopicsOptions.class));
    }

    @Test
    void deleteRejectsMissingNameAndPreservesBrokerFailure() {
        log.info("【删除异常】缺少目标不发请求，Broker 无权限保持原始原因");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteTopic(new TopicRequest()));
        Mockito.verifyNoInteractions(client);
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-a"));
        deleteResult(failed(denied));
        Assertions.assertSame(denied, Assertions.assertThrows(ExecutionException.class,
            () -> service.deleteTopic(request(null, null, null))).getCause());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateValidatesBothStagesBeforeExpandingAndSettingConfigs() throws Exception {
        log.info("【模拟更新】分区 1→3，仅设置 retention.ms，先预校验再执行");
        describe(1);
        partitions(KafkaFuture.completedFuture(null));
        configs(KafkaFuture.completedFuture(null), KafkaFuture.completedFuture(null));
        Assertions.assertEquals(200, service.updateTopic(request(3, null, Map.of("retention.ms", "120000"))).getCode());
        ArgumentCaptor<Map<String, NewPartitions>> partitionMap = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> configMap = ArgumentCaptor.forClass(Map.class);
        InOrder order = Mockito.inOrder(client);
        order.verify(client).describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class));
        order.verify(client).createPartitions(partitionMap.capture(), ArgumentMatchers.argThat(CreatePartitionsOptions::validateOnly));
        order.verify(client).incrementalAlterConfigs(configMap.capture(), ArgumentMatchers.argThat(AlterConfigsOptions::shouldValidateOnly));
        order.verify(client).createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.argThat(value -> !value.validateOnly()));
        order.verify(client).incrementalAlterConfigs(ArgumentMatchers.anyMap(), ArgumentMatchers.argThat(value -> !value.shouldValidateOnly()));
        Assertions.assertEquals(3, partitionMap.getValue().get("topic-a").totalCount());
        Collection<AlterConfigOp> operations = configMap.getValue().get(new ConfigResource(ConfigResource.Type.TOPIC, "topic-a"));
        Assertions.assertEquals(1, operations.size());
        AlterConfigOp operation = operations.iterator().next();
        Assertions.assertEquals(AlterConfigOp.OpType.SET, operation.opType());
        Assertions.assertEquals("retention.ms", operation.configEntry().name());
        Assertions.assertEquals("120000", operation.configEntry().value());
    }

    @Test
    void configOnlyUpdateDoesNotChangePartitions() throws Exception {
        log.info("【配置更新】省略分区数时不扩容");
        describe(3);
        configs(KafkaFuture.completedFuture(null), KafkaFuture.completedFuture(null));
        Assertions.assertEquals(200, service.updateTopic(request(null, null, Map.of("retention.ms", "120000"))).getCode());
        Mockito.verify(client, Mockito.never()).createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.any(CreatePartitionsOptions.class));
    }

    @Test
    void equalPartitionCountIsNoOp() throws Exception {
        log.info("【幂等更新】目标分区等于当前分区，无需发送写请求");
        describe(3);
        Assertions.assertEquals(200, service.updateTopic(request(3, null, null)).getCode());
        noWrites();
    }

    @Test
    void shrinkRejectsAllWrites() {
        log.info("【缩容拒绝】分区 3→1 时连配置也不修改");
        describe(3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(1, null, Map.of("retention.ms", "60000"))));
        noWrites();
    }

    @Test
    void invalidUpdateRejectsBeforeRpc() {
        log.info("【更新校验】副本修改、空更新、非法分区和空配置值均拒绝");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(null, 2, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(null, null, Map.of())));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(-1, null, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(null, null, Map.of(" ", "x"))));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void failedDescribeDoesNotCreateOrUpdate() {
        log.info("【查询失败】更新前查询无权限，不把失败当作主题不存在");
        DescribeTopicsResult result = Mockito.mock(DescribeTopicsResult.class);
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-a"));
        Mockito.when(result.allTopicNames()).thenReturn(failed(denied));
        Mockito.when(client.describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class))).thenReturn(result);
        Assertions.assertSame(denied, Assertions.assertThrows(ExecutionException.class,
            () -> service.updateTopic(request(3, null, null))).getCause());
        noWrites();
    }

    @Test
    void failedConfigValidationPreventsExpansion() {
        log.info("【预校验失败】配置不合法时只校验分区，不真正扩容");
        describe(1);
        partitions(KafkaFuture.completedFuture(null));
        configs(failed(new IllegalArgumentException("invalid config")), KafkaFuture.completedFuture(null));
        Assertions.assertThrows(ExecutionException.class, () -> service.updateTopic(request(3, null, Map.of("retention.ms", "bad"))));
        Mockito.verify(client, Mockito.never()).createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.argThat(value -> !value.validateOnly()));
        Mockito.verify(client, Mockito.never()).incrementalAlterConfigs(ArgumentMatchers.anyMap(),
            ArgumentMatchers.argThat(value -> !value.shouldValidateOnly()));
    }

    @Test
    void failedActualConfigUpdateReportsCompletedExpansion() {
        log.info("【部分完成】扩容成功后改配置失败，报告已扩容且保留失败原因");
        describe(1);
        partitions(KafkaFuture.completedFuture(null));
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-a"));
        configs(KafkaFuture.completedFuture(null), failed(denied));
        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
            () -> service.updateTopic(request(3, null, Map.of("retention.ms", "60000"))));
        Assertions.assertTrue(error.getMessage().contains("expanded to 3"));
        Assertions.assertSame(denied, error.getCause().getCause());
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeTimeoutAndInterruptionPropagate() throws Exception {
        log.info("【写入异常】超时不报成功，中断恢复线程标记");
        KafkaFuture<Void> future = Mockito.mock(KafkaFuture.class);
        Mockito.when(future.get(10000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException("timeout"), new InterruptedException("interrupted"));
        createResult(future);
        Assertions.assertThrows(TimeoutException.class, () -> service.createTopic(request(1, 1, null)));
        try {
            Assertions.assertThrows(InterruptedException.class, () -> service.createTopic(request(1, 1, null)));
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private TopicRequest request(Integer partitions, Integer replicas, Map<String, String> configs) {
        KafkaTopicMetadata metadata = new KafkaTopicMetadata();
        metadata.setTopicName("topic-a");
        metadata.setPartitionCount(partitions);
        metadata.setReplicationFactor(replicas);
        metadata.setConfigs(configs);
        TopicRequest request = new TopicRequest();
        request.setMetaData(metadata);
        return request;
    }

    private void describe(int count) {
        Node node = new Node(1, "localhost", 9092);
        List<TopicPartitionInfo> partitionInfo = java.util.stream.IntStream.range(0, count)
            .mapToObj(id -> new TopicPartitionInfo(id, node, List.of(node), List.of(node))).toList();
        DescribeTopicsResult result = Mockito.mock(DescribeTopicsResult.class);
        Mockito.when(result.allTopicNames())
            .thenReturn(KafkaFuture.completedFuture(Map.of("topic-a", new TopicDescription("topic-a", false, partitionInfo))));
        Mockito.when(client.describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class))).thenReturn(result);
    }

    private void createResult(KafkaFuture<Void> future) {
        CreateTopicsResult result = Mockito.mock(CreateTopicsResult.class);
        Mockito.when(result.all()).thenReturn(future);
        Mockito.when(client.createTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(CreateTopicsOptions.class))).thenReturn(result);
    }

    private void deleteResult(KafkaFuture<Void> future) {
        DeleteTopicsResult result = Mockito.mock(DeleteTopicsResult.class);
        Mockito.when(result.all()).thenReturn(future);
        Mockito.when(client.deleteTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DeleteTopicsOptions.class))).thenReturn(result);
    }

    private void partitions(KafkaFuture<Void> future) {
        CreatePartitionsResult result = Mockito.mock(CreatePartitionsResult.class);
        Mockito.when(result.all()).thenReturn(future);
        Mockito.when(client.createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.any(CreatePartitionsOptions.class))).thenReturn(result);
    }

    private void configs(KafkaFuture<Void> validation, KafkaFuture<Void> execution) {
        AlterConfigsResult result = Mockito.mock(AlterConfigsResult.class);
        Mockito.when(result.all()).thenReturn(validation, execution);
        Mockito.when(client.incrementalAlterConfigs(ArgumentMatchers.anyMap(), ArgumentMatchers.any(AlterConfigsOptions.class))).thenReturn(result);
    }

    private void noWrites() {
        Mockito.verify(client, Mockito.never()).createPartitions(ArgumentMatchers.anyMap(), ArgumentMatchers.any(CreatePartitionsOptions.class));
        Mockito.verify(client, Mockito.never()).incrementalAlterConfigs(ArgumentMatchers.anyMap(), ArgumentMatchers.any(AlterConfigsOptions.class));
        Mockito.verify(client, Mockito.never()).createTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(CreateTopicsOptions.class));
    }

    private <T> KafkaFuture<T> failed(Throwable cause) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.completeExceptionally(cause);
        return future;
    }
}
