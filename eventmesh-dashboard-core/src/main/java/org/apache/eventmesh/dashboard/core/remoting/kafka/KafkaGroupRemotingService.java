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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.DeleteGroupRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.service.remoting.kafka.GroupRemotingService;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions;
import org.apache.kafka.clients.admin.MemberDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KafkaGroupRemotingService extends AbstractKafkaRemotingService implements GroupRemotingService {

    @Override
    public GetGroupResult getAllGroups(GetGroupsRequest request) throws Exception {
        var listed = this.awaitResult(this.getClient().listConsumerGroups(new ListConsumerGroupsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        if (listed == null) {
            throw new IllegalStateException("Kafka group list is missing");
        }
        List<String> names = listed.stream().map(ConsumerGroupListing::groupId).sorted().collect(Collectors.toList());
        List<GroupMetadata> groups = new ArrayList<>();
        if (!names.isEmpty()) {
            Map<String, ConsumerGroupDescription> descriptions = this.awaitResult(this.getClient()
                .describeConsumerGroups(names, new DescribeConsumerGroupsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
            for (String name : names) {
                ConsumerGroupDescription description = descriptions == null ? null : descriptions.get(name);
                if (description == null || !name.equals(description.groupId()) || description.members() == null) {
                    throw new IllegalStateException("Kafka group description is incomplete: " + name);
                }
                GroupMetadata group = new GroupMetadata();
                group.setName(name);
                group.setMemberCount(description.members().size());
                group.setMembers(description.members().stream().map(MemberDescription::consumerId).sorted().collect(Collectors.joining(",")));
                groups.add(group);
            }
        }
        GetGroupResult result = new GetGroupResult();
        result.setCode(200);
        result.setData(groups);
        return result;
    }

    @Override
    public BaseGlobalResult deleteGroup(DeleteGroupRequest request) throws Exception {
        String name = this.requireName(request == null || request.getMetaData() == null ? null : request.getMetaData().getName(), "groupName");
        this.awaitResult(this.getClient().deleteConsumerGroups(List.of(name), new DeleteConsumerGroupsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        return this.successfulResult();
    }
}
