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
import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.ClientRemotingService;

import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Requires the isolated RocketMQ 5.4.0 test Broker; no production resources are used. */
@Slf4j
class RocketMQClientIntegrationTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private String name;
    private ClientRemotingService service;

    private boolean groupCreated;

    @BeforeEach
    void setUp() throws Exception {
        this.runtime = new RuntimeMetadata();
        this.runtime.setId(99503L);
        this.runtime.setClusterId(99500L);
        this.runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance().getSimpleCreateSdkConfig(this.runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.admin.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.admin.broker.port", 21911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, this.runtime, config, this.runtime.getClusterType());
        this.client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, this.runtime.getUnique());
        this.name = "dashboard_admin_" + UUID.randomUUID().toString().replace("-", "");
        this.service = Remoting2Manage.getInstance().createRemotingService(ClientRemotingService.class, this.runtime);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (this.groupCreated) {
                DeleteSubscriptionGroupRequestHeader header = new DeleteSubscriptionGroupRequestHeader();
                header.setGroupName(this.name);
                RemotingCommand response = this.client.invokeSync(RemotingCommand.createRequestCommand(
                    RequestCode.DELETE_SUBSCRIPTIONGROUP, header), 3000);
                Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode());
                log.info("【真实客户端测试】已清理临时消费组 {}", this.name);
            }
        } finally {
            try {
                if (this.client != null) {
                    this.client.shutdown();
                }
            } finally {
                if (this.runtime != null) {
                    SDKManage.getInstance().deleteClient(null, this.runtime.getUnique());
                }
            }
        }
    }

    @Test
    void findsProducerHeartbeatThroughFramework() throws Exception {
        log.info("【真实客户端测试】发送临时生产者心跳，框架查询能找到连接");
        HeartbeatData heartbeat = this.heartbeat();
        ProducerData producer = new ProducerData();
        producer.setGroupName(this.name);
        heartbeat.getProducerDataSet().add(producer);
        this.send(heartbeat);
        List<?> clients = Remoting2Manage.getInstance().createDataMetadataHandler(ClientRemotingService.class, this.runtime).getData();
        Assertions.assertTrue(clients.stream().map(ClientMetadata.class::cast).anyMatch(c -> this.name.equals(c.getName())));
    }

    @Test
    void findsConsumerHeartbeat() throws Exception {
        log.info("【真实客户端测试】创建临时消费组并发送消费者心跳，查询连接信息");
        SubscriptionGroupConfig group = new SubscriptionGroupConfig();
        group.setGroupName(this.name);
        RemotingCommand create = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_SUBSCRIPTIONGROUP, null);
        create.setBody(RemotingSerializable.encode(group));
        Assertions.assertEquals(ResponseCode.SUCCESS, this.client.invokeSync(create, 3000).getCode());
        this.groupCreated = true;
        HeartbeatData heartbeat = this.heartbeat();
        ConsumerData consumer = new ConsumerData();
        consumer.setGroupName(this.name);
        consumer.setConsumeType(ConsumeType.CONSUME_PASSIVELY);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        heartbeat.getConsumerDataSet().add(consumer);
        this.send(heartbeat);
        ClientMetadata client = this.service.getClientList().getData().stream()
            .filter(c -> this.name.equals(c.getName())).findFirst().orElseThrow();
        Assertions.assertNotNull(client.getHost());
        Assertions.assertTrue(client.getPort() > 0);
        Assertions.assertNull(client.getPid());
    }

    private HeartbeatData heartbeat() {
        HeartbeatData heartbeat = new HeartbeatData();
        heartbeat.setClientID(this.name);
        return heartbeat;
    }

    private void send(HeartbeatData heartbeat) throws Exception {
        RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
        command.setBody(RemotingSerializable.encode(heartbeat));
        Assertions.assertEquals(ResponseCode.SUCCESS, this.client.invokeSync(command, 3000).getCode());
    }

}
