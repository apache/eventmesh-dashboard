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
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.group.CreateGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import lombok.extern.slf4j.Slf4j;

/** Direct Broker operations on one named group; run creation before query/update and deletion last. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class RocketMQGroupIntegrationTest {

    private RuntimeMetadata runtime;

    private DefaultRemotingClient client;

    private RocketMQGroupRemotingService service;

    private String groupName;

    @BeforeEach
    void setUp() {
        runtime = new RuntimeMetadata();
        runtime.setId(94001L);
        runtime.setClusterId(94000L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        runtime.setHost(System.getProperty("rocketmq.broker.host", "127.0.0.1"));
        runtime.setPort(Integer.getInteger("rocketmq.broker.port", 20911));
        AbstractSimpleCreateSDKConfig config =
            ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(runtime.getHost());
        address.setPort(runtime.getPort());
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        service = Remoting2Manage.getInstance().createRemotingService(GroupRemotingService.class, runtime);
        groupName = System.getProperty("rocketmq.group.name", "dashboard_group_test_a");
        Assertions.assertSame(client, service.getClient());
    }

    @Test
    @Order(1)
    void createGroup() throws Exception {
        GroupMetadata group = new GroupMetadata();
        group.setName(groupName);
        group.setConsumeEnable(true);
        group.setConsumeBroadcastEnable(false);
        group.setRetryQueueNums(2);
        group.setRetryMaxTimes(8);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setMetaData(group);

        Assertions.assertEquals(200, service.createGroup(request).getCode());

        GroupMetadata actual = queryCreatedGroup();
        Assertions.assertTrue(actual.getConsumeEnable());
        Assertions.assertFalse(actual.getConsumeBroadcastEnable());
        Assertions.assertEquals(2, actual.getRetryQueueNums());
        Assertions.assertEquals(8, actual.getRetryMaxTimes());
        log.info("Created consumer group retained for MQ console inspection: {}", groupName);
    }

    @Test
    @Order(2)
    void updateGroup() throws Exception {
        GroupMetadata previous = queryCreatedGroup();
        GroupMetadata group = new GroupMetadata();
        group.setName(groupName);
        group.setConsumeEnable(false);
        group.setRetryMaxTimes(12);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setMetaData(group);

        // Like createTopic, createGroup handles both ADD and UPDATE for the same name.
        Assertions.assertEquals(200, service.createGroup(request).getCode());

        GroupMetadata actual = queryCreatedGroup();
        Assertions.assertFalse(actual.getConsumeEnable());
        Assertions.assertEquals(12, actual.getRetryMaxTimes());
        Assertions.assertEquals(previous.getConsumeBroadcastEnable(), actual.getConsumeBroadcastEnable());
        Assertions.assertEquals(previous.getRetryQueueNums(), actual.getRetryQueueNums());
    }

    @Test
    @Order(3)
    void queryGroup() throws Exception {
        GetGroupResult result = service.getAllGroups(new GetGroupsRequest());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertTrue(result.getData().stream().anyMatch(group -> groupName.equals(group.getName())));
    }

    @Test
    @Order(4)
    void deleteGroup() throws Exception {
        GroupMetadata group = new GroupMetadata();
        group.setName(groupName);
        DeleteGroupRequest request = new DeleteGroupRequest();
        request.setMetaData(group);

        Assertions.assertEquals(200, service.deleteGroup(request).getCode());

        GetGroupResult result = service.getAllGroups(new GetGroupsRequest());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertFalse(result.getData().stream().anyMatch(value -> groupName.equals(value.getName())));
    }

    private GroupMetadata queryCreatedGroup() throws Exception {
        GetGroupResult result = service.getAllGroups(new GetGroupsRequest());
        Assertions.assertEquals(200, result.getCode());
        return result.getData().stream().filter(group -> groupName.equals(group.getName())).findFirst()
            .orElseThrow(() -> new AssertionError("Consumer group " + groupName + " does not exist; run createGroup first"));
    }

    @AfterEach
    void tearDown() throws Exception {
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
