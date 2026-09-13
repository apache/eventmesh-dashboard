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

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQTopicCrudUnitTest {

    private DefaultRemotingClient client;
    private RocketMQTopicRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new RocketMQTopicRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    void createTopicMapsHeaderAndDefaults() throws Exception {
        log.info("【模拟测试】创建主题：检查队列数和默认配置");
        respond(ResponseCode.SUCCESS, null);
        Assertions.assertEquals(200, logResult(service.createTopic(createRequest(4, 6))).getCode());
        RemotingCommand command = capturedRequest();
        Assertions.assertEquals(RequestCode.UPDATE_AND_CREATE_TOPIC, command.getCode());
        CreateTopicRequestHeader header = (CreateTopicRequestHeader) command.readCustomHeader();
        Assertions.assertEquals("topic-a", header.getTopic());
        Assertions.assertEquals("topic-a", header.getDefaultTopic());
        log.info("创建配置：主题={}，读队列数={}，写队列数={}，顺序消息={}",
            header.getTopic(), header.getReadQueueNums(), header.getWriteQueueNums(), header.getOrder() ? "是" : "否");
        Assertions.assertEquals(4, header.getReadQueueNums());
        Assertions.assertEquals(6, header.getWriteQueueNums());
        Assertions.assertEquals(PermName.PERM_READ | PermName.PERM_WRITE, header.getPerm());
        Assertions.assertEquals("SINGLE_TAG", header.getTopicFilterType());
        Assertions.assertEquals(0, header.getTopicSysFlag());
        Assertions.assertFalse(header.getOrder());
    }

    @Test
    void updateTopicUsesUpsertWithNewConfiguration() throws Exception {
        log.info("【模拟测试】更新主题：检查修改后的队列数和属性");
        respond(ResponseCode.SUCCESS, null);
        CreateTopic2Request request = createRequest(8, 10);
        request.getMetaData().setOrder(1);
        request.getMetaData().setTopicFilterType("MULTI_TAG");
        request.getMetaData().setTopicConfig("{\"+message.type\":\"FIFO\"}");
        log.info("本次提交的主题配置：");
        logConfig(request.getMetaData());
        Assertions.assertEquals(200, logResult(service.createTopic(request)).getCode());
        RemotingCommand command = capturedRequest();
        Assertions.assertEquals(RequestCode.UPDATE_AND_CREATE_TOPIC, command.getCode());
        CreateTopicRequestHeader header = (CreateTopicRequestHeader) command.readCustomHeader();
        Assertions.assertEquals("topic-a", header.getTopic());
        Assertions.assertEquals(8, header.getReadQueueNums());
        Assertions.assertEquals(10, header.getWriteQueueNums());
        Assertions.assertTrue(header.getOrder());
        Assertions.assertEquals("MULTI_TAG", header.getTopicFilterType());
        Assertions.assertEquals("+message.type=FIFO", header.getAttributes());
    }

    @Test
    void queryTopicsDecodesBrokerConfiguration() throws Exception {
        log.info("【模拟测试】查询主题：检查返回配置");
        TopicConfig config = new TopicConfig("topic-a", 4, 6, PermName.PERM_READ | PermName.PERM_WRITE);
        config.setOrder(true);
        config.setTopicFilterType(TopicFilterType.MULTI_TAG);
        config.setAttributes(Map.of("message.type", "FIFO"));
        TopicConfigSerializeWrapper wrapper = new TopicConfigSerializeWrapper();
        wrapper.getTopicConfigTable().put("topic-a", config);
        respond(ResponseCode.SUCCESS, wrapper.encode());
        GetTopicsResult result = logResult(service.getAllTopics(new GetTopics2Request()));
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(1, result.getData().size());
        TopicMetadata actual = result.getData().get(0);
        Assertions.assertEquals("topic-a", actual.getTopicName());
        Assertions.assertEquals(4, actual.getReadQueueNum());
        Assertions.assertEquals(6, actual.getWriteQueueNum());
        Assertions.assertEquals(1, actual.getOrder());
        Assertions.assertEquals("MULTI_TAG", actual.getTopicFilterType());
        Assertions.assertEquals("FIFO", JSON.parseObject(actual.getTopicConfig()).getString("message.type"));
        Assertions.assertEquals(RequestCode.GET_ALL_TOPIC_CONFIG, capturedRequest().getCode());
    }

    @Test
    void emptyTopicTableReturnsEmptyList() throws Exception {
        log.info("【模拟测试】查询主题：验证空列表");
        respond(ResponseCode.SUCCESS, new TopicConfigSerializeWrapper().encode());
        Assertions.assertTrue(logResult(service.getAllTopics(new GetTopics2Request())).getData().isEmpty());
    }

    @Test
    void deleteTopicUsesCorrectHeaderAndResultType() throws Exception {
        log.info("【模拟测试】删除主题：检查组装的请求和返回类型");
        respond(ResponseCode.SUCCESS, null);
        DeleteTopicResult result = logResult(service.deleteTopic(deleteRequest()));
        Assertions.assertEquals(200, result.getCode());
        RemotingCommand command = capturedRequest();
        Assertions.assertEquals(RequestCode.DELETE_TOPIC_IN_BROKER, command.getCode());
        DeleteTopicRequestHeader header = (DeleteTopicRequestHeader) command.readCustomHeader();
        log.info("删除请求：主题={}", header.getTopic());
        Assertions.assertEquals("topic-a", header.getTopic());
    }

    @Test
    void brokerErrorsPreserveCodeAndMessageForCrud() throws Exception {
        log.info("【模拟测试】主题操作：验证无权限错误");
        respond(ResponseCode.NO_PERMISSION, null);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, logResult(service.createTopic(createRequest(4, 4))).getCode());
        GetTopicsResult queried = logResult(service.getAllTopics(new GetTopics2Request()));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, queried.getCode());
        Assertions.assertEquals("denied", queried.getMessage());
        Assertions.assertNull(queried.getData());
        DeleteTopicResult deleted = logResult(service.deleteTopic(deleteRequest()));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, deleted.getCode());
        Assertions.assertEquals("denied", deleted.getMessage());
    }

    @Test
    void timeoutPropagatesForAllOperations() throws Exception {
        log.info("【模拟测试】主题操作：验证请求超时");
        RemotingTimeoutException timeout = new RemotingTimeoutException("broker", 3000);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenThrow(timeout);
        Assertions.assertSame(timeout, logExpectedException(Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.createTopic(createRequest(4, 4))))));
        Assertions.assertSame(timeout, logExpectedException(Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.getAllTopics(new GetTopics2Request())))));
        Assertions.assertSame(timeout, logExpectedException(Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.deleteTopic(deleteRequest())))));
    }

    @Test
    void invalidFilterTypeFailsBeforeRpc() {
        log.info("【模拟测试】主题操作：验证非法过滤类型");
        CreateTopic2Request request = createRequest(4, 4);
        request.getMetaData().setTopicFilterType("invalid");
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.createTopic(request))));
        Mockito.verifyNoInteractions(client);
    }

    private CreateTopic2Request createRequest(int readQueues, int writeQueues) {
        TopicMetadata metadata = new TopicMetadata();
        metadata.setTopicName("topic-a");
        metadata.setReadQueueNum(readQueues);
        metadata.setWriteQueueNum(writeQueues);
        CreateTopic2Request request = new CreateTopic2Request();
        request.setMetaData(metadata);
        return request;
    }

    private DeleteTopicRequest deleteRequest() {
        DeleteTopicRequest request = new DeleteTopicRequest();
        request.setMetaData(createRequest(4, 4).getMetaData());
        return request;
    }

    private void respond(int code, byte[] body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, code == ResponseCode.SUCCESS ? null : "denied");
        response.setBody(body);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenReturn(response);
    }

    private RemotingCommand capturedRequest() throws Exception {
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        return captor.getValue();
    }

    private <T extends GlobalResult<?>> T logResult(T result) {
        log.info("操作结果：{}，返回码={}", Integer.valueOf(200).equals(result.getCode()) ? "成功" : "失败", result.getCode());
        if (!Integer.valueOf(200).equals(result.getCode())) {
            log.info("失败原因：{}", "denied".equals(result.getMessage()) ? "没有操作权限" : result.getMessage());
        }
        if (result.getData() instanceof java.util.List<?> values) {
            log.info("查询结果：共 {} 条", values.size());
            values.forEach(value -> logConfig((TopicMetadata) value));
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

    private void logConfig(TopicMetadata topic) {
        log.info("主题={}，读队列数={}，写队列数={}，顺序消息={}，过滤类型={}，主题属性={}",
            topic.getTopicName(), topic.getReadQueueNum(), topic.getWriteQueueNum(),
            Integer.valueOf(1).equals(topic.getOrder()) ? "是" : "否", topic.getTopicFilterType(), topic.getTopicConfig());
    }
}
