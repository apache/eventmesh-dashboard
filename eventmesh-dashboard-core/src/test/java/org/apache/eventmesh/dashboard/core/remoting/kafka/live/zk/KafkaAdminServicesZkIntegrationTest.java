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
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.UpdateConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTestLog;
import org.apache.eventmesh.dashboard.service.remoting.kafka.ConfigRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.kafka.GroupRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.kafka.OffsetRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

import lombok.extern.slf4j.Slf4j;

/**
 * ZooKeeper 环境：直接连接 127.0.0.1:29092，可在 IDEA 中单独运行每个测试方法，无需 VM options。
 * 切换测试环境时修改本类 BROKER_HOST / BROKER_PORT；每个方法独立准备并清理测试数据。
 */
@Slf4j
@Tag("zookeeper")
@DisplayName("ZooKeeper 环境：消费组、位点与配置管理")
@ExtendWith(KafkaTestLog.class)
class KafkaAdminServicesZkIntegrationTest {

    private static final String BROKER_HOST = "127.0.0.1";
    private static final int BROKER_PORT = 29092;

    private ClusterMetadata cluster;
    private AdminClient client;
    private AdminClient ping;
    private GroupRemotingService groups;
    private OffsetRemotingService offsets;
    private ConfigRemotingService configs;
    private String topic;
    private String group;
    private String address;
    private long timestamp;

    @BeforeEach
    void setUp() throws Exception {
        cluster = new ClusterMetadata();
        cluster.setId(System.nanoTime());
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        AbstractMultiCreateSDKConfig config = ConfigManage.getInstance().getMultiCreateSdkConfig(cluster.getClusterType(), SDKTypeEnum.ADMIN);
        config.setKey(cluster.getId().toString());
        String host = BROKER_HOST;
        int port = BROKER_PORT;
        address = host + ":" + port;
        config.addNetAddress(NetAddress.create(host, port));
        SDKManage manager = SDKManage.getInstance();
        manager.createClient(SDKTypeEnum.ADMIN, cluster, config, cluster.getClusterType());
        client = manager.getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
        ping = manager.getClient(SDKTypeEnum.PING, cluster.getUnique());
        var registry = Remoting2Manage.getInstance();
        groups = registry.createRemotingService(GroupRemotingService.class, cluster);
        offsets = registry.createRemotingService(OffsetRemotingService.class, cluster);
        configs = registry.createRemotingService(ConfigRemotingService.class, cluster);
        topic = "dashboard-kafka-admin-" + UUID.randomUUID();
        group = "dashboard-kafka-group-" + UUID.randomUUID();
        client.createTopics(List.of(new NewTopic(topic, 2, (short) 1).configs(Map.of("retention.ms", "600000", "segment.ms", "300000"))))
            .all().get(10, TimeUnit.SECONDS);
        timestamp = System.currentTimeMillis() - 10000;
        Properties producerConfig = new Properties();
        producerConfig.put("bootstrap.servers", address);
        producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig)) {
            for (int partition = 0; partition < 2; partition++) {
                for (int index = 0; index < 5; index++) {
                    producer.send(new ProducerRecord<>(topic, partition, timestamp + index * 1000, "key", "value"))
                        .get(10, TimeUnit.SECONDS);
                }
            }
        }
        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.assign(List.of(new TopicPartition(topic, 0), new TopicPartition(topic, 1)));
            consumer.commitSync(Map.of(new TopicPartition(topic, 0), new OffsetAndMetadata(2),
                new TopicPartition(topic, 1), new OffsetAndMetadata(3)), Duration.ofSeconds(10));
        }
        awaitEmpty();
        log.info("【数据准备】地址={}，Topic={}，Group={}，每分区消息=5，初始提交位点=2/3，消息起始时间={}", address, topic, group, timestamp);
    }

    /** 查询消费组：返回已创建的空组和成员数 0。 */
    @Test
    @DisplayName("真实 Kafka：查询消费组：返回已创建的空组和成员数 0")
    void queryGroups() throws Exception {
        var found = groups.getAllGroups(null).getData().stream().filter(row -> group.equals(row.getName())).findFirst().orElseThrow();
        log.info("【查询结果】Group={}，成员数={}，预期成员数=0", found.getName(), found.getMemberCount());
        Assertions.assertEquals(0, found.getMemberCount());
    }

    /** 反射查询消费组：结果包含测试组。 */
    @Test
    @DisplayName("真实 Kafka：反射查询消费组：结果包含测试组")
    void reflectiveQueryGroups() throws Exception {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(GroupRemotingService.class, cluster);
        Assertions.assertTrue(handler.getData().stream().map(GroupMetadata.class::cast).anyMatch(row -> group.equals(row.getName())));
    }

    /** 删除消费组：回读确认组不存在。 */
    @Test
    @DisplayName("真实 Kafka：删除消费组：回读确认组不存在")
    void deleteGroup() throws Exception {
        Assertions.assertEquals(200, groups.deleteGroup(deleteRequest()).getCode());
        assertGroupDeleted();
    }

    /** 反射 DELETE 消费组：回读确认组不存在。 */
    @Test
    @DisplayName("真实 Kafka：反射 DELETE 消费组：回读确认组不存在")
    void reflectiveDeleteGroup() throws Exception {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(GroupRemotingService.class, cluster);
        handler.handleAll(List.of(), List.of(), List.of(), List.of(deleteRequest().getMetaData()));
        assertGroupDeleted();
    }

    private DeleteGroupRequest deleteRequest() {
        GroupMetadata metadata = new GroupMetadata();
        metadata.setId(1L);
        metadata.setName(group);
        DeleteGroupRequest request = new DeleteGroupRequest();
        request.setMetaData(metadata);
        return request;
    }

    private void assertGroupDeleted() throws Exception {
        boolean exists = client.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream().anyMatch(row -> group.equals(row.groupId()));
        log.info("【删除回读】Group={}，实际存在={}，预期=false", group, exists);
        Assertions.assertFalse(exists);
    }

    /** 查询 Offset：提交位点为 2 和 3，日志末尾均为 5。 */
    @Test
    @DisplayName("真实 Kafka：查询 Offset：提交位点为 2 和 3，日志末尾均为 5")
    void queryOffsets() throws Exception {
        GetOffsetRequest query = new GetOffsetRequest();
        query.setGroupName(group);
        query.setTopic(topic);
        var rows = offsets.getOffsets(query).getData();
        rows.forEach(row -> log.info("【位点查询结果】Topic={}，分区={}，提交位点={}，日志末尾={}",
            row.getTopic(), row.getPartitionId(), row.getOffset(), row.getBrokerOffset()));
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(2L, rows.get(0).getOffset());
        Assertions.assertEquals(3L, rows.get(1).getOffset());
        Assertions.assertEquals(5L, rows.get(0).getBrokerOffset());
        Assertions.assertEquals(5L, rows.get(1).getBrokerOffset());
    }

    /** 起始位点重置：分区 0 改为 0，分区 1 保持 3。 */
    @Test
    @DisplayName("真实 Kafka：起始位点重置：分区 0 改为 0，分区 1 保持 3")
    void resetOffsetToBeginning() throws Exception {
        assertReset(ResetOffsetMode.CONSUME_FROM_FIRST_OFFSET, 0);
    }

    /** 末尾位点重置：分区 0 改为 5，分区 1 保持 3。 */
    @Test
    @DisplayName("真实 Kafka：末尾位点重置：分区 0 改为 5，分区 1 保持 3")
    void resetOffsetToEnd() throws Exception {
        assertReset(ResetOffsetMode.CONSUME_FROM_LAST_OFFSET, 5);
    }

    /** 时间戳重置：分区 0 改为 2，分区 1 保持 3。 */
    @Test
    @DisplayName("真实 Kafka：时间戳重置：分区 0 改为 2，分区 1 保持 3")
    void resetOffsetByTimestamp() throws Exception {
        assertReset(ResetOffsetMode.CONSUME_FROM_TIMESTAMP, 2);
    }

    /** 指定位点重置：分区 0 改为 1，分区 1 保持 3。 */
    @Test
    @DisplayName("真实 Kafka：指定位点重置：分区 0 改为 1，分区 1 保持 3")
    void resetOffsetToDesignatedPosition() throws Exception {
        assertReset(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET, 1);
    }

    private void assertReset(ResetOffsetMode mode, long expected) throws Exception {
        ResetOffsetRequest request = reset();
        request.setResetOffsetMode(mode);
        request.setTimestamp(mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP ? timestamp + 2000 : null);
        request.setOffset(mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET ? expected : null);
        Assertions.assertEquals(200, offsets.resetOffsets(request).getCode());
        assertOffsets(expected, 3);
        log.info("【回读通过】模式={}，分区0={}，分区1保持3", mode, expected);
    }

    /** 全 Topic 重置：两个分区都改为 4。 */
    @Test
    @DisplayName("真实 Kafka：全 Topic 重置：两个分区都改为 4")
    void resetAllTopicPartitions() throws Exception {
        ResetOffsetRequest request = reset();
        request.setPartitionId(null);
        request.setOffset(4L);
        Assertions.assertEquals(200, offsets.resetOffsets(request).getCode());
        assertOffsets(4, 4);
    }

    /** 时间戳无匹配消息：拒绝重置并保持原位点。 */
    @Test
    @DisplayName("真实 Kafka：时间戳无匹配消息：拒绝重置并保持原位点")
    void resetWithUnmatchedTimestampFails() throws Exception {
        ResetOffsetRequest request = reset();
        request.setResetOffsetMode(ResetOffsetMode.CONSUME_FROM_TIMESTAMP);
        request.setOffset(null);
        request.setTimestamp(System.currentTimeMillis() + 60000);
        Assertions.assertThrows(IllegalArgumentException.class, () -> offsets.resetOffsets(request));
        assertOffsets(2, 3);
    }

    /** 活跃消费组重置：拒绝请求并保持原位点。 */
    @Test
    @DisplayName("真实 Kafka：活跃消费组重置：拒绝请求并保持原位点")
    void activeGroupRejectsReset() throws Exception {
        try (KafkaConsumer<String, String> consumer = consumer()) {
            activate(consumer);
            Assertions.assertThrows(IllegalStateException.class, () -> offsets.resetOffsets(reset()));
            assertOffsets(2, 3);
        }
        awaitEmpty();
    }

    /** 活跃消费组删除：返回组非空错误。 */
    @Test
    @DisplayName("真实 Kafka：活跃消费组删除：返回组非空错误")
    void activeGroupRejectsDeletion() throws Exception {
        try (KafkaConsumer<String, String> consumer = consumer()) {
            activate(consumer);
            Assertions.assertInstanceOf(GroupNotEmptyException.class,
                Assertions.assertThrows(ExecutionException.class, () -> groups.deleteGroup(deleteRequest())).getCause());
            assertOffsets(2, 3);
        }
        awaitEmpty();
    }

    private void activate(KafkaConsumer<String, String> consumer) {
        consumer.subscribe(List.of(topic));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (consumer.assignment().isEmpty() && System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(200));
        }
        log.info("【活跃组准备】Group={}，已分配分区={}", group, consumer.assignment());
        Assertions.assertFalse(consumer.assignment().isEmpty());
    }

    private UpdateConfigRequest updateConfig(GetConfigRequest query, String name, String value) {
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setConfigType(query.getConfigType());
        request.setNode(query.getNode());
        request.setConfigObjectName(query.getConfigObjectName());
        ConfigMetadata metadata = new ConfigMetadata();
        metadata.setConfigName(name);
        metadata.setConfigValue(value);
        request.setMetaData(metadata);
        return request;
    }

    private GetConfigRequest topicConfig() {
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.TOPIC);
        request.setConfigObjectName(topic);
        return request;
    }

    /** 查询 Topic 配置：读取初始保留时间和分段时间。 */
    @Test
    @DisplayName("真实 Kafka：查询 Topic 配置：读取初始保留时间和分段时间")
    void queryTopicConfig() throws Exception {
        var rows = configs.getConfigs(topicConfig()).getData();
        rows.stream().filter(row -> List.of("retention.ms", "segment.ms").contains(row.getConfigName()))
            .forEach(row -> log.info("【配置查询结果】Topic={}，配置项={}，实际值={}", topic, row.getConfigName(), row.getConfigValue()));
        Assertions.assertEquals("600000", rows.stream().filter(row -> row.getConfigName().equals("retention.ms")).findFirst().orElseThrow().getConfigValue());
        Assertions.assertEquals("300000", rows.stream().filter(row -> row.getConfigName().equals("segment.ms")).findFirst().orElseThrow().getConfigValue());
    }

    /** 更新 Topic 配置：修改保留时间并保留分段时间。 */
    @Test
    @DisplayName("真实 Kafka：更新 Topic 配置：修改保留时间并保留分段时间")
    void updateTopicConfig() throws Exception {
        GetConfigRequest request = topicConfig();
        UpdateConfigRequest update = updateConfig(request, "retention.ms", "900000");
        Assertions.assertEquals(200, configs.updateConfigs(update).getCode());
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        var config = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
        log.info("【配置回读】Topic={}，retention.ms={}，预期=900000；segment.ms={}，预期=300000",
            topic, config.get("retention.ms").value(), config.get("segment.ms").value());
        Assertions.assertEquals("900000", config.get("retention.ms").value());
        Assertions.assertEquals("300000", config.get("segment.ms").value());
    }

    /** 非法 Topic 配置：拒绝修改并保留原值。 */
    @Test
    @DisplayName("真实 Kafka：非法 Topic 配置：拒绝修改并保留原值")
    void invalidTopicConfigUpdateFails() throws Exception {
        GetConfigRequest request = topicConfig();
        UpdateConfigRequest update = updateConfig(request, "retention.ms", "invalid");
        Assertions.assertThrows(ExecutionException.class, () -> configs.updateConfigs(update));
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        Assertions.assertEquals("600000", client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS)
            .get(resource).get("retention.ms").value());
    }

    /** 查询指定 Broker 配置：返回配置列表。 */
    @Test
    @DisplayName("真实 Kafka：查询指定 Broker 配置：返回配置列表")
    void queryBrokerConfig() throws Exception {
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.NODE);
        request.setNode(Integer.toString(client.describeCluster().nodes().get(10, TimeUnit.SECONDS).iterator().next().id()));
        var result = configs.getConfigs(request);
        log.info("【配置查询结果】scope={}，目标={}，状态码={}，配置条数={}",
            request.getConfigType(), request.getNode(), result.getCode(), result.getData().size());
        Assertions.assertFalse(result.getData().isEmpty());
    }

    /** 查询默认 Broker 配置：返回成功结果。 */
    @Test
    @DisplayName("真实 Kafka：查询默认 Broker 配置：返回成功结果")
    void queryDefaultBrokerConfig() throws Exception {
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.NODE);
        request.setNode("");
        var result = configs.getConfigs(request);
        log.info("【配置查询结果】scope={}，状态码={}，配置条数={}", request.getConfigType(), result.getCode(), result.getData().size());
        Assertions.assertEquals(200, result.getCode());
    }

    /** 更新指定 Broker 配置：回读生效后恢复原值。 */
    @Test
    @DisplayName("真实 Kafka：更新指定 Broker 配置：回读生效后恢复原值")
    void updateBrokerConfig() throws Exception {
        assertBrokerConfigUpdate(false, false);
    }

    /** 更新默认 Broker 配置：回读生效后恢复原值。 */
    @Test
    @DisplayName("真实 Kafka：更新默认 Broker 配置：回读生效后恢复原值")
    void updateDefaultBrokerConfig() throws Exception {
        assertBrokerConfigUpdate(true, false);
    }

    /** 通过Kafka 独立接口反射查询所有资源，保留目标身份，避免不同资源的同名配置合并。 */
    @Test
    @DisplayName("真实 Kafka：Kafka 独立 Config 接口反射查询包含资源目标")
    void reflectiveQueryConfigs() {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, cluster);
        List<ConfigMetadata> rows = handler.getData().stream().map(ConfigMetadata.class::cast).toList();
        Assertions.assertTrue(rows.stream().anyMatch(row -> row.getInstanceType() == MetadataType.TOPIC
            && topic.equals(row.getInstanceName()) && "retention.ms".equals(row.getConfigName())));
        Assertions.assertTrue(rows.stream().anyMatch(row -> row.getInstanceType() == MetadataType.RUNTIME));
        // With no dynamic default overrides Kafka returns no CLUSTER rows; never invent config entries.
        Assertions.assertTrue(rows.stream().filter(row -> row.getInstanceType() == MetadataType.CLUSTER)
            .allMatch(row -> "".equals(row.getInstanceName())));
        Assertions.assertEquals(rows.size(), rows.stream().map(ConfigMetadata::nodeUnique).distinct().count());
        log.info("【反射查询】配置条数={}，Topic、Broker、默认 Broker 均包含目标信息", rows.size());
    }

    /** ADD 动作使用原有 UpdateConfigRequest，目标由公共 Metadata 提供。 */
    @Test
    @DisplayName("真实 Kafka：反射 ADD 设置指定 Topic 配置")
    void reflectiveAddTopicConfig() throws Exception {
        ConfigMetadata entry = new ConfigMetadata();
        entry.setId(1L);
        entry.setInstanceType(MetadataType.TOPIC);
        entry.setInstanceName(topic);
        entry.setConfigName("compression.type");
        entry.setConfigValue("gzip");
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, cluster);
        handler.handleAll(List.of(), List.of(entry), List.of(), List.of());
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        ConfigEntry actual = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource).get("compression.type");
        Assertions.assertEquals("gzip", actual.value());
        Assertions.assertEquals(topic, entry.getInstanceName());
        Assertions.assertNull(entry.getInstanceId());
        log.info("【反射新增回读】Topic={}，compression.type={}，预期=gzip", topic, actual.value());
    }

    /** 查询结果直接交给 UPDATE，不再由测试手动填写 Request 的目标。 */
    @Test
    @DisplayName("真实 Kafka：反射 UPDATE 复用查询返回的配置目标")
    void reflectiveUpdateTopicConfig() throws Exception {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, cluster);
        ConfigMetadata entry = handler.getData().stream().map(ConfigMetadata.class::cast)
            .filter(row -> row.getInstanceType() == MetadataType.TOPIC && topic.equals(row.getInstanceName())
                && "retention.ms".equals(row.getConfigName())).findFirst().orElseThrow();
        entry.setId(1L);
        entry.setConfigValue("900000");
        handler.handleAll(List.of(), List.of(), List.of(entry), List.of());
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        var actual = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
        Assertions.assertEquals("900000", actual.get("retention.ms").value());
        Assertions.assertEquals("300000", actual.get("segment.ms").value());
        log.info("【反射修改回读】Topic={}，retention.ms={}，segment.ms={}，未传配置保留", topic,
            actual.get("retention.ms").value(), actual.get("segment.ms").value());
    }

    /** DELETE 只移除动态覆盖，保留其他配置项，并回退到 Broker 默认值。 */
    @Test
    @DisplayName("真实 Kafka：反射 DELETE 移除配置覆盖")
    void reflectiveDeleteTopicConfig() throws Exception {
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, cluster);
        ConfigMetadata entry = handler.getData().stream().map(ConfigMetadata.class::cast)
            .filter(row -> row.getInstanceType() == MetadataType.TOPIC && topic.equals(row.getInstanceName())
                && "retention.ms".equals(row.getConfigName())).findFirst().orElseThrow();
        entry.setId(1L);
        handler.handleAll(List.of(), List.of(), List.of(), List.of(entry));
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        var actual = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
        Assertions.assertNotEquals(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG, actual.get("retention.ms").source());
        Assertions.assertEquals("300000", actual.get("segment.ms").value());
        log.info("【反射删除回读】Topic={}，retention.ms 来源={}，segment.ms 保持不变", topic, actual.get("retention.ms").source());
    }

    /** Broker 配置只通过 Metadata 定位，回读后恢复原有覆盖。 */
    @Test
    @DisplayName("真实 Kafka：反射修改指定 Broker 配置并恢复")
    void reflectiveUpdateBrokerConfig() throws Exception {
        assertBrokerConfigUpdate(false, true);
    }

    /** 默认 Broker 配置有显式 CLUSTER 标识，不能由缺省目标误触发。 */
    @Test
    @DisplayName("真实 Kafka：反射修改默认 Broker 配置并恢复")
    void reflectiveUpdateDefaultBrokerConfig() throws Exception {
        assertBrokerConfigUpdate(true, true);
    }

    private void assertBrokerConfigUpdate(boolean defaults, boolean reflective) throws Exception {
        int brokerId = client.describeCluster().nodes().get(10, TimeUnit.SECONDS).iterator().next().id();
        GetConfigRequest request = new GetConfigRequest();
        request.setConfigType(ConfigType.NODE);
        request.setNode(!defaults ? Integer.toString(brokerId) : "");
        var resource = new ConfigResource(ConfigResource.Type.BROKER, !defaults ? Integer.toString(brokerId) : "");
        ConfigEntry original = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource).get("log.retention.ms");
        ConfigEntry.ConfigSource ownSource = !defaults ? ConfigEntry.ConfigSource.DYNAMIC_BROKER_CONFIG
            : ConfigEntry.ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG;
        try {
            if (reflective) {
                if (defaults) {
                    // Default-broker describeConfigs returns only existing overrides. Prepare one for query-to-write coverage.
                    client.incrementalAlterConfigs(Map.of(resource, List.of(new AlterConfigOp(
                        new ConfigEntry("log.retention.ms", "604800001"), AlterConfigOp.OpType.SET)))).all().get(10, TimeUnit.SECONDS);
                    boolean prepared = false;
                    for (int attempt = 0; attempt < 50; attempt++) {
                        ConfigEntry entry = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS)
                            .get(resource).get("log.retention.ms");
                        if (entry != null && "604800001".equals(entry.value())) {
                            prepared = true;
                            break;
                        }
                        Thread.sleep(100);
                    }
                    Assertions.assertTrue(prepared, "Default override fixture did not become visible");
                }
                var handler = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, cluster);
                ConfigMetadata entry = handler.getData().stream().map(ConfigMetadata.class::cast)
                    .filter(row -> row.getInstanceType() == (defaults ? MetadataType.CLUSTER : MetadataType.RUNTIME)
                        && request.getNode().equals(row.getInstanceName()) && "log.retention.ms".equals(row.getConfigName()))
                    .findFirst().orElseThrow();
                entry.setId(1L);
                entry.setConfigValue("604800123");
                handler.handleAll(List.of(), List.of(), List.of(entry), List.of());
                Assertions.assertEquals(request.getNode(), entry.getInstanceName());
            } else {
                UpdateConfigRequest update = updateConfig(request, "log.retention.ms", "604800123");
                Assertions.assertEquals(200, configs.updateConfigs(update).getCode());
            }
            boolean visible = false;
            for (int attempt = 0; attempt < 50; attempt++) {
                var current = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource).get("log.retention.ms");
                if (current != null && "604800123".equals(current.value()) && ownSource == current.source()) {
                    visible = true;
                    break;
                }
                Thread.sleep(100);
            }
            Assertions.assertTrue(visible, "Dynamic configuration did not become visible");
            log.info("【配置真实验证】scope={}，目标={}，增量修改并回读成功", defaults, request.getNode());
        } finally {
            boolean hadOverride = original != null && original.source() == ownSource;
            client.incrementalAlterConfigs(Map.of(resource, List.of(new AlterConfigOp(
                new ConfigEntry("log.retention.ms", hadOverride ? original.value() : null),
                hadOverride ? AlterConfigOp.OpType.SET : AlterConfigOp.OpType.DELETE)))).all().get(10, TimeUnit.SECONDS);
        }
    }

    private KafkaConsumer<String, String> consumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", address);
        properties.put("group.id", group);
        properties.put("enable.auto.commit", "false");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(properties);
    }

    private ResetOffsetRequest reset() {
        ResetOffsetRequest request = new ResetOffsetRequest();
        request.setGroupName(group);
        request.setTopic(topic);
        request.setPartitionId(0);
        request.setResetOffsetMode(ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET);
        request.setOffset(1L);
        return request;
    }

    private void assertOffsets(long first, long second) throws Exception {
        var result = client.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
        log.info("【位点回读】Group={}，Topic={}，分区0实际={}、预期={}，分区1实际={}、预期={}",
            group, topic, result.get(new TopicPartition(topic, 0)).offset(), first, result.get(new TopicPartition(topic, 1)).offset(), second);
        Assertions.assertEquals(first, result.get(new TopicPartition(topic, 0)).offset());
        Assertions.assertEquals(second, result.get(new TopicPartition(topic, 1)).offset());
    }

    private void awaitEmpty() throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (client.describeConsumerGroups(List.of(group)).all().get(10, TimeUnit.SECONDS).get(group).state() == ConsumerGroupState.EMPTY) {
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Consumer group did not become EMPTY");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client == null) {
            return;
        }
        List<Exception> errors = new ArrayList<>();
        try {
            if (group != null) {
                try {
                    client.deleteConsumerGroups(List.of(group)).all().get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof GroupIdNotFoundException)) {
                        errors.add(e);
                    }
                }
            }
            if (topic != null) {
                client.deleteTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
            }
        } finally {
            client.close(Duration.ofSeconds(5));
            ping.close(Duration.ofSeconds(5));
            SDKManage.getInstance().deleteClient(null, cluster.getUnique());
        }
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }
}
