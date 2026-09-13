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
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import lombok.extern.slf4j.Slf4j;

/** Real Broker CRUD tests. Kept under the existing class name for IDEA run configurations. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class RocketMQTopicCrudUnitTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private TopicRemotingService service;
    private String topicName;

    @BeforeEach
    void setUp() {
        runtime = new RuntimeMetadata();
        runtime.setId(96001L);
        runtime.setClusterId(96000L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        runtime.setHost(System.getProperty("rocketmq.broker.host", "127.0.0.1"));
        runtime.setPort(Integer.getInteger("rocketmq.broker.port", 20911));
        topicName = System.getProperty("rocketmq.topic.name", "dashboard_topic_test_a");
        Assertions.assertFalse(topicName.isBlank(), "测试主题名不能为空");
        AbstractSimpleCreateSDKConfig config =
            ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(runtime.getHost());
        address.setPort(runtime.getPort());
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        RocketMQTopicRemotingService implementation =
            Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, runtime);
        Assertions.assertSame(client, implementation.getClient());
        service = implementation;
        log.info("连接真实 Broker：{}:{}，测试主题={}", runtime.getHost(), runtime.getPort(), topicName);
    }

    @Test
    @Order(1)
    void createTopic() throws Exception {
        log.info("【真实创建】创建主题并查询验证");
        TopicMetadata metadata = new TopicMetadata();
        metadata.setTopicName(topicName);
        metadata.setReadQueueNum(4);
        metadata.setWriteQueueNum(4);
        metadata.setOrder(0);
        metadata.setTopicFilterType("SINGLE_TAG");
        logConfig("创建参数", metadata);
        CreateTopic2Request request = new CreateTopic2Request();
        request.setMetaData(metadata);
        assertSuccess("创建", service.createTopic(request));
        TopicMetadata actual = queryTargetTopic();
        logConfig("创建后查询", actual);
        Assertions.assertEquals(4, actual.getReadQueueNum());
        Assertions.assertEquals(4, actual.getWriteQueueNum());
        Assertions.assertEquals(0, actual.getOrder());
        Assertions.assertEquals("SINGLE_TAG", actual.getTopicFilterType());
        log.info("创建验证通过：主题已保留，可在 MQ 控制台查看 {}", topicName);
    }

    @Test
    @Order(2)
    void updateTopic() throws Exception {
        log.info("【真实更新】修改已有主题的读写队列数");
        TopicMetadata previous = queryTargetTopic();
        logConfig("更新前", previous);
        TopicMetadata metadata = new TopicMetadata();
        metadata.setTopicName(topicName);
        metadata.setReadQueueNum(8);
        metadata.setWriteQueueNum(8);
        // Topic upsert requires a full configuration; pass through the existing flags explicitly.
        metadata.setOrder(previous.getOrder());
        metadata.setTopicFilterType(previous.getTopicFilterType());
        logConfig("本次修改参数", metadata);
        CreateTopic2Request request = new CreateTopic2Request();
        request.setMetaData(metadata);
        assertSuccess("更新", service.createTopic(request));
        TopicMetadata actual = queryTargetTopic();
        logConfig("更新后查询", actual);
        log.info("修改对比：读队列数 {} → {}，写队列数 {} → {}",
            previous.getReadQueueNum(), actual.getReadQueueNum(), previous.getWriteQueueNum(), actual.getWriteQueueNum());
        Assertions.assertEquals(8, actual.getReadQueueNum());
        Assertions.assertEquals(8, actual.getWriteQueueNum());
        Assertions.assertEquals(previous.getOrder(), actual.getOrder());
        Assertions.assertEquals(previous.getTopicFilterType(), actual.getTopicFilterType());
        log.info("更新验证通过：Broker 已保存新配置");
    }

    @Test
    @Order(3)
    void queryTopic() throws Exception {
        log.info("【真实查询】读取 Broker 当前主题列表，不创建或修改主题");
        long started = System.nanoTime();
        GetTopicsResult result = service.getAllTopics(new GetTopics2Request());
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        assertSuccess("查询", result);
        Assertions.assertNotNull(result.getData(), "查询成功时应返回主题列表");
        log.info("查询结果：共 {} 个主题，耗时 {} 毫秒", result.getData().size(), elapsedMillis);
        result.getData().stream().sorted(Comparator.comparing(TopicMetadata::getTopicName))
            .forEach(topic -> logConfig("主题配置", topic));
        boolean exists = result.getData().stream().anyMatch(topic -> topicName.equals(topic.getTopicName()));
        log.info("目标主题={}，是否存在={}", topicName, exists ? "是" : "否");
    }

    @Test
    @Order(4)
    void deleteTopic() throws Exception {
        log.info("【真实删除】删除当前 Broker 的主题配置：{}", topicName);
        TopicMetadata metadata = new TopicMetadata();
        metadata.setTopicName(topicName);
        DeleteTopicRequest request = new DeleteTopicRequest();
        request.setMetaData(metadata);
        assertSuccess("删除", service.deleteTopic(request));
        GetTopicsResult result = service.getAllTopics(new GetTopics2Request());
        assertSuccess("删除后查询", result);
        boolean exists = result.getData().stream().anyMatch(topic -> topicName.equals(topic.getTopicName()));
        log.info("删除后检查：主题={}，是否仍存在={}", topicName, exists ? "是" : "否");
        Assertions.assertFalse(exists, "删除后主题不应继续出现在 Broker 配置中");
    }

    private TopicMetadata queryTargetTopic() throws Exception {
        GetTopicsResult result = service.getAllTopics(new GetTopics2Request());
        assertSuccess("读取目标主题配置", result);
        return result.getData().stream().filter(topic -> topicName.equals(topic.getTopicName())).findFirst()
            .orElseThrow(() -> new AssertionError("主题 " + topicName + " 不存在，请先运行 createTopic"));
    }

    private void assertSuccess(String operation, GlobalResult<?> result) {
        log.info("{}结果：{}，返回码={}", operation, Integer.valueOf(200).equals(result.getCode()) ? "成功" : "失败", result.getCode());
        if (!Integer.valueOf(200).equals(result.getCode())) {
            log.error("失败原因：{}", result.getMessage());
        }
        Assertions.assertEquals(200, result.getCode(), result.getMessage());
    }

    private void logConfig(String stage, TopicMetadata topic) {
        log.info("{}：主题={}，读队列数={}，写队列数={}，顺序消息={}，过滤类型={}，主题属性={}",
            stage, topic.getTopicName(), topic.getReadQueueNum(), topic.getWriteQueueNum(),
            Integer.valueOf(1).equals(topic.getOrder()) ? "是" : "否", topic.getTopicFilterType(), topic.getTopicConfig());
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
}
