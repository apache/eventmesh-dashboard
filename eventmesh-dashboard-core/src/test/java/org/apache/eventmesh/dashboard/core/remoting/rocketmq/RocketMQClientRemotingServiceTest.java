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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.client.GetClientsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.Connection;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.ProducerInfo;
import org.apache.rocketmq.remoting.protocol.body.ProducerTableInfo;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQClientRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQClientRemotingService service;

    @BeforeEach
    void setUp() throws Exception {
        this.client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, this.client);
        this.service = new RocketMQClientRemotingService();
        this.service.setClientWrapper(wrapper);
        this.respond(ResponseCode.SUCCESS, null);
    }

    private void respond(int code, String body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "test-result");
        response.setBody(body == null ? null : body.getBytes(StandardCharsets.UTF_8));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
    }

    private RemotingCommand captured() throws Exception {
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(this.client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        return captor.getValue();
    }

    @Test
    void mergesProducerAndConsumerConnectionsWithoutDuplicates() throws Exception {
        log.info("【客户端模拟测试】合并生产者与消费者，同一连接去重");
        this.responses(false, false);
        GetClientsResult result = this.service.getClientList();
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(2, result.getData().size());
        ClientMetadata ipv6 = result.getData().stream().filter(c -> c.getName().equals("consumer")).findFirst().orElseThrow();
        Assertions.assertEquals("::1", ipv6.getHost());
        Assertions.assertEquals(2000, ipv6.getPort());
        Assertions.assertEquals("JAVA", ipv6.getLanguage());
        Assertions.assertNull(ipv6.getPid());
    }

    @Test
    void ignoresOfflineGroups() throws Exception {
        log.info("【客户端模拟测试】离线消费组不产生客户端记录");
        this.responses(true, false);
        Assertions.assertEquals(1, this.service.getClientList().getData().size());
    }

    @Test
    void doesNotReturnPartialDataOnConsumerFailure() throws Exception {
        log.info("【客户端模拟测试】消费者查询失败时，不返回部分成功的生产者列表");
        this.responses(false, true);
        GetClientsResult result = this.service.getClientList();
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertNull(result.getData());
    }

    @Test
    void emptyBrokerReturnsEmptyList() throws Exception {
        log.info("【客户端模拟测试】无生产者和消费组时返回空列表");
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenAnswer(call -> {
            RemotingCommand command = call.getArgument(0);
            return this.ok(command.getCode() == RequestCode.GET_ALL_PRODUCER_INFO
                ? new ProducerTableInfo(Map.of()) : new SubscriptionGroupWrapper());
        });
        Assertions.assertTrue(this.service.getClientList().getData().isEmpty());
    }

    @Test
    void preservesProducerQueryErrors() throws Exception {
        log.info("【客户端模拟测试】保留不支持的 Broker 请求码");
        this.respond(ResponseCode.REQUEST_CODE_NOT_SUPPORTED, null);
        Assertions.assertEquals(10000 + ResponseCode.REQUEST_CODE_NOT_SUPPORTED, this.service.getClientList().getCode());
        Assertions.assertEquals(RequestCode.GET_ALL_PRODUCER_INFO, this.captured().getCode());
    }

    @Test
    void rejectsMissingAndMalformedBodies() throws Exception {
        log.info("【客户端模拟测试】空响应和缺字段响应不能当作空客户端列表");
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getClientList());
        this.respond(ResponseCode.SUCCESS, "{}");
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getClientList());
    }

    @Test
    void rejectsMalformedEndpoint() throws Exception {
        log.info("【客户端模拟测试】拒绝缺少端口的客户端地址");
        RemotingCommand response = this.ok(new ProducerTableInfo(Map.of("group",
            List.of(new ProducerInfo("client", "bad-address", LanguageCode.JAVA, 1, 1)))));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getClientList());
    }

    @Test
    void propagatesNetworkTimeout() throws Exception {
        log.info("【客户端模拟测试】网络超时直接上抛");
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("test-broker", 3000));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> this.service.getClientList());
    }

    private void responses(boolean offline, boolean failure) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenAnswer(call -> {
            int index = calls.getAndIncrement();
            RemotingCommand command = call.getArgument(0);
            if (index == 0) {
                Assertions.assertEquals(RequestCode.GET_ALL_PRODUCER_INFO, command.getCode());
                ProducerInfo producer = new ProducerInfo("shared", "127.0.0.1:1000", LanguageCode.JAVA, 1, 1);
                return this.ok(new ProducerTableInfo(Map.of("g1", List.of(producer), "g2", List.of(producer))));
            }
            if (index == 1) {
                Assertions.assertEquals(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, command.getCode());
                SubscriptionGroupWrapper groups = new SubscriptionGroupWrapper();
                groups.getSubscriptionGroupTable().put("consumer-group", new SubscriptionGroupConfig());
                return this.ok(groups);
            }
            Assertions.assertEquals(RequestCode.GET_CONSUMER_CONNECTION_LIST, command.getCode());
            if (offline || failure) {
                return RemotingCommand.createResponseCommand(offline ? ResponseCode.CONSUMER_NOT_ONLINE : ResponseCode.NO_PERMISSION, null);
            }
            ConsumerConnection consumers = new ConsumerConnection();
            for (String[] entry : new String[][] {{"shared", "127.0.0.1:1000"}, {"consumer", "[::1]:2000"}}) {
                Connection connection = new Connection();
                connection.setClientId(entry[0]);
                connection.setClientAddr(entry[1]);
                connection.setLanguage(LanguageCode.JAVA);
                consumers.getConnectionSet().add(connection);
            }
            return this.ok(consumers);
        });
    }

    private RemotingCommand ok(Object body) {
        RemotingCommand response = RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null);
        response.setBody(RemotingSerializable.encode(body));
        return response;
    }

}
