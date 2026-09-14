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

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMemberMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQGroupMemberRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQGroupMemberRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new RocketMQGroupMemberRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    void queryMapsSubscriptionAndUsesGroupHeader() throws Exception {
        log.info("【模拟测试】查询订阅：检查组名和过滤条件");
        respond(connection());
        GetSubscriptionResult result = logResult(service.getSubscription(request("orders", "topic-a")));
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(1, result.getData().size());
        GroupMemberMetadata metadata = result.getData().get(0);
        Assertions.assertEquals("orders", metadata.getGroupName());
        Assertions.assertEquals("topic-a", metadata.getTopicName());
        Assertions.assertNull(metadata.getGroupId());
        Assertions.assertNull(metadata.getTopicId());
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        Assertions.assertEquals(RequestCode.GET_CONSUMER_CONNECTION_LIST, captor.getValue().getCode());
        GetConsumerConnectionListRequestHeader header = (GetConsumerConnectionListRequestHeader) captor.getValue().readCustomHeader();
        Assertions.assertEquals("orders", header.getConsumerGroup());
    }

    @Test
    void allGroupsSkipsOfflineAndFiltersTopic() throws Exception {
        log.info("【模拟测试】查询订阅：跳过离线组并筛选主题");
        respond(groups(), response(ResponseCode.CONSUMER_NOT_ONLINE, null), connection());
        GetSubscriptionResult result = logResult(service.getSubscription(request(null, "topic-a")));
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals("b-online", result.getData().get(0).getGroupName());
    }

    @Test
    void nullRequestReturnsAllSubscriptions() throws Exception {
        log.info("【模拟测试】查询订阅：不传条件时返回全部关系");
        respond(groups(), response(ResponseCode.CONSUMER_NOT_ONLINE, null), connection());
        GetSubscriptionResult result = logResult(service.getSubscription(null));
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals("topic-a", result.getData().get(0).getTopicName());
        Assertions.assertEquals("topic-b", result.getData().get(1).getTopicName());
        Assertions.assertNotEquals(result.getData().get(0).nodeUnique(), result.getData().get(1).nodeUnique());
    }

    @Test
    void offlineAndEmptySubscriptionsReturnEmptyLists() throws Exception {
        log.info("【模拟测试】查询订阅：验证离线组和空订阅");
        respond(response(ResponseCode.CONSUMER_NOT_ONLINE, null), response(ResponseCode.SUCCESS, new ConsumerConnection().encode()));
        Assertions.assertTrue(logResult(service.getSubscription(request("offline", null))).getData().isEmpty());
        Assertions.assertTrue(logResult(service.getSubscription(request("online", null))).getData().isEmpty());
    }

    @Test
    void noConfiguredGroupsReturnsEmptyList() throws Exception {
        log.info("【模拟测试】查询订阅：验证没有消费组");
        respond(response(ResponseCode.SUCCESS, new SubscriptionGroupWrapper().encode()));
        Assertions.assertTrue(logResult(service.getSubscription(new GetSubscriptionRequest())).getData().isEmpty());
        Mockito.verify(client).invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L));
    }

    @Test
    void failuresPreserveCodeAndDoNotReturnPartialData() throws Exception {
        log.info("【模拟测试】查询订阅：失败时不返回部分数据");
        RemotingCommand denied = response(ResponseCode.NO_PERMISSION, null);
        denied.setRemark("denied");
        respond(denied, groups(), connection(), denied);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, logResult(service.getSubscription(null)).getCode());
        GetSubscriptionResult result = logResult(service.getSubscription(null));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertEquals("denied", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void missingOrMalformedBodiesFailExplicitly() throws Exception {
        log.info("【模拟测试】查询订阅：验证响应缺失和格式错误");
        respond(response(ResponseCode.SUCCESS, null), response(ResponseCode.SUCCESS, "{".getBytes(StandardCharsets.UTF_8)),
            response(ResponseCode.SUCCESS, null));
        logExpectedException(Assertions.assertThrows(IllegalStateException.class, () -> logResult(service.getSubscription(request("orders", null)))));
        logExpectedException(Assertions.assertThrows(RuntimeException.class, () -> logResult(service.getSubscription(request("orders", null)))));
        logExpectedException(Assertions.assertThrows(IllegalStateException.class, () -> logResult(service.getSubscription(null))));
    }

    @Test
    void blankFiltersAreRejectedBeforeRpc() {
        log.info("【模拟测试】查询订阅：验证空白筛选条件");
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.getSubscription(request(" ", null)))));
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.getSubscription(request(null, " ")))));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void transportExceptionsPropagate() throws Exception {
        log.info("【模拟测试】查询订阅：验证超时和中断");
        RemotingTimeoutException timeout = new RemotingTimeoutException("broker", 3000);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenThrow(timeout);
        Assertions.assertSame(timeout, logExpectedException(Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.getSubscription(request("orders", null))))));
        InterruptedException interrupted = new InterruptedException("interrupted");
        Mockito.reset(client);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenThrow(interrupted);
        Assertions.assertSame(interrupted, logExpectedException(Assertions.assertThrows(InterruptedException.class,
            () -> logResult(service.getSubscription(request("orders", null))))));
    }

    private GetSubscriptionRequest request(String group, String topic) {
        GroupMemberMetadata metadata = new GroupMemberMetadata();
        metadata.setGroupName(group);
        metadata.setTopicName(topic);
        GetSubscriptionRequest request = new GetSubscriptionRequest();
        request.setMetaData(metadata);
        log.info("订阅筛选条件：消费组={}，主题={}", group == null ? "全部" : group, topic == null ? "全部" : topic);
        return request;
    }

    private RemotingCommand groups() {
        SubscriptionGroupWrapper wrapper = new SubscriptionGroupWrapper();
        wrapper.getSubscriptionGroupTable().put("a-offline", new SubscriptionGroupConfig());
        wrapper.getSubscriptionGroupTable().put("b-online", new SubscriptionGroupConfig());
        return response(ResponseCode.SUCCESS, wrapper.encode());
    }

    private RemotingCommand connection() {
        ConsumerConnection connection = new ConsumerConnection();
        SubscriptionData subscription = new SubscriptionData("topic-a", "tag-a || tag-b");
        subscription.setExpressionType("TAG");
        connection.getSubscriptionTable().put("topic-a", subscription);
        connection.getSubscriptionTable().put("topic-b", new SubscriptionData("topic-b", "*"));
        return response(ResponseCode.SUCCESS, connection.encode());
    }

    private RemotingCommand response(int code, byte[] body) {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, null);
        response.setBody(body);
        return response;
    }

    private void respond(RemotingCommand first, RemotingCommand... next) throws Exception {
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenReturn(first, next);
    }

    private <T extends GlobalResult<?>> T logResult(T result) {
        log.info("操作结果：{}，返回码={}", Integer.valueOf(200).equals(result.getCode()) ? "成功" : "失败", result.getCode());
        if (!Integer.valueOf(200).equals(result.getCode())) {
            log.info("失败原因：{}", "denied".equals(result.getMessage()) ? "没有操作权限" : result.getMessage());
        }
        if (result.getData() instanceof java.util.List<?> values) {
            log.info("查询结果：共 {} 条", values.size());
            values.forEach(value -> logConfig((GroupMemberMetadata) value));
        }
        return result;
    }

    private <T extends Throwable> T logExpectedException(T exception) {
        String reason = switch (exception.getClass().getSimpleName()) {
            case "RemotingTimeoutException" -> "请求超时";
            case "InterruptedException" -> "请求被中断";
            case "IllegalArgumentException" -> "请求参数不合法";
            default -> "响应内容缺失或格式不合法";
        };
        log.info("异常处理验证通过：{}", reason);
        return exception;
    }

    private void logConfig(GroupMemberMetadata subscription) {
        log.info("消费组={}，主题={}", subscription.getGroupName(), subscription.getTopicName());
    }
}
