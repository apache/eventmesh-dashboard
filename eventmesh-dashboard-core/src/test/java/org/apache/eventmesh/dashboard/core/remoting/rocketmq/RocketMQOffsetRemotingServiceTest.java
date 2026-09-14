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

import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.body.ResetOffsetBody;
import org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ResetOffsetRequestHeader;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQOffsetRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQOffsetRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new RocketMQOffsetRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    void queryMapsQueueOffsetsAndFilter() throws Exception {
        log.info("【模拟测试】查询位点：验证消费组、Topic 过滤和队列位点");
        ConsumeStats stats = new ConsumeStats();
        OffsetWrapper value = new OffsetWrapper();
        value.setConsumerOffset(12L);
        value.setBrokerOffset(30L);
        value.setLastTimestamp(123L);
        stats.getOffsetTable().put(new MessageQueue("topic", "broker", 2), value);
        respond(response(ResponseCode.SUCCESS, stats.encode()));
        GetOffsetResult result = logResult(service.getOffset(query()));
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(12L, result.getData().get(0).getOffset());
        Assertions.assertEquals(30L, result.getData().get(0).getBrokerOffset());
        Assertions.assertEquals(123L, result.getData().get(0).getLastTimestamp());
        Assertions.assertEquals(2, result.getData().get(0).getPartitionId());
        ArgumentCaptor<RemotingCommand> capture = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(capture.capture(), ArgumentMatchers.eq(3000L));
        Assertions.assertEquals(RequestCode.GET_CONSUME_STATS, capture.getValue().getCode());
        GetConsumeStatsRequestHeader header = (GetConsumeStatsRequestHeader) capture.getValue().readCustomHeader();
        Assertions.assertEquals("group", header.getConsumerGroup());
        Assertions.assertEquals("topic", header.getTopic());
    }

    @Test
    void emptyQueryIsSuccessful() throws Exception {
        log.info("【模拟测试】查询空位点表：成功返回空列表");
        respond(response(ResponseCode.SUCCESS, new ConsumeStats().encode()));
        Assertions.assertTrue(logResult(service.getOffset(query())).getData().isEmpty());
    }

    @Test
    void errorsPreserveBrokerCode() throws Exception {
        log.info("【模拟测试】查询失败：保留 Broker 错误码和原因");
        respond(response(ResponseCode.NO_PERMISSION, null));
        GetOffsetResult result = logResult(service.getOffset(query()));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertEquals("denied", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void malformedQueryFails() throws Exception {
        log.info("【模拟测试】查询响应缺失或无效：明确抛出异常");
        respond(response(ResponseCode.SUCCESS, null));
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalStateException.class, () -> logResult(service.getOffset(query()))).getMessage());
        respond(response(ResponseCode.SUCCESS, "null".getBytes(StandardCharsets.UTF_8)));
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalStateException.class, () -> logResult(service.getOffset(query()))).getMessage());
    }

    @Test
    void resetModesMapWithoutLosingQueueScope() throws Exception {
        log.info("【模拟测试】重置位点：验证四种模式和队列范围");
        for (ResetOffsetMode mode : ResetOffsetMode.values()) {
            Mockito.reset(client);
            ResetOffsetRequest request = reset(mode);
            ResetOffsetBody body = new ResetOffsetBody();
            body.getOffsetTable().put(new MessageQueue("topic", "broker", 2), 7L);
            respond(config(true), response(ResponseCode.SUCCESS, body.encode()));
            Assertions.assertEquals(7L, logResult(service.resetOffset(request)).getData().get(0).getOffset());
            ArgumentCaptor<RemotingCommand> capture = ArgumentCaptor.forClass(RemotingCommand.class);
            Mockito.verify(client, Mockito.times(2)).invokeSync(capture.capture(), ArgumentMatchers.eq(3000L));
            RemotingCommand command = capture.getAllValues().get(1);
            Assertions.assertEquals(RequestCode.INVOKE_BROKER_TO_RESET_OFFSET, command.getCode());
            ResetOffsetRequestHeader header = (ResetOffsetRequestHeader) command.readCustomHeader();
            Assertions.assertEquals("group", header.getGroup());
            Assertions.assertEquals("topic", header.getTopic());
            Assertions.assertTrue(header.isForce());
            Assertions.assertEquals(mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET ? 2 : -1, header.getQueueId());
            Assertions.assertEquals(mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET ? 7L : -1L, header.getOffset());
            long timestamp = mode == ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET ? 0L
                : mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP ? 123L : -1L;
            Assertions.assertEquals(timestamp, header.getTimestamp());
            log.info("请求校验通过：模式={}，队列={}（-1 表示全部），位点={}，时间戳={}，强制重置={}",
                modeName(mode), header.getQueueId(), header.getOffset(), header.getTimestamp(), header.isForce());
        }
    }

    @Test
    void legacyBrokerNeverReceivesReset() throws Exception {
        log.info("【模拟测试】旧式 Broker：拦截不安全的重置路径");
        respond(config(false));
        log.info("捕获预期异常：{}", Assertions.assertThrows(UnsupportedOperationException.class,
            () -> logResult(service.resetOffset(reset(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET)))).getMessage());
        Mockito.verify(client, Mockito.times(1)).invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L));
        log.info("校验通过：仅查询 Broker 配置，未发送重置请求");
    }

    @Test
    void failedPreflightNeverResets() throws Exception {
        log.info("【模拟测试】配置查询失败：不发送重置请求");
        respond(response(ResponseCode.NO_PERMISSION, null));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION,
            logResult(service.resetOffset(reset(ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET))).getCode());
        Mockito.verify(client, Mockito.times(1)).invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L));
        log.info("校验通过：仅查询 Broker 配置，未发送重置请求");
    }

    @Test
    void resetFailureAndMissingBodyAreExplicit() throws Exception {
        log.info("【模拟测试】重置失败或响应缺失：不误报成功");
        respond(config(true), response(ResponseCode.SYSTEM_ERROR, null));
        Assertions.assertEquals(10000 + ResponseCode.SYSTEM_ERROR,
            logResult(service.resetOffset(reset(ResetOffsetMode.CONSUME_FROM_LAST_OFFSET))).getCode());
        respond(config(true), response(ResponseCode.SUCCESS, null));
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalStateException.class,
            () -> logResult(service.resetOffset(reset(ResetOffsetMode.CONSUME_FROM_LAST_OFFSET)))).getMessage());
    }

    @Test
    void invalidInputDoesNotReachBroker() {
        log.info("【模拟测试】参数无效：在请求 Broker 前拒绝");
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.getOffset(new GetOffsetRequest()))).getMessage());
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalArgumentException.class,
            () -> logResult(service.resetOffset(new ResetOffsetRequest()))).getMessage());
        ResetOffsetRequest request = reset(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET);
        request.setOffset(-1L);
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.resetOffset(request))).getMessage());
        request.setResetOffsetMode(ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET);
        log.info("捕获预期异常：{}", Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.resetOffset(request))).getMessage());
        Mockito.verifyNoInteractions(client);
        log.info("校验通过：未向 Broker 发送请求");
    }

    @Test
    void transportFailurePropagates() throws Exception {
        log.info("【模拟测试】网络超时：向调用方传递异常");
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("broker", 3000L));
        log.info("捕获预期异常：{}", Assertions.assertThrows(RemotingTimeoutException.class, () -> logResult(service.getOffset(query()))).getMessage());
        log.info("捕获预期异常：{}", Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.resetOffset(reset(ResetOffsetMode.CONSUME_FROM_LAST_OFFSET)))).getMessage());
    }

    private <T extends GlobalResult<?>> T logResult(T result) {
        log.info("操作结果：{}，返回码={}", Integer.valueOf(200).equals(result.getCode()) ? "成功" : "失败", result.getCode());
        if (!Integer.valueOf(200).equals(result.getCode())) {
            log.info("失败原因：{}", result.getMessage());
        }
        if (result.getData() instanceof java.util.List<?> values) {
            log.info("返回队列数：{}", values.size());
            for (Object value : values) {
                if (value instanceof GetOffsetResponse offset) {
                    log.info("主题={}，Broker={}，队列={}，消费位点={}，Broker 位点={}，最后时间戳={}",
                        offset.getTopic(), offset.getBrokerName(), offset.getPartitionId(), offset.getOffset(),
                        offset.getBrokerOffset(), offset.getLastTimestamp());
                } else if (value instanceof ResetOffsetResponse offset) {
                    log.info("重置结果：主题={}，Broker={}，队列={}，位点={}",
                        offset.getTopic(), offset.getBrokerName(), offset.getPartitionId(), offset.getOffset());
                }
            }
        }
        return result;
    }

    private String modeName(ResetOffsetMode mode) {
        switch (mode) {
            case CONSUME_FROM_FIRST_OFFSET:
                return "最早位点";
            case CONSUME_FROM_LAST_OFFSET:
                return "最新位点";
            case CONSUME_FROM_TIMESTAMP:
                return "指定时间戳";
            case CONSUME_FROM_DESIGNATED_OFFSET:
                return "指定队列位点";
            default:
                return mode.name();
        }
    }

    private GetOffsetRequest query() {
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName("group");
        request.setTopic("topic");
        log.info("查询参数：消费组={}，主题={}", request.getGroupName(), request.getTopic());
        return request;
    }

    private ResetOffsetRequest reset(ResetOffsetMode mode) {
        ResetOffsetRequest request = new ResetOffsetRequest();
        request.setGroupName("group");
        request.setTopic("topic");
        request.setResetOffsetMode(mode);
        if (mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET) {
            request.setPartitionId(2);
            request.setOffset(7L);
        }
        if (mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP) {
            request.setTimestamp(123L);
        }
        log.info("重置参数：消费组={}，主题={}，模式={}，队列={}，位点={}，时间戳={}",
            request.getGroupName(), request.getTopic(), modeName(mode), request.getPartitionId(), request.getOffset(), request.getTimestamp());
        return request;
    }

    private RemotingCommand config(boolean enabled) {
        return response(ResponseCode.SUCCESS, ("useServerSideResetOffset=" + enabled).getBytes(StandardCharsets.UTF_8));
    }

    private RemotingCommand response(int code, byte[] body) {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "denied");
        response.setBody(body);
        return response;
    }

    private void respond(RemotingCommand first, RemotingCommand... next) throws Exception {
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(first, next);
    }
}
