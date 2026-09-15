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
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/** Legacy wire protocol, isolated from SDK classes removed in RocketMQ 5.3.3. */
final class RocketMQAclV1ProtocolHandler implements RocketMQAclProtocolHandler {

    // RequestCode in the ACL 1.0 Broker protocol (4.9.x / 5.2.x).
    private static final int UPDATE_AND_CREATE_ACL_CONFIG = 50;

    private static final int DELETE_ACL_CONFIG = 51;

    private static final int GET_BROKER_CLUSTER_ACL_CONFIG = 54;

    private final RocketMQAclRemotingService service;

    RocketMQAclV1ProtocolHandler(RocketMQAclRemotingService service) {
        this.service = service;
    }

    @Override
    public CreateAclResult createAcl(CreateAclRequest request) throws Exception {
        RocketMQAclAccountMetadata account = this.account(request == null ? null : request.getMetaData());
        // Native upsert has mixed patch/replacement semantics. Require complete input to prevent accidental resets.
        if (StringUtils.isBlank(account.getSecretKey()) || account.getSecretKey().length() <= 6) {
            throw new IllegalArgumentException("ACL 1.0 secretKey is required and must be longer than 6 characters");
        }
        if (account.getAdmin() == null || account.getWhiteRemoteAddress() == null) {
            throw new IllegalArgumentException("ACL 1.0 admin and whiteRemoteAddress must be explicitly supplied");
        }
        this.permission(account.getDefaultTopicPerm());
        this.permission(account.getDefaultGroupPerm());
        String topics = this.rules(account.getTopicPerms());
        String groups = this.rules(account.getGroupPerms());
        RemotingCommand command = RemotingCommand.createRequestCommand(UPDATE_AND_CREATE_ACL_CONFIG, null);
        command.addExtField("accessKey", account.getAccessKey());
        command.addExtField("secretKey", account.getSecretKey());
        command.addExtField("admin", account.getAdmin().toString());
        command.addExtField("whiteRemoteAddress", account.getWhiteRemoteAddress());
        command.addExtField("defaultTopicPerm", account.getDefaultTopicPerm());
        command.addExtField("defaultGroupPerm", account.getDefaultGroupPerm());
        command.addExtField("topicPerms", topics);
        command.addExtField("groupPerms", groups);
        return this.service.buildResult(this.service.invokeSync(command), new CreateAclResult());
    }

    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest request) throws Exception {
        RocketMQAclAccountMetadata account = this.account(request == null ? null : request.getMetaData());
        if (!request.isDeleteAccount()) {
            throw new IllegalArgumentException("ACL 1.0 deletion removes an entire account; explicitly set deleteAccount=true");
        }
        RemotingCommand command = RemotingCommand.createRequestCommand(DELETE_ACL_CONFIG, null);
        command.addExtField("accessKey", account.getAccessKey());
        return this.service.buildResult(this.service.invokeSync(command), new DeleteAclResult());
    }

    @Override
    public GetAclsResult getAllAcls(GetAcls2Request request) throws Exception {
        // Supported by 4.9.4, but removed in 4.9.8/5.2.0. Never substitute code 52 (version information).
        RemotingCommand response = this.service.invokeSync(RemotingCommand.createRequestCommand(GET_BROKER_CLUSTER_ACL_CONFIG, null));
        if (response.getCode() == ResponseCode.REQUEST_CODE_NOT_SUPPORTED) {
            throw new UnsupportedOperationException(
                "This ACL 1.0 Broker does not expose account-list RPC; automatic ACL synchronization is unsupported");
        }
        GetAclsResult result = this.service.buildResult(response, new GetAclsResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned a missing ACL 1.0 configuration body");
        }
        JSONObject body = JSON.parseObject(response.getBody(), JSONObject.class);
        if (body == null || !(body.get("plainAccessConfigs") instanceof JSONArray)) {
            throw new IllegalStateException("RocketMQ returned an invalid ACL 1.0 account list");
        }
        List<AclMetadata> accounts = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (Object value : body.getJSONArray("plainAccessConfigs")) {
            if (!(value instanceof JSONObject)) {
                throw new IllegalStateException("RocketMQ returned an invalid ACL 1.0 account");
            }
            JSONObject item = (JSONObject) value;
            RocketMQAclAccountMetadata account = new RocketMQAclAccountMetadata();
            account.setAccessKey(item.getString("accessKey"));
            this.account(account);
            if (!identities.add(account.getAccessKey())) {
                throw new IllegalStateException("RocketMQ returned duplicate ACL 1.0 accounts");
            }
            // The legacy Broker response includes credentials. Never expose secretKey in query results.
            account.setAdmin(item.getBoolean("admin"));
            account.setWhiteRemoteAddress(item.getString("whiteRemoteAddress"));
            account.setDefaultTopicPerm(item.getString("defaultTopicPerm"));
            account.setDefaultGroupPerm(item.getString("defaultGroupPerm"));
            account.setTopicPerms(this.readRules(item, "topicPerms"));
            account.setGroupPerms(this.readRules(item, "groupPerms"));
            accounts.add(account);
        }
        accounts.sort(Comparator.comparing(AclMetadata::nodeUnique));
        result.setData(accounts);
        return result;
    }

    private List<String> readRules(JSONObject item, String name) {
        Object value = item.get(name);
        if (value == null) {
            return new ArrayList<>();
        }
        if (!(value instanceof JSONArray)) {
            throw new IllegalStateException("RocketMQ returned invalid ACL 1.0 resource rules");
        }
        List<String> rules = new ArrayList<>();
        for (Object rule : (JSONArray) value) {
            if (!(rule instanceof String)) {
                throw new IllegalStateException("RocketMQ returned an invalid ACL 1.0 resource rule");
            }
            rules.add((String) rule);
        }
        this.rules(rules);
        return rules;
    }

    private RocketMQAclAccountMetadata account(AclMetadata metadata) {
        if (!(metadata instanceof RocketMQAclAccountMetadata)) {
            throw new IllegalArgumentException("ACL 1.0 requires RocketMQAclAccountMetadata");
        }
        RocketMQAclAccountMetadata account = (RocketMQAclAccountMetadata) metadata;
        if (StringUtils.isBlank(account.getAccessKey()) || account.getAccessKey().length() <= 6) {
            throw new IllegalArgumentException("ACL 1.0 accessKey is required and must be longer than 6 characters");
        }
        if (account.getPrincipal() != null || account.getResourceType() != null || account.getResourceName() != null
            || account.getPolicyType() != null || account.getActions() != null || account.getSourceIps() != null
            || account.getPermissionType() != null || account.getHost() != null || account.getOperation() != null
            || account.getPatternType() != null) {
            throw new IllegalArgumentException("ACL 1.0 account configuration cannot contain resource-policy fields");
        }
        return account;
    }

    private void permission(String value) {
        if (!Set.of("DENY", "PUB", "SUB", "PUB|SUB", "SUB|PUB").contains(value == null ? "" : value)) {
            throw new IllegalArgumentException("ACL 1.0 permission must be DENY, PUB, SUB or PUB|SUB");
        }
    }

    private String rules(List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("ACL 1.0 requires complete topicPerms and groupPerms lists");
        }
        Set<String> names = new HashSet<>();
        for (String value : values) {
            if (value == null || value.contains(",")) {
                throw new IllegalArgumentException("ACL 1.0 rule must be resource=permission without commas");
            }
            String[] parts = value.split("=", -1);
            if (parts.length != 2 || StringUtils.isBlank(parts[0]) || !parts[0].equals(parts[0].trim()) || !names.add(parts[0])) {
                throw new IllegalArgumentException("ACL 1.0 rules require distinct resource names and one permission each");
            }
            this.permission(parts[1]);
        }
        return String.join(",", values);
    }
}
