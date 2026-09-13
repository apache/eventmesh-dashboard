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
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.CreateGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RocketMQGroupRemotingService extends AbstractRocketMQRemotingService implements GroupRemotingService {

    @Override
    public BaseGlobalResult createGroup(CreateGroupRequest createGroupRequest) throws Exception {
        GroupMetadata metadata = createGroupRequest == null ? null : createGroupRequest.getMetaData();
        if (metadata == null || StringUtils.isBlank(metadata.getName())) {
            throw new IllegalArgumentException("Consumer group name must not be blank");
        }
        if (metadata.getRetryQueueNums() != null && metadata.getRetryQueueNums() < 0
            || metadata.getRetryMaxTimes() != null && metadata.getRetryMaxTimes() < 0) {
            throw new IllegalArgumentException("Consumer group retry queue count and retry limit must not be negative");
        }
        // RocketMQ replaces the group configuration; preserve fields omitted by the caller.
        RemotingCommand query = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = this.invokeSync(query);
        if (response.getCode() != ResponseCode.SUCCESS) {
            return this.buildResult(response, new BaseGlobalResult());
        }
        if (response.getBody() == null) {
            throw new IllegalStateException("RocketMQ returned no subscription group configuration");
        }
        SubscriptionGroupWrapper wrapper = RemotingSerializable.decode(response.getBody(), SubscriptionGroupWrapper.class);
        if (wrapper == null || wrapper.getSubscriptionGroupTable() == null) {
            throw new IllegalStateException("RocketMQ returned no subscription group configuration");
        }
        SubscriptionGroupConfig config = wrapper.getSubscriptionGroupTable().get(metadata.getName());
        if (config == null) {
            config = new SubscriptionGroupConfig();
            config.setGroupName(metadata.getName());
        }
        if (metadata.getConsumeEnable() != null) {
            config.setConsumeEnable(metadata.getConsumeEnable());
        }
        if (metadata.getConsumeBroadcastEnable() != null) {
            config.setConsumeBroadcastEnable(metadata.getConsumeBroadcastEnable());
        }
        if (metadata.getRetryQueueNums() != null) {
            config.setRetryQueueNums(metadata.getRetryQueueNums());
        }
        if (metadata.getRetryMaxTimes() != null) {
            config.setRetryMaxTimes(metadata.getRetryMaxTimes());
        }
        // The Broker expects attribute changes; an empty map preserves its existing attributes.
        config.setAttributes(Collections.emptyMap());
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_SUBSCRIPTIONGROUP, null);
        request.setBody(RemotingSerializable.encode(config));
        return this.buildResult(this.invokeSync(request), new BaseGlobalResult());
    }

    @Override
    public GetGroupResult getAllGroups(GetGroupsRequest getGroupsRequest) throws Exception {
        RocketMQFunction<SubscriptionGroupWrapper> handlerFunction = wrapper -> {
            if (wrapper == null || wrapper.getSubscriptionGroupTable() == null) {
                throw new IllegalStateException("RocketMQ returned no subscription group configuration");
            }
            List<GroupMetadata> groupMetadataList = new ArrayList<>();
            wrapper.getSubscriptionGroupTable().forEach((name, config) -> {
                GroupMetadata groupMetadata = new GroupMetadata();
                groupMetadata.setName(name);
                groupMetadata.setConsumeEnable(config.isConsumeEnable());
                groupMetadata.setConsumeBroadcastEnable(config.isConsumeBroadcastEnable());
                groupMetadata.setRetryQueueNums(config.getRetryQueueNums());
                groupMetadata.setRetryMaxTimes(config.getRetryMaxTimes());
                groupMetadataList.add(groupMetadata);
            });
            return groupMetadataList;
        };
        // Omit pagination fields to request the full table, including offline subscription groups.
        RequestHandler requestHandler = RequestHandler.builder().code(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG)
            .resultClass(GetGroupResult.class).resultDataClass(SubscriptionGroupWrapper.class).handlerFunction(handlerFunction).build();
        return this.invokeSync(requestHandler);
    }

    @Override
    public BaseGlobalResult deleteGroup(DeleteGroupRequest deleteGroupRequest) throws Exception {
        if (deleteGroupRequest == null || deleteGroupRequest.getMetaData() == null
            || StringUtils.isBlank(deleteGroupRequest.getMetaData().getName())) {
            throw new IllegalArgumentException("Consumer group name must not be blank");
        }
        DeleteSubscriptionGroupRequestHeader requestHeader = new DeleteSubscriptionGroupRequestHeader();
        requestHeader.setGroupName(deleteGroupRequest.getMetaData().getName());
        // Delete the selected Broker's group configuration while retaining its consumption offsets.
        requestHeader.setCleanOffset(false);
        return this.invokeSync(RequestCode.DELETE_SUBSCRIPTIONGROUP, requestHeader, new BaseGlobalResult());
    }
}
