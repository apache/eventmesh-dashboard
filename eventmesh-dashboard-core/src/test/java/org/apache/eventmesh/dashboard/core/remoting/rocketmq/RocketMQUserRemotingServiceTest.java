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

import org.apache.eventmesh.dashboard.common.model.metadata.InstanceUserMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleterUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.UserInfo;
import org.apache.rocketmq.remoting.protocol.header.CreateUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateUserRequestHeader;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQUserRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQUserRemotingService service;

    @BeforeEach
    void setUp() throws Exception {
        this.client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, this.client);
        this.service = new RocketMQUserRemotingService();
        this.service.setClientWrapper(wrapper);
        this.respond(ResponseCode.SUCCESS, null);
    }

    private void respond(int code, String body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "test-result");
        response.setBody(body == null ? null : body.getBytes(StandardCharsets.UTF_8));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
    }

    private RemotingCommand captured() throws Exception {
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(this.client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        return captor.getValue();
    }

    @Test
    void createsUserWithExplicitTypeAndStatus() throws Exception {
        log.info("【用户模拟测试】创建普通用户，校验请求头及状态，不输出密码");
        CreateUserRequest request = this.create();
        Assertions.assertEquals(200, this.service.createInstanceUser(request).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals(RequestCode.AUTH_CREATE_USER, command.getCode());
        Assertions.assertEquals("test-user", ((CreateUserRequestHeader) command.readCustomHeader()).getUsername());
        UserInfo body = RemotingSerializable.decode(command.getBody(), UserInfo.class);
        Assertions.assertEquals("Normal", body.getUserType());
        Assertions.assertEquals("enable", body.getUserStatus());
        Assertions.assertEquals(request.getMetaData().getPassword(), body.getPassword());
    }

    @Test
    void updateUsesUpdateProtocolAndPreservesOmittedFields() throws Exception {
        log.info("【用户模拟测试】只禁用用户，更新请求不携带密码和用户类型");
        CreateUserRequest request = this.create();
        request.getMetaData().setPassword(null);
        request.getMetaData().setUserType(null);
        request.getMetaData().setUserStatus("disable");
        Assertions.assertEquals(200, this.service.updateInstanceUser(request).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals(RequestCode.AUTH_UPDATE_USER, command.getCode());
        Assertions.assertEquals("test-user", ((UpdateUserRequestHeader) command.readCustomHeader()).getUsername());
        UserInfo body = RemotingSerializable.decode(command.getBody(), UserInfo.class);
        Assertions.assertNull(body.getPassword());
        Assertions.assertNull(body.getUserType());
        Assertions.assertEquals("disable", body.getUserStatus());
    }

    @Test
    void deletesOnlyNamedUser() throws Exception {
        log.info("【用户模拟测试】删除指定用户");
        DeleterUserRequest request = new DeleterUserRequest();
        request.setMetaData(this.create().getMetaData());
        Assertions.assertEquals(200, this.service.deleteInstanceUser(request).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals(RequestCode.AUTH_DELETE_USER, command.getCode());
        Assertions.assertEquals("test-user", ((DeleteUserRequestHeader) command.readCustomHeader()).getUsername());
        Assertions.assertNull(command.getBody());
    }

    @Test
    void querySanitizesCredentialsAndUsesListEnvelope() throws Exception {
        log.info("【用户模拟测试】列表查询过滤密码，返回标准列表结果");
        this.respond(ResponseCode.SUCCESS, "[{\"username\":\"test-user\",\"password\":\"must-not-leak\","
            + "\"userType\":\"Normal\",\"userStatus\":\"enable\"}]");
        GetUserResult result = this.service.getInstanceUser(new GetUserRequest());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertNull(result.getData().get(0).getPassword());
        Assertions.assertFalse(result.toString().contains("must-not-leak"));
        Assertions.assertEquals(RequestCode.AUTH_LIST_USER, this.captured().getCode());
        Assertions.assertFalse(this.create().toString().contains("private-password"));
    }

    @Test
    void exactQueryAndMissingUser() throws Exception {
        log.info("【用户模拟测试】指定用户查询及用户不存在的空结果");
        GetUserRequest request = new GetUserRequest();
        request.setMetaData(this.create().getMetaData());
        Assertions.assertTrue(this.service.getInstanceUser(request).getData().isEmpty());
        Assertions.assertEquals(RequestCode.AUTH_GET_USER, this.captured().getCode());
    }

    @Test
    void rejectsInvalidOrIncompleteUser() {
        log.info("【用户模拟测试】拒绝缺失用户、空密码和非法类型");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createInstanceUser(null));
        CreateUserRequest request = this.create();
        request.getMetaData().setUserType("administrator");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createInstanceUser(request));
        request.getMetaData().setUserType("Normal");
        request.getMetaData().setPassword(" ");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.updateInstanceUser(request));
        Mockito.verifyNoInteractions(this.client);
    }

    @Test
    void rejectsV1AndUnsupportedBrokerExplicitly() throws Exception {
        log.info("【用户模拟测试】ACL 1.0 不发送用户协议，旧 Broker 不进行自动降级");
        CreateRemotingConfig config = new CreateRemotingConfig();
        config.setAclVersion(CreateRemotingConfig.AclVersion.V1);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.setConfig(config);
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, this.client);
        this.service.setClientWrapper(wrapper);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.createInstanceUser(this.create()));
        Mockito.verifyNoInteractions(this.client);
        config.setAclVersion(CreateRemotingConfig.AclVersion.V2);
        this.respond(ResponseCode.REQUEST_CODE_NOT_SUPPORTED, null);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.getInstanceUser(null));
    }

    @Test
    void propagatesFailuresAndRejectsMalformedLists() throws Exception {
        log.info("【用户模拟测试】保留权限错误，拒绝畸形用户列表");
        this.respond(ResponseCode.NO_PERMISSION, null);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, this.service.getInstanceUser(null).getCode());
        this.respond(ResponseCode.SUCCESS, "[{}]");
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getInstanceUser(null));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("test-broker", 3000));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> this.service.createInstanceUser(this.create()));
    }

    private CreateUserRequest create() {
        InstanceUserMetadata user = new InstanceUserMetadata();
        user.setUserName("test-user");
        user.setPassword("private-password");
        user.setUserType("Normal");
        user.setUserStatus("enable");
        CreateUserRequest request = new CreateUserRequest();
        request.setMetaData(user);
        return request;
    }

}
