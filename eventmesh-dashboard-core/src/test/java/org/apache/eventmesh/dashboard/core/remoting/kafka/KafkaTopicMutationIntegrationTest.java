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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.KafkaTopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.kafka.topic.TopicRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.kafka.TopicRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnabledIfSystemProperty(named = "kafka.integration", matches = "true")
class KafkaTopicMutationIntegrationTest {

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
        config.addNetAddress(NetAddress.create(System.getProperty("kafka.broker.host", "127.0.0.1"), Integer.getInteger("kafka.broker.port", 19092)));
        SDKManage manager = SDKManage.getInstance();
        manager.createClient(SDKTypeEnum.ADMIN, cluster, config, cluster.getClusterType());
        client = manager.getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
        ping = manager.getClient(SDKTypeEnum.PING, cluster.getUnique());
        service = Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, cluster);
        topicName = "dashboard-kafka-crud-" + UUID.randomUUID();
    }

    @Test
    void createUpdateDeleteThroughService() throws Exception {
        log.info("【真实创建】主题={}，分区=1，副本=1，设置 retention.ms 和 segment.ms", topicName);
        TopicRequest create = request(1, 1, Map.of("retention.ms", "60000", "segment.ms", "300000"));
        Assertions.assertEquals(200, service.createTopic(create).getCode());
        assertState(1, "60000", "300000");
        Assertions.assertEquals(1, description().partitions().get(0).replicas().size());
        Assertions.assertInstanceOf(TopicExistsException.class,
            Assertions.assertThrows(ExecutionException.class, () -> service.createTopic(create)).getCause());

        log.info("【真实更新】分区 1→3，retention.ms 60000→120000，保留 segment.ms");
        Assertions.assertEquals(200, service.updateTopic(request(3, null, Map.of("retention.ms", "120000"))).getCode());
        assertState(3, "120000", "300000");
        Assertions.assertEquals(200, service.updateTopic(request(null, null, Map.of("retention.ms", "180000"))).getCode());
        assertState(3, "180000", "300000");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateTopic(request(1, null, Map.of("retention.ms", "90000"))));
        assertState(3, "180000", "300000");

        log.info("【真实删除】删除主题并回读确认不存在");
        Assertions.assertEquals(200, service.deleteTopic(request(null, null, null)).getCode());
        assertDeleted();
    }

    @Test
    void reflectiveActionsUseSeparateCreateAndUpdateMappings() throws Exception {
        log.info("【真实反射】验证 ADD、UPDATE、DELETE 分别分发到 Kafka 方法，主题={}", topicName);
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(TopicRemotingService.class, cluster);
        handler.handleAll(List.of(), List.of(request(1, 1, Map.of("retention.ms", "60000", "segment.ms", "300000")).getMetaData()),
            List.of(), List.of());
        assertState(1, "60000", "300000");
        handler.handleAll(List.of(), List.of(), List.of(request(2, null, Map.of("retention.ms", "120000")).getMetaData()), List.of());
        assertState(2, "120000", "300000");
        handler.handleAll(List.of(), List.of(), List.of(), List.of(request(null, null, null).getMetaData()));
        assertDeleted();
    }

    @Test
    void invalidConfigValidationLeavesPartitionsAndSettingsUnchanged() throws Exception {
        log.info("【真实预校验】非法 retention.ms 与扩分区一起提交，验证没有任何修改");
        service.createTopic(request(1, 1, Map.of("retention.ms", "60000", "segment.ms", "300000")));
        assertState(1, "60000", "300000");
        Assertions.assertThrows(ExecutionException.class, () -> service.updateTopic(request(3, null, Map.of("retention.ms", "not-a-number"))));
        assertState(1, "60000", "300000");
    }

    @Test
    void brokerRejectsImpossibleReplicationFactor() {
        log.info("【真实错误】单 Broker 请求 2 个副本，保留 Broker 错误");
        ExecutionException error = Assertions.assertThrows(ExecutionException.class, () -> service.createTopic(request(1, 2, null)));
        Assertions.assertInstanceOf(InvalidReplicationFactorException.class, error.getCause());
    }

    private TopicRequest request(Integer partitions, Integer replicas, Map<String, String> configs) {
        KafkaTopicMetadata metadata = new KafkaTopicMetadata();
        metadata.setId(1L);
        metadata.setTopicName(topicName);
        metadata.setPartitionCount(partitions);
        metadata.setReplicationFactor(replicas);
        metadata.setConfigs(configs);
        TopicRequest request = new TopicRequest();
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
