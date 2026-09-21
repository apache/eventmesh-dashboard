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
import org.apache.eventmesh.dashboard.common.model.metadata.InstanceUserMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleterUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.UserRemotingService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Requires the isolated RocketMQ 5.4.0 test Broker; no production resources are used. */
@Slf4j
class RocketMQUserIntegrationTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private String name;
    private UserRemotingService service;

    @BeforeEach
    void setUp() throws Exception {
        this.runtime = new RuntimeMetadata();
        this.runtime.setId(99501L);
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
        this.service = Remoting2Manage.getInstance().createRemotingService(UserRemotingService.class, this.runtime);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (this.service != null && this.name != null) {
                DeleterUserRequest request = new DeleterUserRequest();
                request.setMetaData(this.user());
                Assertions.assertEquals(200, this.service.deleteInstanceUser(request).getCode());
                log.info("【真实用户测试】已清理临时用户 {}", this.name);
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
    void createUser() throws Exception {
        log.info("【真实用户测试】创建普通用户并查询验证");
        this.create();
        InstanceUserMetadata actual = this.read();
        Assertions.assertEquals(this.name, actual.getUserName());
        Assertions.assertEquals("Normal", actual.getUserType());
        Assertions.assertEquals("enable", actual.getUserStatus());
        Assertions.assertNull(actual.getPassword());
    }

    @Test
    void updateUserThroughFramework() throws Exception {
        log.info("【真实用户测试】通过框架 UPDATE 分发禁用用户，保留用户类型");
        this.create();
        InstanceUserMetadata user = this.user();
        user.setPassword(null);
        user.setUserType(null);
        user.setUserStatus("disable");
        Remoting2Manage.getInstance().createDataMetadataHandler(UserRemotingService.class, this.runtime)
            .handleAll(null, null, List.of(user), null);
        Assertions.assertEquals("disable", this.read().getUserStatus());
        Assertions.assertEquals("Normal", this.read().getUserType());
    }

    @Test
    void listUsersThroughFramework() throws Exception {
        log.info("【真实用户测试】框架列表查询能找到临时用户，查询结果不包含密码");
        this.create();
        List<?> users = Remoting2Manage.getInstance().createDataMetadataHandler(UserRemotingService.class, this.runtime).getData();
        Assertions.assertTrue(users.stream().map(InstanceUserMetadata.class::cast).anyMatch(u -> this.name.equals(u.getUserName())));
        Assertions.assertTrue(users.stream().map(InstanceUserMetadata.class::cast).allMatch(u -> u.getPassword() == null));
    }

    @Test
    void deleteUserThroughFramework() throws Exception {
        log.info("【真实用户测试】通过框架 DELETE 删除用户并确认不存在");
        this.create();
        Remoting2Manage.getInstance().createDataMetadataHandler(UserRemotingService.class, this.runtime)
            .handleAll(null, null, null, List.of(this.user()));
        GetUserRequest query = new GetUserRequest();
        query.setMetaData(this.user());
        Assertions.assertTrue(this.service.getInstanceUser(query).getData().isEmpty());
    }

    private void create() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setMetaData(this.user());
        Assertions.assertEquals(200, this.service.createInstanceUser(request).getCode());
    }

    private InstanceUserMetadata user() {
        InstanceUserMetadata user = new InstanceUserMetadata();
        user.setId(99510L);
        user.setUserName(this.name);
        user.setPassword(UUID.randomUUID().toString());
        user.setUserType("Normal");
        user.setUserStatus("enable");
        return user;
    }

    private InstanceUserMetadata read() throws Exception {
        GetUserRequest query = new GetUserRequest();
        query.setMetaData(this.user());
        return this.service.getInstanceUser(query).getData().get(0);
    }

}
