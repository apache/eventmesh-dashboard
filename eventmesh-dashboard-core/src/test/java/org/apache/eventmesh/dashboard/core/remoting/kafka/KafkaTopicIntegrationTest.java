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
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.kafka.TopicRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnabledIfSystemProperty(named = "kafka.integration", matches = "true")
class KafkaTopicIntegrationTest {

    @Test
    void queryThroughBrokerRegistration() throws Exception {
        verifyQuery(ClusterType.STORAGE_KAFKA_BROKER);
    }

    @Test
    void queryThroughRaftRegistration() throws Exception {
        verifyQuery(ClusterType.STORAGE_KAFKA_RAFT);
    }

    private void verifyQuery(ClusterType clusterType) throws Exception {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(clusterType == ClusterType.STORAGE_KAFKA_BROKER ? 98001L : 98002L);
        cluster.setClusterType(clusterType);
        AbstractMultiCreateSDKConfig config = ConfigManage.getInstance().getMultiCreateSdkConfig(clusterType, SDKTypeEnum.ADMIN);
        config.setKey(cluster.getId().toString());
        String host = System.getProperty("kafka.broker.host", "127.0.0.1");
        int port = Integer.getInteger("kafka.broker.port", 19092);
        config.addNetAddress(NetAddress.create(host, port));
        SDKManage manager = SDKManage.getInstance();
        manager.createClient(SDKTypeEnum.ADMIN, cluster, config, clusterType);
        AdminClient client = manager.getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
        AdminClient ping = manager.getClient(SDKTypeEnum.PING, cluster.getUnique());
        String prefix = "dashboard-kafka-query-" + UUID.randomUUID();
        List<String> names = List.of(prefix + "-one", prefix + "-three");
        try {
            log.info("【真实 Kafka】地址={}:{}，注册类型={}，准备自有主题={}", host, port, clusterType, names);
            client.createTopics(List.of(new NewTopic(names.get(0), 1, (short) 1), new NewTopic(names.get(1), 3, (short) 1)))
                .all().get(30, TimeUnit.SECONDS);
            KafkaTopicRemotingService service = Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, cluster);
            Assertions.assertSame(client, service.getClient(), "必须复用 SDKManage 的 ADMIN 客户端");
            // 创建成功后的集群元数据传播可能稍有延迟，有限重试直到两个主题都可见。
            GetTopicsResult result = null;
            for (int attempt = 0; attempt < 20; attempt++) {
                result = service.getAllTopics(new GetTopics2Request());
                if (result.getData().stream().map(TopicMetadata::getTopicName).collect(Collectors.toSet()).containsAll(names)) {
                    break;
                }
                Thread.sleep(200);
            }
            Assertions.assertNotNull(result);
            Assertions.assertEquals(200, result.getCode());
            assertTopics(result.getData(), names);
            List<TopicMetadata> reflected = Remoting2Manage.getInstance()
                .createDataMetadataHandler(TopicRemotingService.class, cluster).getData().stream().map(TopicMetadata.class::cast).toList();
            assertTopics(reflected, names);
            log.info("【验证通过】托管客户端、直接查询、反射构造请求及列表转换均通过，类型={}", clusterType);
        } finally {
            try {
                client.deleteTopics(names).all().get(30, TimeUnit.SECONDS);
                log.info("【清理】删除本次测试主题={}", names);
            } finally {
                try {
                    client.close(Duration.ofSeconds(5));
                    ping.close(Duration.ofSeconds(5));
                } finally {
                    manager.deleteClient(null, cluster.getUnique());
                }
            }
        }
    }

    private void assertTopics(List<TopicMetadata> topics, List<String> names) {
        Map<String, TopicMetadata> byName = topics.stream().collect(Collectors.toMap(TopicMetadata::getTopicName, topic -> topic));
        Assertions.assertTrue(byName.keySet().containsAll(names), "查询结果应包含本次创建的两个主题");
        for (int i = 0; i < names.size(); i++) {
            TopicMetadata topic = byName.get(names.get(i));
            Assertions.assertEquals(i == 0 ? 1 : 3, topic.getReadQueueNum());
            Assertions.assertEquals(topic.getReadQueueNum(), topic.getWriteQueueNum());
            Assertions.assertNull(topic.getRetentionMs());
            log.info("【结果】主题={}，读分区={}，写分区={}", topic.getTopicName(), topic.getReadQueueNum(), topic.getWriteQueueNum());
        }
    }
}
