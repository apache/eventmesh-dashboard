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
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.UserInfo;
import org.apache.rocketmq.remoting.protocol.header.CreateUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteUserRequestHeader;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Requires an ACL 2.0 Broker with metadata providers. Tests policy storage, not signed-client authorization. */
@Slf4j
class RocketMQAclIntegrationTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private AclRemotingService service;
    private String username;
    private boolean userCreated;

    @BeforeEach
    void setUp() throws Exception {
        runtime = new RuntimeMetadata();
        runtime.setId(97001L);
        runtime.setClusterId(97000L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.acl.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.acl.broker.port", 21911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        service = Remoting2Manage.getInstance().createRemotingService(AclRemotingService.class, runtime);
        username = "dashboard_acl_" + UUID.randomUUID().toString().replace("-", "");
        UserInfo user = UserInfo.of(username, UUID.randomUUID().toString(), "Normal", "Enable");
        RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.AUTH_CREATE_USER, new CreateUserRequestHeader(username));
        command.setBody(RemotingSerializable.encode(user));
        RemotingCommand response = client.invokeSync(command, 3000);
        Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), "ACL 2.0 test Broker required: " + response.getRemark());
        userCreated = true;
        log.info("临时 ACL 测试用户已创建：{}", username);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (userCreated) {
                try {
                    for (AclMetadata metadata : this.queryMine()) {
                        DeleteAclRequest delete = new DeleteAclRequest();
                        delete.setMetaData(metadata);
                        Assertions.assertEquals(200, service.deleteAcl(delete).getCode());
                    }
                } finally {
                    RemotingCommand response = client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.AUTH_DELETE_USER,
                        new DeleteUserRequestHeader(username)), 3000);
                    Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), response.getRemark());
                }
                log.info("临时 ACL 和用户已清理：{}", username);
            }
        } finally {
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

    @Test
    void createAcl() throws Exception {
        log.info("【真实 Broker 测试】创建资源授权并查询验证");
        this.create(this.metadata("orders"));
        List<AclMetadata> entries = this.queryMine();
        Assertions.assertEquals(1, entries.size());
        Assertions.assertEquals(List.of("Pub", "Sub"), entries.get(0).getActions());
        Assertions.assertEquals("Allow", entries.get(0).getPermissionType());
        Assertions.assertEquals(List.of("127.0.0.1"), entries.get(0).getSourceIps());
    }

    @Test
    void updateAcl() throws Exception {
        log.info("【真实 Broker 测试】更新一个资源授权，保留另一资源权限");
        this.create(this.metadata("orders"));
        this.create(this.metadata("untouched"));
        AclMetadata update = this.metadata("orders");
        update.setActions(List.of("Sub"));
        update.setPermissionType("Deny");
        update.setSourceIps(List.of("10.0.0.0/8"));
        this.create(update);
        List<AclMetadata> entries = this.queryMine();
        Assertions.assertEquals(2, entries.size());
        AclMetadata changed = entries.stream().filter(value -> "orders".equals(value.getResourceName())).findFirst().orElseThrow();
        Assertions.assertEquals(List.of("Sub"), changed.getActions());
        Assertions.assertEquals("Deny", changed.getPermissionType());
        Assertions.assertEquals(List.of("10.0.0.0/8"), changed.getSourceIps());
        AclMetadata unchanged = entries.stream().filter(value -> "untouched".equals(value.getResourceName())).findFirst().orElseThrow();
        Assertions.assertEquals(List.of("Pub", "Sub"), unchanged.getActions());
        Assertions.assertEquals("Allow", unchanged.getPermissionType());
        Assertions.assertEquals(List.of("127.0.0.1"), unchanged.getSourceIps());
    }

    @Test
    void queryAclsThroughFramework() throws Exception {
        log.info("【真实 Broker 测试】通过框架反射创建、更新和查询 ACL");
        AclMetadata first = this.metadata("orders");
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(AclRemotingService.class, runtime);
        handler.handleAll(List.of(), List.of(first), List.of(), List.of());
        Assertions.assertEquals(1, this.queryMine().size());
        first.setActions(List.of("Sub"));
        handler.handleAll(List.of(), List.of(), List.of(first), List.of());
        Assertions.assertEquals(List.of("Sub"), this.queryMine().get(0).getActions());
        AclMetadata second = this.metadata("orders");
        second.setPolicyType("Default");
        second.setPermissionType("Deny");
        this.create(second);
        List<AclMetadata> snapshot = handler.getData().stream().map(value -> (AclMetadata) value)
            .filter(value -> ("User:" + username).equals(value.getPrincipal())).collect(Collectors.toList());
        Assertions.assertEquals(2, snapshot.size());
        Assertions.assertEquals(2, snapshot.stream().map(AclMetadata::nodeUnique).distinct().count());
        handler.handleAll(List.of(), List.of(), List.of(), List.of(first));
        Assertions.assertEquals("Default", this.queryMine().get(0).getPolicyType());
    }

    @Test
    void deleteAcl() throws Exception {
        log.info("【真实 Broker 测试】删除指定资源授权，其他资源仍然存在");
        AclMetadata target = this.metadata("orders");
        this.create(target);
        this.create(this.metadata("untouched"));
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(target);
        Assertions.assertEquals(200, service.deleteAcl(request).getCode());
        List<AclMetadata> entries = this.queryMine();
        Assertions.assertEquals(1, entries.size());
        Assertions.assertEquals("untouched", entries.get(0).getResourceName());
    }

    private AclMetadata metadata(String resourceName) {
        AclMetadata metadata = new AclMetadata();
        metadata.setPrincipal("User:" + username);
        metadata.setResourceType("Topic");
        metadata.setResourceName(resourceName);
        metadata.setActions(List.of("Pub", "Sub"));
        metadata.setPermissionType("Allow");
        metadata.setSourceIps(List.of("127.0.0.1"));
        return metadata;
    }

    private void create(AclMetadata metadata) throws Exception {
        log.info("提交授权：用户={}，资源={}:{}，动作={}，权限={}",
            metadata.getPrincipal(), metadata.getResourceType(), metadata.getResourceName(), metadata.getActions(), metadata.getPermissionType());
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(metadata);
        var result = service.createAcl(request);
        Assertions.assertEquals(200, result.getCode(), result.getMessage());
    }

    private List<AclMetadata> queryMine() throws Exception {
        GetAclsResult result = service.getAllAcls(new GetAcls2Request());
        Assertions.assertEquals(200, result.getCode(), result.getMessage());
        List<AclMetadata> entries = result.getData().stream().filter(value -> ("User:" + username).equals(value.getPrincipal()))
            .collect(Collectors.toList());
        entries.forEach(value -> log.info("查询授权：用户={}，策略={}，资源={}:{}，动作={}，权限={}",
            value.getPrincipal(), value.getPolicyType(), value.getResourceType(), value.getResourceName(), value.getActions(), value.getPermissionType()));
        return entries;
    }
}
