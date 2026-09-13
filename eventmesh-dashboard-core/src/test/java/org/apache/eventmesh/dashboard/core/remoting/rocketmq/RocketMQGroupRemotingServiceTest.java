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

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQGroupRemotingServiceTest {

    private DefaultRemotingClient client;

    private RocketMQGroupRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new RocketMQGroupRemotingService();
        service.setClientWrapper(wrapper);
    }

    @Test
    void queryReturnsConfiguredGroups() throws Exception {
        log.info("Running Mock test: queryReturnsConfiguredGroups");
        SubscriptionGroupWrapper body = new SubscriptionGroupWrapper();
        body.getSubscriptionGroupTable().put("offline-group", new SubscriptionGroupConfig());
        body.getSubscriptionGroupTable().put("another-group", new SubscriptionGroupConfig());
        respond(ResponseCode.SUCCESS, null, body.encode());

        GetGroupResult result = logResult(service.getAllGroups(new GetGroupsRequest()));

        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(Set.of("offline-group", "another-group"), result.getData().stream()
            .map(GroupMetadata::getName).collect(Collectors.toSet()));
        Assertions.assertNull(result.getData().get(0).getMemberCount());
        ArgumentCaptor<RemotingCommand> request = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(request.capture(), ArgumentMatchers.eq(3000L));
        Assertions.assertEquals(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, request.getValue().getCode());
        Assertions.assertNull(request.getValue().getExtFields());
    }

    @Test
    void emptyTableReturnsEmptyList() throws Exception {
        log.info("Running Mock test: emptyTableReturnsEmptyList");
        respond(ResponseCode.SUCCESS, null, new SubscriptionGroupWrapper().encode());
        Assertions.assertTrue(logResult(service.getAllGroups(new GetGroupsRequest())).getData().isEmpty());
    }

    @Test
    void brokerFailurePreservesCodeAndMessage() throws Exception {
        log.info("Running Mock test: brokerFailurePreservesCodeAndMessage");
        respond(ResponseCode.NO_PERMISSION, "denied", null);
        GetGroupResult result = logResult(service.getAllGroups(new GetGroupsRequest()));
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertEquals("denied", result.getMessage());
        Assertions.assertNull(result.getData());
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, logResult(service.deleteGroup(deleteRequest("group-a"))).getCode());
    }

    @Test
    void missingBodyIsNotAnEmptySuccessfulQuery() throws Exception {
        log.info("Running Mock test: missingBodyIsNotAnEmptySuccessfulQuery");
        respond(ResponseCode.SUCCESS, null, null);
        logExpectedException(Assertions.assertThrows(RuntimeException.class, () -> logResult(service.getAllGroups(new GetGroupsRequest()))));
    }

    @Test
    void timeoutPropagatesToCaller() throws Exception {
        log.info("Running Mock test: timeoutPropagatesToCaller");
        RemotingTimeoutException timeout = new RemotingTimeoutException("broker", 3000);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenThrow(timeout);
        Assertions.assertSame(timeout, logExpectedException(Assertions.assertThrows(RemotingTimeoutException.class,
            () -> logResult(service.getAllGroups(new GetGroupsRequest())))));
    }

    @Test
    void deleteUsesGroupNameAndRetainsOffsets() throws Exception {
        log.info("Running Mock test: deleteUsesGroupNameAndRetainsOffsets");
        respond(ResponseCode.SUCCESS, null, null);
        Assertions.assertEquals(200, logResult(service.deleteGroup(deleteRequest("group-a"))).getCode());
        ArgumentCaptor<RemotingCommand> request = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(client).invokeSync(request.capture(), ArgumentMatchers.eq(3000L));
        Assertions.assertEquals(RequestCode.DELETE_SUBSCRIPTIONGROUP, request.getValue().getCode());
        DeleteSubscriptionGroupRequestHeader requestHeader =
            (DeleteSubscriptionGroupRequestHeader) request.getValue().readCustomHeader();
        Assertions.assertEquals("group-a", requestHeader.getGroupName());
        Assertions.assertFalse(requestHeader.isCleanOffset());
    }

    @Test
    void deleteRejectsMissingGroupBeforeRpc() {
        log.info("Running Mock test: deleteRejectsMissingGroupBeforeRpc");
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.deleteGroup(null))));
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.deleteGroup(new DeleteGroupRequest()))));
        logExpectedException(Assertions.assertThrows(IllegalArgumentException.class, () -> logResult(service.deleteGroup(deleteRequest(" ")))));
        Mockito.verifyNoInteractions(client);
    }

    private void respond(int code, String remark, byte[] body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, remark);
        response.setBody(body);
        Mockito.when(client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
            RemotingCommand command = invocation.getArgument(0);
            log.info("Mock RPC: requestCode={}, header={}, responseCode={}, remark={}",
                command.getCode(), command.readCustomHeader(), response.getCode(), response.getRemark());
            return response;
        });
    }

    private DeleteGroupRequest deleteRequest(String name) {
        GroupMetadata metadata = new GroupMetadata();
        metadata.setName(name);
        DeleteGroupRequest request = new DeleteGroupRequest();
        request.setMetaData(metadata);
        return request;
    }

    private <T extends GlobalResult<?>> T logResult(T result) {
        log.info("Service result: code={}, message={}, data={}", result.getCode(), result.getMessage(), result.getData());
        return result;
    }

    private <T extends Throwable> T logExpectedException(T exception) {
        log.info("Expected exception verified: type={}, message={}", exception.getClass().getSimpleName(), exception.getMessage());
        return exception;
    }
}
