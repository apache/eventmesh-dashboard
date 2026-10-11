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
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMemberMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.CreateGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.GroupMemberRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.UnregisterClientRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Registers a real protocol heartbeat; verifies subscription lookup, not message delivery. */
@Slf4j
class RocketMQGroupMemberIntegrationTest {

    @Test
    void queryLiveSubscriptionThroughManagedServiceAndSyncHandler() throws Exception {
        log.info("【真实 Broker 测试】订阅查询：验证上线、查询和注销");
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(95001L);
        runtime.setClusterId(95000L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        AbstractSimpleCreateSDKConfig config =
            ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.broker.port", 20911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        DefaultRemotingClient client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        String groupName = "dashboard_subscription_" + UUID.randomUUID().toString().replace("-", "");
        String topic = "dashboard_subscription_topic";
        GroupRemotingService groups = Remoting2Manage.getInstance().createRemotingService(GroupRemotingService.class, runtime);
        GroupMemberRemotingService service = Remoting2Manage.getInstance().createRemotingService(GroupMemberRemotingService.class, runtime);
        GroupMetadata group = new GroupMetadata();
        group.setName(groupName);
        try {
            CreateGroupRequest create = new CreateGroupRequest();
            create.setMetaData(group);
            Assertions.assertEquals(200, logResult(groups.createGroup(create)).getCode());
            GroupMemberMetadata filter = new GroupMemberMetadata();
            filter.setGroupName(groupName);
            filter.setTopicName(topic);
            GetSubscriptionRequest query = new GetSubscriptionRequest();
            query.setMetaData(filter);
            Assertions.assertTrue(logResult(service.getSubscription(query)).getData().isEmpty());

            ConsumerData consumer = new ConsumerData();
            consumer.setGroupName(groupName);
            consumer.setConsumeType(ConsumeType.CONSUME_PASSIVELY);
            consumer.setMessageModel(MessageModel.BROADCASTING);
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            consumer.getSubscriptionDataSet().add(new SubscriptionData(topic, "tag-a"));
            HeartbeatData heartbeat = new HeartbeatData();
            heartbeat.setClientID(groupName);
            heartbeat.getConsumerDataSet().add(consumer);
            RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
            command.setBody(heartbeat.encode());
            log.info("注册订阅：消费组={}，主题={}，过滤表达式=tag-a", groupName, topic);
            Assertions.assertEquals(ResponseCode.SUCCESS, client.invokeSync(command, 3000).getCode());

            GetSubscriptionResult result = logResult(service.getSubscription(query));
            Assertions.assertEquals(200, result.getCode());
            Assertions.assertEquals(1, result.getData().size());
            Assertions.assertEquals(topic, result.getData().get(0).getTopicName());
            Assertions.assertEquals(groupName, result.getData().get(0).getGroupName());
            List<BaseClusterIdBase> snapshot = Remoting2Manage.getInstance()
                .createDataMetadataHandler(GroupMemberRemotingService.class, runtime).getData();
            log.info("同步查询返回 {} 条订阅关系", snapshot.size());
            snapshot.stream().map(value -> (GroupMemberMetadata) value)
                .filter(value -> groupName.equals(value.getGroupName())).forEach(this::logConfig);
            Assertions.assertTrue(snapshot.stream().map(value -> (GroupMemberMetadata) value)
                .anyMatch(value -> groupName.equals(value.getGroupName()) && topic.equals(value.getTopicName())));

            unregister(client, groupName);
            Assertions.assertTrue(logResult(service.getSubscription(query)).getData().isEmpty());
        } finally {
            try {
                unregister(client, groupName);
            } finally {
                try {
                    DeleteGroupRequest delete = new DeleteGroupRequest();
                    delete.setMetaData(group);
                    Assertions.assertEquals(200, logResult(groups.deleteGroup(delete)).getCode());
                } finally {
                    try {
                        client.shutdown();
                    } finally {
                        SDKManage.getInstance().deleteClient(null, runtime.getUnique());
                    }
                }
            }
        }
    }

    private void unregister(DefaultRemotingClient client, String groupName) throws Exception {
        UnregisterClientRequestHeader header = new UnregisterClientRequestHeader();
        header.setClientID(groupName);
        header.setConsumerGroup(groupName);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UNREGISTER_CLIENT, header);
        Assertions.assertEquals(ResponseCode.SUCCESS, client.invokeSync(request, 3000).getCode());
    }

    private <T extends GlobalResult<?>> T logResult(T result) {
        log.info("操作结果：{}，返回码={}", Integer.valueOf(200).equals(result.getCode()) ? "成功" : "失败", result.getCode());
        if (!Integer.valueOf(200).equals(result.getCode())) {
            log.info("失败原因：{}", "denied".equals(result.getMessage()) ? "没有操作权限" : result.getMessage());
        }
        if (result.getData() instanceof java.util.List<?> values) {
            log.info("查询结果：共 {} 条", values.size());
            values.forEach(value -> logConfig((GroupMemberMetadata) value));
        }
        return result;
    }

    private void logConfig(GroupMemberMetadata subscription) {
        log.info("消费组={}，主题={}", subscription.getGroupName(), subscription.getTopicName());
    }
}
