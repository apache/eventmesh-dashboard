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

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMethodMapper;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.AbstractGlobal2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.AclInfo;
import org.apache.rocketmq.remoting.protocol.header.CreateAclRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteAclRequestHeader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQAclRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQAclRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new RocketMQAclRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    void createMapsOneCompleteResourcePolicy() throws Exception {
        log.info("【模拟测试】创建 ACL：检查用户、资源、动作和来源地址");
        AclMetadata metadata = metadata();
        metadata.setActions(List.of("pub", "Sub", "Pub"));
        respond(ResponseCode.SUCCESS, null);
        Assertions.assertEquals(200, service.createAcl(create(metadata)).getCode());
        RemotingCommand command = captured();
        Assertions.assertEquals(RequestCode.AUTH_CREATE_ACL, command.getCode());
        Assertions.assertEquals("User:acl-test", ((CreateAclRequestHeader) command.readCustomHeader()).getSubject());
        AclInfo body = RemotingSerializable.decode(command.getBody(), AclInfo.class);
        Assertions.assertEquals("User:acl-test", body.getSubject());
        Assertions.assertEquals(1, body.getPolicies().size());
        Assertions.assertEquals("Custom", body.getPolicies().get(0).getPolicyType());
        Assertions.assertEquals(1, body.getPolicies().get(0).getEntries().size());
        AclInfo.PolicyEntryInfo entry = body.getPolicies().get(0).getEntries().get(0);
        Assertions.assertEquals("Topic:orders", entry.getResource());
        Assertions.assertEquals(List.of("Pub", "Sub"), entry.getActions());
        Assertions.assertEquals(List.of("127.0.0.1"), entry.getSourceIps());
        Assertions.assertEquals("Allow", entry.getDecision());
        log.info("校验通过：用户={}，资源={}，动作={}，权限={}", body.getSubject(), entry.getResource(), entry.getActions(), entry.getDecision());
    }

    @Test
    void updateSupportsExplicitDenyAndEmptySourceList() throws Exception {
        log.info("【模拟测试】更新 ACL：显式拒绝权限并清空来源限制");
        AclMetadata metadata = metadata();
        metadata.setPermissionType("deny");
        metadata.setPolicyType("Default");
        metadata.setSourceIps(List.of());
        metadata.setResourceName("orders-*");
        respond(ResponseCode.SUCCESS, null);
        Assertions.assertEquals(200, service.createAcl(create(metadata)).getCode());
        AclInfo body = RemotingSerializable.decode(captured().getBody(), AclInfo.class);
        Assertions.assertEquals("Default", body.getPolicies().get(0).getPolicyType());
        Assertions.assertEquals("Deny", body.getPolicies().get(0).getEntries().get(0).getDecision());
        Assertions.assertEquals("Topic:orders-*", body.getPolicies().get(0).getEntries().get(0).getResource());
        Assertions.assertTrue(body.getPolicies().get(0).getEntries().get(0).getSourceIps().isEmpty());
    }

    @Test
    void deletePreservesResourceAndPolicyScope() throws Exception {
        log.info("【模拟测试】删除 ACL：仅删除指定资源策略");
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata());
        respond(ResponseCode.SUCCESS, null);
        Assertions.assertEquals(200, service.deleteAcl(request).getCode());
        RemotingCommand command = captured();
        Assertions.assertEquals(RequestCode.AUTH_DELETE_ACL, command.getCode());
        DeleteAclRequestHeader header = (DeleteAclRequestHeader) command.readCustomHeader();
        Assertions.assertEquals("User:acl-test", header.getSubject());
        Assertions.assertEquals("Custom", header.getPolicyType());
        Assertions.assertEquals("Topic:orders", header.getResource());
        log.info("删除请求校验通过：用户={}，策略={}，资源={}", header.getSubject(), header.getPolicyType(), header.getResource());
    }

    @Test
    void missingResourceNeverDeletesWholeSubject() {
        log.info("【模拟测试】缺少资源时拒绝删除，避免扩大范围");
        AclMetadata metadata = metadata();
        metadata.setResourceName(null);
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteAcl(request));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteAcl(null));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void queryFlattensPoliciesWithoutCollapsingResources() throws Exception {
        log.info("【模拟测试】查询 ACL：同一用户的多个资源和策略分别返回");
        AclInfo acl = AclInfo.of("User:acl-test", List.of("Topic:orders", "Group:consumers"), List.of("Sub"), List.of("10.0.0.0/8"), "Allow");
        acl.setPolicies(new ArrayList<>(acl.getPolicies()));
        acl.getPolicies().get(0).setPolicyType("Custom");
        AclInfo.PolicyInfo policy = AclInfo.PolicyInfo.of(List.of("*"), List.of("All"), null, "Deny");
        policy.setPolicyType("Default");
        acl.getPolicies().add(policy);
        respond(ResponseCode.SUCCESS, RemotingSerializable.encode(List.of(acl)));
        GetAclsResult result = service.getAllAcls(new GetAcls2Request());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(3, result.getData().size());
        Assertions.assertEquals(3, result.getData().stream().map(AclMetadata::nodeUnique).distinct().count());
        Assertions.assertEquals(RequestCode.AUTH_LIST_ACL, captured().getCode());
        AclMetadata any = result.getData().stream().filter(value -> "Any".equals(value.getResourceType())).findFirst().orElseThrow();
        Assertions.assertEquals("*", any.getResourceName());
        Assertions.assertEquals("Default", any.getPolicyType());
        Assertions.assertTrue(any.getSourceIps().isEmpty());
        result.getData().forEach(value -> log.info("用户={}，策略={}，资源={}:{}，动作={}，权限={}",
            value.getPrincipal(), value.getPolicyType(), value.getResourceType(), value.getResourceName(), value.getActions(), value.getPermissionType()));
    }

    @Test
    void emptyBrokerListMayOmitBody() throws Exception {
        log.info("【模拟测试】Broker 空列表协议：无响应体或空数组都返回空列表");
        respond(ResponseCode.SUCCESS, null);
        Assertions.assertTrue(service.getAllAcls(null).getData().isEmpty());
        respond(ResponseCode.SUCCESS, "[]".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(service.getAllAcls(new GetAcls2Request()).getData().isEmpty());
    }

    @Test
    void brokerFailuresRemainFailuresForAllOperations() throws Exception {
        log.info("【模拟测试】权限错误及不支持的协议：保留 Broker 返回码");
        respond(ResponseCode.NO_PERMISSION, null);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, service.createAcl(create(metadata())).getCode());
        DeleteAclRequest delete = new DeleteAclRequest();
        delete.setMetaData(metadata());
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, service.deleteAcl(delete).getCode());
        GetAclsResult result = service.getAllAcls(null);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertEquals("test failure", result.getMessage());
        Assertions.assertNull(result.getData());
        respond(ResponseCode.REQUEST_CODE_NOT_SUPPORTED, null);
        Assertions.assertEquals(10000 + ResponseCode.REQUEST_CODE_NOT_SUPPORTED, service.getAllAcls(null).getCode());
    }

    @Test
    void malformedListNeverReturnsPartialSuccess() throws Exception {
        log.info("【模拟测试】无效 ACL 响应不返回部分成功结果");
        respond(ResponseCode.SUCCESS, "null".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalStateException.class, () -> service.getAllAcls(null));
        AclInfo valid = AclInfo.of("User:acl-test", List.of("Topic:orders"), List.of("Pub"), List.of(), "Allow");
        respond(ResponseCode.SUCCESS, RemotingSerializable.encode(List.of(valid, new AclInfo())));
        Assertions.assertThrows(IllegalStateException.class, () -> service.getAllAcls(null));
        respond(ResponseCode.SUCCESS, "not-json".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(RuntimeException.class, () -> service.getAllAcls(null));
    }

    @Test
    void invalidPermissionsAndSourcesAreRejectedBeforeRpc() {
        log.info("【模拟测试】非法动作、缺失字段和无效来源地址在 RPC 前拒绝");
        for (String action : List.of("Unknown", "Any", "invalid")) {
            AclMetadata metadata = metadata();
            metadata.setActions(List.of(action));
            Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        }
        AclMetadata metadata = metadata();
        metadata.setSourceIps(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        metadata.setSourceIps(List.of("invalid-ip"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        metadata.setSourceIps(List.of());
        metadata.setPermissionType(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(null));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void invalidIdentityAndWildcardAreRejected() {
        log.info("【模拟测试】非法用户和资源通配符不会发送请求");
        AclMetadata metadata = metadata();
        metadata.setPrincipal("acl-test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        metadata.setPrincipal("User:acl-test");
        metadata.setResourceName("orders*extra");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        metadata.setResourceName("orders");
        metadata.setOperation(1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create(metadata)));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void transportFailuresPropagate() throws Exception {
        log.info("【模拟测试】网络超时保留原始异常");
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("broker", 3000L));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> service.createAcl(create(metadata())));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> service.getAllAcls(null));
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata());
        Assertions.assertThrows(RemotingTimeoutException.class, () -> service.deleteAcl(request));
    }

    @Test
    void requestsSupportFrameworkReflectionAndUpsertMapping() throws Exception {
        log.info("【框架测试】请求可反射构造，创建方法映射新增与更新");
        for (Class<?> type : List.of(CreateAclRequest.class, DeleteAclRequest.class, GetAcls2Request.class)) {
            Assertions.assertInstanceOf(AbstractGlobal2Request.class, type.getDeclaredConstructor().newInstance());
        }
        RemotingServiceMethodMapper annotation = AclRemotingService.class.getMethod("createAcl", CreateAclRequest.class)
            .getAnnotation(RemotingServiceMethodMapper.class);
        Assertions.assertArrayEquals(new RemotingActionType[] {RemotingActionType.ADD, RemotingActionType.UPDATE}, annotation.value());
        AclMetadata entry = metadata();
        String identity = entry.nodeUnique();
        entry.setActions(List.of("Sub"));
        entry.setPermissionType("Deny");
        Assertions.assertEquals(identity, entry.nodeUnique());
    }

    private AclMetadata metadata() {
        AclMetadata metadata = new AclMetadata();
        metadata.setPrincipal("User:acl-test");
        metadata.setResourceType("Topic");
        metadata.setResourceName("orders");
        metadata.setActions(List.of("Pub"));
        metadata.setPermissionType("Allow");
        metadata.setSourceIps(List.of("127.0.0.1"));
        return metadata;
    }

    private CreateAclRequest create(AclMetadata metadata) {
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(metadata);
        return request;
    }

    private void respond(int code, byte[] body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "test failure");
        response.setBody(body);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
    }

    private RemotingCommand captured() throws Exception {
        ArgumentCaptor<RemotingCommand> capture = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(capture.capture(), ArgumentMatchers.eq(3000L));
        return capture.getValue();
    }
}
