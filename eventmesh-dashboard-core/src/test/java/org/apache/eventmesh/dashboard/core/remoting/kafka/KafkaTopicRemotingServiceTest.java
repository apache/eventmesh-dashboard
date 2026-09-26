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

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKakfaConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.kafka.KafkaAdminOperation;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.internals.KafkaFutureImpl;

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
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class KafkaTopicRemotingServiceTest {

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
    void queryMapsPartitionsAndLeavesUnknownFieldsUnset() throws Exception {
        log.info("【模拟查询】批量查询两个主题，验证分区映射、排序和未知字段");
        list(KafkaFuture.completedFuture(Set.of("topic-b", "topic-a")));
        describe(KafkaFuture.completedFuture(Map.of("topic-a", description("topic-a", 1), "topic-b", description("topic-b", 3))));
        GetTopics2Request request = new GetTopics2Request();
        request.setRuntimeHost("ignored-request-host");
        request.setRuntimePort(1);
        GetTopicsResult result = service.getAllTopics(request);
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(List.of("topic-a", "topic-b"), result.getData().stream().map(TopicMetadata::getTopicName).toList());
        for (int i = 0; i < result.getData().size(); i++) {
            TopicMetadata topic = result.getData().get(i);
            Assertions.assertEquals(i == 0 ? 1 : 3, topic.getReadQueueNum());
            Assertions.assertEquals(topic.getReadQueueNum(), topic.getWriteQueueNum());
            Assertions.assertNull(topic.getId());
            Assertions.assertNull(topic.getRetentionMs());
            Assertions.assertNull(topic.getTopicConfig());
            log.info("主题={}，读分区={}，写分区={}", topic.getTopicName(), topic.getReadQueueNum(), topic.getWriteQueueNum());
        }
        ArgumentCaptor<ListTopicsOptions> options = ArgumentCaptor.forClass(ListTopicsOptions.class);
        Mockito.verify(client).listTopics(options.capture());
        Assertions.assertFalse(options.getValue().shouldListInternal());
        Assertions.assertEquals(10000, options.getValue().timeoutMs());
        Mockito.verify(client).describeTopics(ArgumentMatchers.eq(List.of("topic-a", "topic-b")),
            ArgumentMatchers.any(DescribeTopicsOptions.class));
    }

    @Test
    void emptyListSkipsDescribe() throws Exception {
        log.info("【模拟查询】空集群返回成功空列表，不发起详情查询");
        list(KafkaFuture.completedFuture(Set.of()));
        GetTopicsResult result = service.getAllTopics(new GetTopics2Request());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertTrue(result.getData().isEmpty());
        Mockito.verify(client, Mockito.never()).describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class));
    }

    @Test
    void listFailureRetainsCause() {
        log.info("【模拟异常】列表查询无权限，不转为空列表");
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-a"));
        list(failed(denied));
        ExecutionException error = Assertions.assertThrows(ExecutionException.class, () -> service.getAllTopics(new GetTopics2Request()));
        Assertions.assertSame(denied, error.getCause());
    }

    @Test
    void describeFailureDoesNotReturnPartialSuccess() {
        log.info("【模拟异常】详情查询部分失败，整个查询明确失败");
        list(KafkaFuture.completedFuture(Set.of("topic-a", "topic-b")));
        TopicAuthorizationException denied = new TopicAuthorizationException(Set.of("topic-b"));
        describe(failed(denied));
        ExecutionException error = Assertions.assertThrows(ExecutionException.class, () -> service.getAllTopics(new GetTopics2Request()));
        Assertions.assertSame(denied, error.getCause());
    }

    @Test
    void incompleteDescriptionFails() {
        log.info("【模拟异常】详情缺项，不返回残缺成功列表");
        list(KafkaFuture.completedFuture(Set.of("topic-a", "topic-b")));
        describe(KafkaFuture.completedFuture(Map.of("topic-a", description("topic-a", 1))));
        Assertions.assertThrows(IllegalStateException.class, () -> service.getAllTopics(new GetTopics2Request()));
    }

    @Test
    void missingListFails() {
        log.info("【模拟异常】缺失列表与合法空列表区分");
        list(KafkaFuture.completedFuture(null));
        Assertions.assertThrows(IllegalStateException.class, () -> service.getAllTopics(new GetTopics2Request()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeoutPropagates() throws Exception {
        log.info("【模拟异常】列表等待超时，保留异常");
        KafkaFuture<Set<String>> future = Mockito.mock(KafkaFuture.class);
        TimeoutException timeout = new TimeoutException("query timed out");
        Mockito.when(future.get(10000, TimeUnit.MILLISECONDS)).thenThrow(timeout);
        list(future);
        Assertions.assertSame(timeout, Assertions.assertThrows(TimeoutException.class, () -> service.getAllTopics(new GetTopics2Request())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void interruptionRestoresFlag() throws Exception {
        log.info("【模拟异常】详情等待中断，恢复线程中断标记");
        list(KafkaFuture.completedFuture(Set.of("topic-a")));
        KafkaFuture<Map<String, TopicDescription>> future = Mockito.mock(KafkaFuture.class);
        Mockito.when(future.get(10000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("interrupted"));
        describe(future);
        try {
            Assertions.assertThrows(InterruptedException.class, () -> service.getAllTopics(new GetTopics2Request()));
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void clientRequiresBootstrapAddresses() {
        log.info("【连接校验】缺少集群地址时在创建客户端前报错");
        KafkaAdminOperation operation = new KafkaAdminOperation();
        Assertions.assertThrows(IllegalArgumentException.class, () -> operation.createClient(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> operation.createClient(new CreateKakfaConfig()));
    }

    private void list(KafkaFuture<Set<String>> future) {
        ListTopicsResult result = Mockito.mock(ListTopicsResult.class);
        Mockito.when(result.names()).thenReturn(future);
        Mockito.when(client.listTopics(ArgumentMatchers.any(ListTopicsOptions.class))).thenReturn(result);
    }

    private void describe(KafkaFuture<Map<String, TopicDescription>> future) {
        DescribeTopicsResult result = Mockito.mock(DescribeTopicsResult.class);
        Mockito.when(result.allTopicNames()).thenReturn(future);
        Mockito.when(client.describeTopics(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(DescribeTopicsOptions.class))).thenReturn(result);
    }

    private <T> KafkaFuture<T> failed(Throwable cause) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.completeExceptionally(cause);
        return future;
    }

    private TopicDescription description(String name, int count) {
        Node node = new Node(1, "localhost", 9092);
        List<TopicPartitionInfo> partitions = java.util.stream.IntStream.range(0, count)
            .mapToObj(id -> new TopicPartitionInfo(id, node, List.of(node), List.of(node))).toList();
        return new TopicDescription(name, false, partitions);
    }
}
