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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.group.CreateGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.OffsetRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageResponseHeader;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import lombok.extern.slf4j.Slf4j;

/** Explicit initialization followed by operations on the same retained single-queue Topic and group. */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RocketMQOffsetIntegrationTest {

    // Reuse the queue retained by the original initializeQueue run, including across separate IDEA runs.
    private static final String DEFAULT_NAME = "dashboard_offset_5775fe9940564d5baf2ecb89cedb53b6";

    private String name;
    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private OffsetRemotingService offsets;
    private TopicRemotingService topics;
    private GroupRemotingService groups;

    @BeforeEach
    void setUp() throws Exception {
        name = System.getProperty("rocketmq.offset.name", DEFAULT_NAME);
        runtime = new RuntimeMetadata();
        runtime.setId(96001L);
        runtime.setClusterId(96000L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        AbstractSimpleCreateSDKConfig config =
            ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.broker.port", 20911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        offsets = Remoting2Manage.getInstance().createRemotingService(OffsetRemotingService.class, runtime);
        topics = Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, runtime);
        groups = Remoting2Manage.getInstance().createRemotingService(GroupRemotingService.class, runtime);
        log.info("本次操作目标：主题={}，消费组={}，队列=0", name, name);
    }

    @AfterEach
    void tearDown() {
        try {
            if (client != null) {
                client.shutdown();
            }
        } finally {
            if (runtime != null) {
                SDKManage.getInstance().deleteClient(null, runtime.getUnique());
            }
        }
    }

    @Test
    @Order(1)
    void initializeQueue() throws Exception {
        log.info("【真实 Broker 测试】初始化共享队列；已有完整数据时直接复用");
        GetTopicsResult topicResult = topics.getAllTopics(new GetTopics2Request());
        Assertions.assertEquals(200, topicResult.getCode(), topicResult.getMessage());
        boolean topicExists = topicResult.getData().stream().anyMatch(value -> name.equals(value.getTopicName()));
        if (topicExists) {
            TopicMetadata existing = topicResult.getData().stream().filter(value -> name.equals(value.getTopicName())).findFirst().orElseThrow();
            Assertions.assertEquals(1, existing.getReadQueueNum(), "共享 Topic 必须只有一个读队列");
            Assertions.assertEquals(1, existing.getWriteQueueNum(), "共享 Topic 必须只有一个写队列");
        }
        GetGroupResult groupResult = groups.getAllGroups(new GetGroupsRequest());
        Assertions.assertEquals(200, groupResult.getCode(), groupResult.getMessage());
        if (groupResult.getData().stream().noneMatch(value -> name.equals(value.getName()))) {
            GroupMetadata group = new GroupMetadata();
            group.setName(name);
            CreateGroupRequest create = new CreateGroupRequest();
            create.setMetaData(group);
            Assertions.assertEquals(200, groups.createGroup(create).getCode());
        }
        if (!topicExists) {
            TopicMetadata topic = new TopicMetadata();
            topic.setTopicName(name);
            topic.setReadQueueNum(1);
            topic.setWriteQueueNum(1);
            CreateTopic2Request create = new CreateTopic2Request();
            create.setMetaData(topic);
            Assertions.assertEquals(200, topics.createTopic(create).getCode());
            this.sendMessages(client, name, 0, 100);
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            GetOffsetResult ready;
            do {
                ready = offsets.getOffset(this.queryRequest());
                Assertions.assertEquals(200, ready.getCode(), ready.getMessage());
                if (ready.getData().size() == 1 && ready.getData().get(0).getBrokerOffset() == 100L) {
                    break;
                }
                Thread.sleep(50L);
            } while (System.nanoTime() < deadline);
            this.assertSharedQueue(ready);
            this.resetAndVerify(this.resetRequest(ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET), 0L);
        }
        this.assertSharedQueue(offsets.getOffset(this.queryRequest()));
        log.info("共享数据已就绪并保留：主题={}，消费组={}，队列=0，消息位点=0～99；已有 Topic 不重复写入或重置", name, name);
    }

    @Test
    @Order(6)
    void queryOffset() throws Exception {
        log.info("【真实 Broker 测试】查询共享队列当前位点");
        this.assertSharedQueue(offsets.getOffset(this.queryRequest()));
    }

    @Test
    @Order(2)
    void resetToFirstOffset() throws Exception {
        log.info("【真实 Broker 测试】共享队列重置到最早位点 0");
        this.resetAndVerify(this.resetRequest(ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET), 0L);
    }

    @Test
    @Order(3)
    void resetToLastOffset() throws Exception {
        log.info("【真实 Broker 测试】共享队列重置到最新位点 100");
        this.resetAndVerify(this.resetRequest(ResetOffsetMode.CONSUME_FROM_LAST_OFFSET), 100L);
    }

    @Test
    @Order(4)
    void resetByTimestamp() throws Exception {
        log.info("【真实 Broker 测试】共享队列按时间戳重置");
        ResetOffsetRequest request = this.resetRequest(ResetOffsetMode.CONSUME_FROM_TIMESTAMP);
        request.setTimestamp(Long.getLong("rocketmq.offset.timestamp", System.currentTimeMillis()));
        this.resetAndVerify(request, null);
    }

    @Test
    @Order(5)
    void resetToDesignatedOffset() throws Exception {
        log.info("【真实 Broker 测试】共享队列 0 重置到指定逻辑位点 100");
        ResetOffsetRequest request = this.resetRequest(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET);
        request.setPartitionId(0);
        request.setOffset(50L);
        this.resetAndVerify(request, 50L);
    }

    private GetOffsetRequest queryRequest() {
        GetOffsetRequest request = new GetOffsetRequest();
        request.setGroupName(name);
        request.setTopic(name);
        return request;
    }

    private ResetOffsetRequest resetRequest(ResetOffsetMode mode) {
        ResetOffsetRequest request = new ResetOffsetRequest();
        request.setGroupName(name);
        request.setTopic(name);
        request.setResetOffsetMode(mode);
        return request;
    }

    private void assertSharedQueue(GetOffsetResult result) {
        Assertions.assertEquals(200, result.getCode(), "请先运行 initializeQueue：" + result.getMessage());
        Assertions.assertEquals(1, result.getData().size(), "请先运行 initializeQueue，且只使用共享的单队列 Topic");
        Assertions.assertEquals(name, result.getData().get(0).getTopic());
        Assertions.assertEquals(0, result.getData().get(0).getPartitionId());
        Assertions.assertEquals(100L, result.getData().get(0).getBrokerOffset(), "初始化数据应为 100 条；不会自动追加或重建");
        log.info("查询结果：主题={}，队列=0，消费位点={}，Broker 位点={}",
            name, result.getData().get(0).getOffset(), result.getData().get(0).getBrokerOffset());
    }

    private void resetAndVerify(ResetOffsetRequest request, Long expectedOffset) throws Exception {
        this.assertSharedQueue(offsets.getOffset(this.queryRequest()));
        log.info("重置参数：模式={}，队列={}，位点={}，时间戳={}",
            request.getResetOffsetMode(), request.getPartitionId(), request.getOffset(), request.getTimestamp());
        ResetOffsetResult result = offsets.resetOffset(request);
        Assertions.assertEquals(200, result.getCode(), result.getMessage());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals(name, result.getData().get(0).getTopic());
        Assertions.assertEquals(0, result.getData().get(0).getPartitionId());
        Long actualOffset = result.getData().get(0).getOffset();
        Assertions.assertNotNull(actualOffset);
        Assertions.assertTrue(actualOffset >= 0L && actualOffset <= 100L);
        if (expectedOffset != null) {
            Assertions.assertEquals(expectedOffset, actualOffset);
        }
        GetOffsetResult after = offsets.getOffset(this.queryRequest());
        this.assertSharedQueue(after);
        Assertions.assertEquals(actualOffset, after.getData().get(0).getOffset());
        log.info("重置并查询验证通过：队列=0，消费位点={}；Topic、消费组和消息均保留", actualOffset);
    }

    private void sendMessages(DefaultRemotingClient client, String topic, int queueId, int count) throws Exception {
        for (int index = 0; index < count; index++) {
            SendMessageRequestHeader header = new SendMessageRequestHeader();
            header.setProducerGroup(topic + "_producer");
            header.setTopic(topic);
            header.setDefaultTopic(topic);
            header.setDefaultTopicQueueNums(1);
            header.setQueueId(queueId);
            header.setSysFlag(0);
            header.setBornTimestamp(System.currentTimeMillis());
            header.setFlag(0);
            header.setReconsumeTimes(0);
            header.setUnitMode(false);
            header.setBatch(false);
            RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, header);
            command.setBody(("offset-test-" + queueId + "-" + index).getBytes(StandardCharsets.UTF_8));
            RemotingCommand response = client.invokeSync(command, 3000);
            Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), response.getRemark());
            SendMessageResponseHeader sent = (SendMessageResponseHeader) response.decodeCommandCustomHeader(SendMessageResponseHeader.class);
            Assertions.assertEquals(queueId, sent.getQueueId());
            Assertions.assertEquals((long) index, sent.getQueueOffset());
        }
        log.info("消息写入完成：主题={}，队列={}，消息数={}，逻辑位点范围=0～{}", topic, queueId, count, count - 1);
    }

}
