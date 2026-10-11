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

package org.apache.eventmesh.dashboard.core.remoting.kafka.live.zk;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTestLog;
import org.apache.eventmesh.dashboard.service.remoting.kafka.TopicRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import lombok.extern.slf4j.Slf4j;

/**
 * ZooKeeper 环境：直接连接 127.0.0.1:29092，可在 IDEA 中单独运行每个测试方法，无需 VM options。
 * 切换测试环境时修改本类 BROKER_HOST / BROKER_PORT；每个方法独立准备并清理测试数据。
 */
@Slf4j
@Tag("zookeeper")
@DisplayName("ZooKeeper 环境：Topic 创建、更新与删除")
@ExtendWith(KafkaTestLog.class)
class KafkaTopicMutationZkIntegrationTest {

    private static final String BROKER_HOST = "127.0.0.1";
    private static final int BROKER_PORT = 29092;

    private ClusterMetadata cluster;
    private AdminClient client;
    private AdminClient ping;
    private TopicRemotingService service;
    private String topicName;

    @BeforeEach
    void setUp() {
        cluster = new ClusterMetadata();
        cluster.setId(System.nanoTime());
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        AbstractMultiCreateSDKConfig config = ConfigManage.getInstance().getMultiCreateSdkConfig(cluster.getClusterType(), SDKTypeEnum.ADMIN);
        config.setKey(cluster.getId().toString());
        config.addNetAddress(NetAddress.create(BROKER_HOST, BROKER_PORT));
        SDKManage manager = SDKManage.getInstance();
        manager.createClient(SDKTypeEnum.ADMIN, cluster, config, cluster.getClusterType());
        client = manager.getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
        ping = manager.getClient(SDKTypeEnum.PING, cluster.getUnique());
        service = Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, cluster);
        topicName = "dashboard-kafka-crud-" + UUID.randomUUID();
    }

    /** 创建 Topic：1 分区、1 副本并回读配置。 */
    @Test
    @DisplayName("真实 Kafka：创建 Topic：1 分区、1 副本并回读配置")
    void createTopic() throws Exception {
        Assertions.assertEquals(200, service.createTopic(request(1, 1, Map.of("retention.ms", "60000", "segment.ms", "300000"))).getCode());
        assertState(1, "60000", "300000");
        Assertions.assertEquals(1, description().partitions().get(0).replicas().size());
    }

    /** 重复创建 Topic：返回已存在错误。 */
    @Test
    @DisplayName("真实 Kafka：重复创建 Topic：返回已存在错误")
    void createExistingTopicFails() throws Exception {
        prepareTopic();
        Assertions.assertInstanceOf(TopicExistsException.class, Assertions.assertThrows(ExecutionException.class,
            () -> service.createTopic(request(1, 1, null))).getCause());
    }

    /** 更新 Topic：分区从 1 扩到 3 并增量修改配置。 */
    @Test
    @DisplayName("真实 Kafka：更新 Topic：分区从 1 扩到 3 并增量修改配置")
    void updatePartitionsAndConfig() throws Exception {
        prepareTopic();
        Assertions.assertEquals(200, service.updateTopic(request(3, null, Map.of("retention.ms", "120000"))).getCode());
        assertState(3, "120000", "300000");
    }

    /** 仅更新 Topic 配置：保留分区数和未传配置。 */
    @Test
    @DisplayName("真实 Kafka：仅更新 Topic 配置：保留分区数和未传配置")
    void updateConfigOnly() throws Exception {
        prepareTopic();
        Assertions.assertEquals(200, service.updateTopic(request(null, null, Map.of("retention.ms", "180000"))).getCode());
        assertState(1, "180000", "300000");
    }

    /** 缩减 Topic 分区：拒绝请求并保持原状态。 */
    @Test
    @DisplayName("真实 Kafka：缩减 Topic 分区：拒绝请求并保持原状态")
    void updateCannotShrinkPartitions() throws Exception {
        client.createTopics(List.of(new NewTopic(topicName, 3, (short) 1)
            .configs(Map.of("retention.ms", "60000", "segment.ms", "300000")))).all().get(10, TimeUnit.SECONDS);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(1, null, Map.of("retention.ms", "90000"))));
        assertState(3, "60000", "300000");
    }

    /** 删除 Topic：回读确认主题不存在。 */
    @Test
    @DisplayName("真实 Kafka：删除 Topic：回读确认主题不存在")
    void deleteTopic() throws Exception {
        prepareTopic();
        Assertions.assertEquals(200, service.deleteTopic(deleteRequest()).getCode());
        assertDeleted();
    }

    /** 反射 ADD：创建 Topic 并回读验证。 */
    @Test
    @DisplayName("真实 Kafka：反射 ADD：创建 Topic 并回读验证")
    void reflectiveCreate() throws Exception {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(TopicRemotingService.class, cluster);
        handler.handleAll(List.of(), List.of(request(1, 1, Map.of("retention.ms", "60000", "segment.ms", "300000")).getMetaData()),
            List.of(), List.of());
        assertState(1, "60000", "300000");
    }

    /** 反射 UPDATE：扩分区并回读验证。 */
    @Test
    @DisplayName("真实 Kafka：反射 UPDATE：扩分区并回读验证")
    void reflectiveUpdate() throws Exception {
        prepareTopic();
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(TopicRemotingService.class, cluster);
        TopicMetadata queried = service.getAllTopics(new GetTopics2Request()).getData().stream()
            .filter(topic -> topicName.equals(topic.getTopicName())).findFirst().orElseThrow();
        Assertions.assertEquals(1, queried.getReplicationFactor());
        queried.setId(1L);
        queried.setReadQueueNum(2);
        queried.setWriteQueueNum(2);
        queried.setTopicConfig("{\"retention.ms\":\"120000\"}");
        handler.handleAll(List.of(), List.of(), List.of(queried), List.of());
        assertState(2, "120000", "300000");
    }

    /** 反射 DELETE：删除 Topic 并确认不存在。 */
    @Test
    @DisplayName("真实 Kafka：反射 DELETE：删除 Topic 并确认不存在")
    void reflectiveDelete() throws Exception {
        prepareTopic();
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(TopicRemotingService.class, cluster);
        var queried = service.getAllTopics(new GetTopics2Request()).getData()
            .stream().filter(topic -> topicName.equals(topic.getTopicName())).findFirst().orElseThrow();
        Assertions.assertEquals(TopicMetadata.class, queried.getClass());
        queried.setId(1L);
        handler.handleAll(List.of(), List.of(), List.of(), List.of(queried));
        assertDeleted();
    }

    private void prepareTopic() throws Exception {
        client.createTopics(List.of(new NewTopic(topicName, 1, (short) 1)
            .configs(Map.of("retention.ms", "60000", "segment.ms", "300000")))).all().get(10, TimeUnit.SECONDS);
        assertState(1, "60000", "300000");
    }

    /** 非法配置预校验：分区和配置均保持不变。 */
    @Test
    @DisplayName("真实 Kafka：非法配置预校验：分区和配置均保持不变")
    void invalidConfigValidationLeavesPartitionsAndSettingsUnchanged() throws Exception {
        log.info("【真实预校验】非法 retention.ms 与扩分区一起提交，验证没有任何修改");
        prepareTopic();
        assertState(1, "60000", "300000");
        Assertions.assertThrows(ExecutionException.class, () -> service.updateTopic(request(3, null, Map.of("retention.ms", "not-a-number"))));
        assertState(1, "60000", "300000");
    }

    /** 单 Broker 请求两个副本：返回副本数错误。 */
    @Test
    @DisplayName("真实 Kafka：单 Broker 请求两个副本：返回副本数错误")
    void brokerRejectsImpossibleReplicationFactor() {
        log.info("【真实错误】单 Broker 请求 2 个副本，保留 Broker 错误");
        ExecutionException error = Assertions.assertThrows(ExecutionException.class, () -> service.createTopic(request(1, 2, null)));
        Assertions.assertInstanceOf(InvalidReplicationFactorException.class, error.getCause());
    }

    private DeleteTopicRequest deleteRequest() {
        DeleteTopicRequest request = new DeleteTopicRequest();
        request.setMetaData(request(null, null, null).getMetaData());
        return request;
    }

    private CreateTopic2Request request(Integer partitions, Integer replicas, Map<String, String> configs) {
        TopicMetadata metadata = new TopicMetadata();
        metadata.setId(1L);
        metadata.setTopicName(topicName);
        metadata.setReadQueueNum(partitions);
        metadata.setWriteQueueNum(partitions);
        metadata.setReplicationFactor(replicas);
        metadata.setTopicConfig(configs == null ? null : JSON.toJSONString(configs, SerializerFeature.WriteMapNullValue));
        CreateTopic2Request request = new CreateTopic2Request();
        request.setMetaData(metadata);
        return request;
    }

    private TopicDescription description() throws Exception {
        return client.describeTopics(List.of(topicName)).allTopicNames().get(10, TimeUnit.SECONDS).get(topicName);
    }

    private void assertState(int partitions, String retention, String segment) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                TopicDescription description = description();
                Config config = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
                if (description.partitions().size() == partitions && retention.equals(config.get("retention.ms").value())
                    && segment.equals(config.get("segment.ms").value())) {
                    log.info("【回读通过】主题={}，分区={}，retention.ms={}，segment.ms={}", topicName, partitions, retention, segment);
                    return;
                }
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof UnknownTopicOrPartitionException)) {
                    throw e;
                }
            }
            Thread.sleep(100);
        }
        Assertions.fail("主题状态未在等待期限内达到预期: " + topicName);
    }

    private void assertDeleted() throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (!client.listTopics().names().get(10, TimeUnit.SECONDS).contains(topicName)) {
                log.info("【删除验证通过】主题={} 已不存在", topicName);
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("删除后主题仍然存在: " + topicName);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client == null) {
            return;
        }
        try {
            try {
                client.deleteTopics(List.of(topicName)).all().get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof UnknownTopicOrPartitionException)) {
                    throw e;
                }
            }
        } finally {
            try {
                client.close(Duration.ofSeconds(5));
                ping.close(Duration.ofSeconds(5));
            } finally {
                SDKManage.getInstance().deleteClient(null, cluster.getUnique());
            }
        }
    }
}
