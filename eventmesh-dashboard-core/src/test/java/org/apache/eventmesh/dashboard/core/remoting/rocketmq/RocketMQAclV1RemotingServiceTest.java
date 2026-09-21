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

import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RocketMQAclAccountMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig.AclVersion;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQAclV1RemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQAclRemotingService service;
    private CreateRemotingConfig config;

    @BeforeEach
    void setUp() throws Exception {
        this.client = Mockito.mock(DefaultRemotingClient.class);
        this.config = new CreateRemotingConfig();
        this.config.setAclVersion(AclVersion.V1);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.setConfig(this.config);
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, this.client);
        this.service = new RocketMQAclRemotingService();
        this.service.setClientWrapper(wrapper);
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenReturn(RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null));
    }

    @Test
    void createSerializesLegacyFieldsUsingManagedClient() throws Exception {
        log.info("【模拟测试】ACL 1.0 创建账号，检查完整配置和旧协议请求码");
        Assertions.assertEquals(200, this.service.createAcl(this.create(this.account())).getCode());
        RemotingCommand command = this.captured();
        // Round-trip the command header as the network codec does; no removed SDK header class is needed.
        ByteBuffer encoded = command.encode();
        encoded.getInt();
        RemotingCommand decoded = RemotingCommand.decode(encoded);
        Assertions.assertEquals(50, decoded.getCode());
        Assertions.assertEquals("test-access", decoded.getExtFields().get("accessKey"));
        Assertions.assertEquals("test-secret", decoded.getExtFields().get("secretKey"));
        Assertions.assertEquals("false", decoded.getExtFields().get("admin"));
        Assertions.assertEquals("", decoded.getExtFields().get("whiteRemoteAddress"));
        Assertions.assertEquals("DENY", decoded.getExtFields().get("defaultTopicPerm"));
        Assertions.assertEquals("DENY", decoded.getExtFields().get("defaultGroupPerm"));
        Assertions.assertEquals("orders=PUB|SUB,events=DENY", decoded.getExtFields().get("topicPerms"));
        Assertions.assertEquals("workers=SUB", decoded.getExtFields().get("groupPerms"));
        log.info("校验通过：请求码={}，账号={}，Topic 权限={}", decoded.getCode(),
            decoded.getExtFields().get("accessKey"), decoded.getExtFields().get("topicPerms"));
    }

    @Test
    void updateCanExplicitlyClearRules() throws Exception {
        log.info("【模拟测试】ACL 1.0 更新账号，空列表显式清除资源权限");
        RocketMQAclAccountMetadata account = this.account();
        account.setTopicPerms(List.of());
        account.setGroupPerms(List.of());
        Assertions.assertEquals(200, this.service.createAcl(this.create(account)).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals("", command.getExtFields().get("topicPerms"));
        Assertions.assertEquals("", command.getExtFields().get("groupPerms"));
        Assertions.assertEquals(50, command.getCode());
    }

    @Test
    void missingOrInvalidConfigurationCannotResetExistingAccount() {
        log.info("【模拟测试】ACL 1.0 拒绝缺失字段、重复资源与错误权限，禁止发送请求");
        RocketMQAclAccountMetadata account = this.account();
        account.setAdmin(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(account)));
        account.setAdmin(false);
        account.setTopicPerms(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(account)));
        account.setTopicPerms(List.of("orders=PUB", "orders=SUB"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(account)));
        account.setTopicPerms(List.of("orders=Allow"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(account)));
        account.setTopicPerms(List.of("orders=PUB,events=SUB"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(account)));
        Mockito.verifyNoInteractions(this.client);
    }

    @Test
    void deleteRequiresExplicitAccountScope() throws Exception {
        log.info("【模拟测试】ACL 1.0 删除必须明确指定整个账号，不能按资源误删");
        DeleteAclRequest request = new DeleteAclRequest();
        RocketMQAclAccountMetadata identity = new RocketMQAclAccountMetadata();
        identity.setAccessKey("test-access");
        request.setMetaData(identity);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.deleteAcl(request));
        Mockito.verifyNoInteractions(this.client);
        request.setDeleteAccount(true);
        Assertions.assertEquals(200, this.service.deleteAcl(request).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals(51, command.getCode());
        Assertions.assertEquals(1, command.getExtFields().size());
        Assertions.assertEquals("test-access", command.getExtFields().get("accessKey"));
    }

    @Test
    void queryReportsUnsupportedInsteadOfAnEmptyList() throws Exception {
        log.info("【模拟测试】ACL 1.0 查询能力缺失时明确失败，不伪造空列表");
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenReturn(RemotingCommand.createResponseCommand(ResponseCode.REQUEST_CODE_NOT_SUPPORTED, "unsupported"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.getAllAcls(new GetAcls2Request()));
        Assertions.assertEquals(54, this.captured().getCode());
    }

    @Test
    void queryDecodesLegacyAccountListWithoutExposingSecrets() throws Exception {
        log.info("【模拟测试】兼容支持查询的 V1 Broker，返回账号配置时移除密钥");
        RemotingCommand response = RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null);
        response.setBody(("{\"plainAccessConfigs\":[{\"accessKey\":\"test-access\",\"secretKey\":\"hidden-secret\","
            + "\"admin\":false,\"defaultTopicPerm\":\"DENY\",\"defaultGroupPerm\":\"DENY\","
            + "\"topicPerms\":[\"orders=PUB\"],\"groupPerms\":[]}]}").getBytes(StandardCharsets.UTF_8));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
        GetAclsResult result = this.service.getAllAcls(new GetAcls2Request());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(1, result.getData().size());
        RocketMQAclAccountMetadata account = (RocketMQAclAccountMetadata) result.getData().get(0);
        Assertions.assertEquals("test-access", account.getAccessKey());
        Assertions.assertNull(account.getSecretKey());
        Assertions.assertEquals(List.of("orders=PUB"), account.getTopicPerms());
        Assertions.assertEquals(List.of(), account.getGroupPerms());
        Assertions.assertEquals(false, account.getAdmin());
    }

    @Test
    void queryRejectsMalformedBodiesAndPreservesBrokerErrors() throws Exception {
        log.info("【模拟测试】V1 查询拒绝无效响应，空数组与 Broker 拒绝结果分别处理");
        RemotingCommand response = RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null);
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getAllAcls(new GetAcls2Request()));
        response.setBody("{}".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getAllAcls(new GetAcls2Request()));
        response.setBody("{\"plainAccessConfigs\":[null]}".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getAllAcls(new GetAcls2Request()));
        response.setBody("{\"plainAccessConfigs\":[]}".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(this.service.getAllAcls(new GetAcls2Request()).getData().isEmpty());
        response.setCode(ResponseCode.NO_PERMISSION);
        GetAclsResult result = this.service.getAllAcls(new GetAcls2Request());
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertNull(result.getData());
    }

    @Test
    void protocolSelectionRejectsMixedModelsAndNullVersion() {
        log.info("【模拟测试】按节点配置选择协议，拒绝 V1/V2 模型混用和空版本");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(new AclMetadata())));
        RocketMQAclAccountMetadata mixed = this.account();
        mixed.setResourceName("orders");
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(mixed)));
        this.config.setAclVersion(AclVersion.V2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.createAcl(this.create(this.account())));
        DeleteAclRequest request = new DeleteAclRequest();
        request.setDeleteAccount(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.deleteAcl(request));
        this.config.setAclVersion(null);
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.createAcl(this.create(this.account())));
        Mockito.verifyNoInteractions(this.client);
    }

    @Test
    void failuresNeverRetryWithAnotherProtocol() throws Exception {
        log.info("【模拟测试】ACL 1.0 Broker 拒绝或网络超时，不降级、不切换协议重试");
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenReturn(RemotingCommand.createResponseCommand(ResponseCode.NO_PERMISSION, "denied"));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, this.service.createAcl(this.create(this.account())).getCode());
        Mockito.verify(this.client, Mockito.times(1)).invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L));
        Mockito.clearInvocations(this.client);
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("broker", 3000));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> this.service.createAcl(this.create(this.account())));
        Mockito.verify(this.client, Mockito.times(1)).invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L));
    }

    @Test
    void accountIdentityDoesNotDependOnPermissionsAndLogsHideSecret() {
        log.info("【模拟测试】ACL 1.0 以账号标识资源，日志对象不显示密钥");
        RocketMQAclAccountMetadata account = this.account();
        String identity = account.nodeUnique();
        account.setTopicPerms(List.of());
        Assertions.assertEquals(identity, account.nodeUnique());
        Assertions.assertFalse(account.toString().contains(account.getSecretKey()));
    }

    private RemotingCommand captured() throws Exception {
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(this.client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        return captor.getValue();
    }

    private CreateAclRequest create(AclMetadata metadata) {
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(metadata);
        return request;
    }

    private RocketMQAclAccountMetadata account() {
        RocketMQAclAccountMetadata account = new RocketMQAclAccountMetadata();
        account.setAccessKey("test-access");
        account.setSecretKey("test-secret");
        account.setAdmin(false);
        account.setWhiteRemoteAddress("");
        account.setDefaultTopicPerm("DENY");
        account.setDefaultGroupPerm("DENY");
        account.setTopicPerms(List.of("orders=PUB|SUB", "events=DENY"));
        account.setGroupPerms(List.of("workers=SUB"));
        return account;
    }
}
