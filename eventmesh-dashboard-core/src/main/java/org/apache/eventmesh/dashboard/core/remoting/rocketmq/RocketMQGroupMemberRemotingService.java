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

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMemberMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionResult;
import org.apache.eventmesh.dashboard.service.remoting.GroupMemberRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RocketMQGroupMemberRemotingService extends AbstractRocketMQRemotingService implements GroupMemberRemotingService {

    /**
     * Reads Broker-local heartbeat subscriptions. Without a group filter, enumerates configured groups.
     * Offline groups contribute no relationships; database IDs and producer relationships are not inferred.
     */
    @Override
    public GetSubscriptionResult getSubscription(GetSubscriptionRequest request) throws Exception {
        GroupMemberMetadata filter = request == null ? null : request.getMetaData();
        String groupName = filter == null ? null : filter.getGroupName();
        String topicName = filter == null ? null : filter.getTopicName();
        if (groupName != null && StringUtils.isBlank(groupName) || topicName != null && StringUtils.isBlank(topicName)) {
            throw new IllegalArgumentException("Subscription group and topic filters must not be blank");
        }
        List<String> groups;
        if (groupName != null) {
            groups = Collections.singletonList(groupName);
        } else {
            RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null));
            if (response.getCode() != ResponseCode.SUCCESS) {
                return this.buildResult(response, new GetSubscriptionResult());
            }
            SubscriptionGroupWrapper body = this.decodeBody(response, SubscriptionGroupWrapper.class);
            if (body.getSubscriptionGroupTable() == null) {
                throw new IllegalStateException("RocketMQ returned no subscription group table");
            }
            groups = new ArrayList<>(body.getSubscriptionGroupTable().keySet());
            Collections.sort(groups);
        }
        List<GroupMemberMetadata> subscriptions = new ArrayList<>();
        for (String group : groups) {
            GetConsumerConnectionListRequestHeader header = new GetConsumerConnectionListRequestHeader();
            header.setConsumerGroup(group);
            RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST, header));
            // Offline configuration does not establish an active subscription relationship.
            if (response.getCode() == ResponseCode.CONSUMER_NOT_ONLINE) {
                continue;
            }
            if (response.getCode() != ResponseCode.SUCCESS) {
                // Never expose a partial result as a successful full synchronization snapshot.
                return this.buildResult(response, new GetSubscriptionResult());
            }
            ConsumerConnection connection = this.decodeBody(response, ConsumerConnection.class);
            if (connection.getSubscriptionTable() == null) {
                throw new IllegalStateException("RocketMQ returned no consumer subscription table");
            }
            connection.getSubscriptionTable().forEach((topic, subscription) -> {
                if (topicName != null && !topicName.equals(topic)) {
                    return;
                }
                if (subscription == null) {
                    throw new IllegalStateException("RocketMQ returned an invalid subscription");
                }
                GroupMemberMetadata metadata = new GroupMemberMetadata();
                metadata.setGroupName(group);
                metadata.setTopicName(topic);
                subscriptions.add(metadata);
            });
        }
        subscriptions.sort(Comparator.comparing(GroupMemberMetadata::getGroupName).thenComparing(GroupMemberMetadata::getTopicName));
        GetSubscriptionResult result = new GetSubscriptionResult();
        result.setCode(200);
        result.setData(subscriptions);
        return result;
    }

    private <T> T decodeBody(RemotingCommand response, Class<T> bodyType) {
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no subscription response body");
        }
        T body = RemotingSerializable.decode(response.getBody(), bodyType);
        if (body == null) {
            throw new IllegalStateException("RocketMQ returned an invalid subscription response body");
        }
        return body;
    }
}
