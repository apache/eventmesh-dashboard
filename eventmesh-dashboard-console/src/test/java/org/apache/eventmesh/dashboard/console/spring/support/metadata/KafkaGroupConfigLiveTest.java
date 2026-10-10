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

package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.serialization.StringDeserializer;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Opt-in integration coverage for the console metadata mapping through MetadataSyncManage and the managed Kafka client.
 * Requires the local KRaft broker at 127.0.0.1:19092 and ZooKeeper broker at 127.0.0.1:29092.
 */
@Tag("live")
@EnabledIfSystemProperty(named = "kafka.live", matches = "true")
@DisplayName("真实 Kafka：控制台 Group/Config 元数据映射")
@Slf4j
class KafkaGroupConfigLiveTest {

    private static final String GROUP_PREFIX = "dashboard-console-sync-";
    private static final String TOPIC_PREFIX = "dashboard-console-sync-";
    private ClusterMetadata cluster;
    private AdminClient admin;
    private AdminClient ping;
    private String uniqueKey;
    private String topic;
    private String group;
    private String address;
    private DataMetadataHandler<BaseClusterIdBase> groupHandler;
    private DataMetadataHandler<BaseClusterIdBase> configHandler;

    static List<Object[]> brokerTargets() {
        return List.of(new Object[] {ClusterType.STORAGE_KAFKA_RAFT, "127.0.0.1", 19092},
            new Object[] {ClusterType.STORAGE_KAFKA_BROKER, "127.0.0.1", 29092});
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerTargets")
    @DisplayName("真实 Broker：Group 查询/删除和 Config 查询/设置/删除经过控制台映射")
    void syncMappingsReachRealBroker(ClusterType type, String host, int port) throws Exception {
        this.cluster = new ClusterMetadata();
        this.cluster.setId(System.nanoTime());
        this.cluster.setClusterType(type);
        this.initialize(host, port);
        this.createOwnedResources();
        this.verifyGroupMapping();
        this.verifyConfigMapping();
    }

    private void initialize(String host, int port) {
        AbstractMultiCreateSDKConfig clientConfig = ConfigManage.getInstance()
            .getMultiCreateSdkConfig(this.cluster.getClusterType(), SDKTypeEnum.ADMIN);
        clientConfig.setKey(this.cluster.getId().toString());
        clientConfig.addNetAddress(NetAddress.create(host, port));
        this.address = host + ":" + port;
        SDKManage manager = SDKManage.getInstance();
        manager.createClient(SDKTypeEnum.ADMIN, this.cluster, clientConfig, this.cluster.getClusterType());
        this.uniqueKey = this.cluster.getUnique();
        this.admin = manager.getClient(SDKTypeEnum.ADMIN, this.uniqueKey);
        this.ping = manager.getClient(SDKTypeEnum.PING, this.uniqueKey);
        this.groupHandler = this.createHandler(DatabaseAndMetadataType.GROUP);
        this.configHandler = this.createHandler(DatabaseAndMetadataType.CONFIG);
    }

    private DataMetadataHandler<BaseClusterIdBase> createHandler(DatabaseAndMetadataType type) {
        DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        factory.setDatabaseAndMetadataMapper(mapper);
        factory.setMetadataType(mapper.getMetaType());
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.getSyncMetadataCreateFactoryMap().put(mapper.getMetaType(), factory);
        MetadataSyncResult result = manager.createMetadataSyncResult(new ArrayList<>(), mapper.getMetaType(), this.cluster);
        MetadataSyncManage.MetadataSyncConfig config = ReflectionTestUtils.invokeMethod(manager, "createMetadataSyncConfig",
            mapper.getMetaType(), result, this.cluster);
        Assertions.assertNotNull(config);
        return config.getClusterService();
    }

    private void createOwnedResources() throws Exception {
        this.topic = TOPIC_PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.group = GROUP_PREFIX + UUID.randomUUID().toString().replace("-", "");
        this.admin.createTopics(List.of(new NewTopic(this.topic, 1, (short) 1))).all().get(15, TimeUnit.SECONDS);
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapAddress());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, this.group);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
            TopicPartition partition = new TopicPartition(this.topic, 0);
            consumer.assign(List.of(partition));
            consumer.commitSync(Map.of(partition, new org.apache.kafka.clients.consumer.OffsetAndMetadata(0L)), Duration.ofSeconds(15));
        }
    }

    private String bootstrapAddress() {
        return this.address;
    }

    private void verifyGroupMapping() throws Exception {
        List<BaseClusterIdBase> rows = this.groupHandler.getData();
        GroupMetadata found = rows.stream().map(GroupMetadata.class::cast).filter(row -> this.group.equals(row.getName()))
            .findFirst().orElseThrow();
        Assertions.assertEquals(this.group, found.getName());
        this.groupHandler.handleAll(List.of(), List.of(), List.of(), List.of(found));
        Assertions.assertFalse(this.admin.listConsumerGroups().all().get(15, TimeUnit.SECONDS).stream()
            .anyMatch(row -> this.group.equals(row.groupId())));
        log.info("【真实验证通过】集群类型={}，Group={}，已通过控制台映射查询并删除", this.cluster.getClusterType(), this.group);
    }

    private void verifyConfigMapping() throws Exception {
        ConfigResource target = new ConfigResource(ConfigResource.Type.TOPIC, this.topic);
        ConfigMetadata discovered = this.configHandler.getData().stream().map(ConfigMetadata.class::cast)
            .filter(row -> MetadataType.TOPIC == row.getInstanceType() && this.topic.equals(row.getInstanceName()))
            .findFirst().orElseThrow();
        Assertions.assertNotNull(discovered.getConfigName());
        String originalRetention = this.readTopicConfig(target);

        ConfigMetadata ownedConfig = new ConfigMetadata();
        ownedConfig.setInstanceType(MetadataType.TOPIC);
        ownedConfig.setInstanceName(this.topic);
        ownedConfig.setConfigName("retention.ms");
        ownedConfig.setConfigValue("180000");
        this.configHandler.handleAll(List.of(), List.of(ownedConfig), List.of(), List.of());
        Assertions.assertEquals("180000", this.readTopicConfig(target));

        ownedConfig.setConfigValue("240000");
        this.configHandler.handleAll(List.of(), List.of(), List.of(ownedConfig), List.of());
        Assertions.assertEquals("240000", this.readTopicConfig(target));

        this.configHandler.handleAll(List.of(), List.of(), List.of(), List.of(ownedConfig));
        Assertions.assertEquals(originalRetention, this.readTopicConfig(target));
        log.info("【真实验证通过】集群类型={}，Topic={}，Config retention.ms 已查询、设置、更新并恢复",
            this.cluster.getClusterType(), this.topic);
    }

    private String readTopicConfig(ConfigResource target) throws Exception {
        return this.admin.describeConfigs(List.of(target)).all().get(15, TimeUnit.SECONDS).get(target)
            .get("retention.ms").value();
    }

    @AfterEach
    void cleanup() {
        this.cleanupOwnedResources();
    }

    private void cleanupOwnedResources() {
        Exception cleanupFailure = null;
        try {
            if (this.admin != null) {
                if (this.group != null) {
                    try {
                        this.admin.deleteConsumerGroups(List.of(this.group)).all().get(10, TimeUnit.SECONDS);
                    } catch (ExecutionException exception) {
                        if (!(exception.getCause() instanceof GroupIdNotFoundException)) {
                            cleanupFailure = exception;
                        }
                    } catch (Exception exception) {
                        cleanupFailure = exception;
                    }
                }
                if (this.topic != null) {
                    try {
                        this.admin.deleteTopics(List.of(this.topic)).all().get(10, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        cleanupFailure = append(cleanupFailure, exception);
                    }
                }
                try {
                    if (this.topic != null) {
                        Assertions.assertFalse(this.admin.listTopics().names().get(10, TimeUnit.SECONDS).contains(this.topic));
                    }
                    if (this.group != null) {
                        Assertions.assertFalse(this.admin.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream()
                            .anyMatch(row -> this.group.equals(row.groupId())));
                    }
                    log.info("【资源清理通过】集群类型={}，Topic={}，Group={} 均已删除",
                        this.cluster.getClusterType(), this.topic, this.group);
                } catch (Exception exception) {
                    cleanupFailure = append(cleanupFailure, exception);
                }
            }
        } catch (Exception exception) {
            cleanupFailure = append(cleanupFailure, exception);
        } finally {
            try {
                if (this.admin != null) {
                    this.admin.close(Duration.ofSeconds(5));
                }
            } catch (Exception exception) {
                cleanupFailure = append(cleanupFailure, exception);
            } finally {
                try {
                    if (this.ping != null) {
                        this.ping.close(Duration.ofSeconds(5));
                    }
                } catch (Exception exception) {
                    cleanupFailure = append(cleanupFailure, exception);
                } finally {
                    if (this.uniqueKey != null) {
                        SDKManage.getInstance().deleteClient(null, this.uniqueKey);
                    }
                }
            }
        }
        if (cleanupFailure != null) {
            Assertions.fail("Unable to clean up test-owned Kafka resources", cleanupFailure);
        }
    }

    private Exception append(Exception current, Exception next) {
        if (current == null) {
            return next;
        }
        current.addSuppressed(next);
        return current;
    }
}
