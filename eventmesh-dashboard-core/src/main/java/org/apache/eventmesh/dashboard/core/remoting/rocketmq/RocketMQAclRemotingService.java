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
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.action.Action;
import org.apache.rocketmq.common.resource.ResourceType;
import org.apache.rocketmq.common.utils.IPAddressUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.AclInfo;
import org.apache.rocketmq.remoting.protocol.header.CreateAclRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteAclRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ListAclsRequestHeader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/** RocketMQ ACL operations using the registered target's V1 or V2 protocol configuration. */
public class RocketMQAclRemotingService extends AbstractRocketMQRemotingService implements AclRemotingService {

    // RequestCode in the ACL 1.0 Broker protocol (4.9.x / 5.2.x).
    private static final int UPDATE_AND_CREATE_ACL_CONFIG = 50;

    private static final int DELETE_ACL_CONFIG = 51;

    private static final int GET_BROKER_CLUSTER_ACL_CONFIG = 54;

    private CreateRemotingConfig.AclVersion aclVersion() {
        // Preserve ACL 2.0 behavior for existing client registrations without an explicit setting.
        if (this.getCreateSdkConfig() == null) {
            return CreateRemotingConfig.AclVersion.V2;
        }
        if (!(this.getCreateSdkConfig() instanceof CreateRemotingConfig)) {
            throw new IllegalStateException("RocketMQ ACL requires CreateRemotingConfig");
        }
        CreateRemotingConfig config = (CreateRemotingConfig) this.getCreateSdkConfig();
        if (config.getAclVersion() == null) {
            throw new IllegalStateException("RocketMQ aclVersion must be V1 or V2");
        }
        switch (config.getAclVersion()) {
            case V1:
                return CreateRemotingConfig.AclVersion.V1;
            case V2:
                return CreateRemotingConfig.AclVersion.V2;
            default:
                throw new IllegalStateException("Unsupported RocketMQ ACL version");
        }
    }

    /** Upsert a complete V1 account configuration or one V2 resource policy. */
    @Override
    public CreateAclResult createAcl(CreateAclRequest request) throws Exception {
        if (this.aclVersion() == CreateRemotingConfig.AclVersion.V1) {
            return this.createAclV1(request);
        }
        AclMetadata metadata = request == null ? null : request.getMetaData();
        this.validateIdentity(metadata);
        String resource = this.resourceKey(metadata);
        List<String> actions = this.actions(metadata.getActions());
        String decision = this.decision(metadata.getPermissionType());
        if (metadata.getSourceIps() == null) {
            throw new IllegalArgumentException("sourceIps is required; use an empty list explicitly for unrestricted sources");
        }
        for (String sourceIp : metadata.getSourceIps()) {
            if (StringUtils.isBlank(sourceIp) || !IPAddressUtils.isValidIPOrCidr(sourceIp)) {
                throw new IllegalArgumentException("Source addresses must be valid IP addresses or CIDRs");
            }
        }
        AclInfo acl = AclInfo.of(metadata.getPrincipal(), List.of(resource), actions, new ArrayList<>(metadata.getSourceIps()), decision);
        acl.getPolicies().get(0).setPolicyType(this.policyType(metadata.getPolicyType()));
        // AUTH_CREATE_ACL is an upsert in the Broker metadata manager, matching framework ADD/UPDATE.
        RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.AUTH_CREATE_ACL,
            new CreateAclRequestHeader(metadata.getPrincipal()));
        command.setBody(RemotingSerializable.encode(acl));
        return this.buildResult(this.invokeSync(command), new CreateAclResult());
    }

    /** Delete an explicitly selected V1 account or exactly one V2 resource policy. */
    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest request) throws Exception {
        if (this.aclVersion() == CreateRemotingConfig.AclVersion.V1) {
            return this.deleteAclV1(request);
        }
        if (request != null && request.isDeleteAccount()) {
            throw new IllegalArgumentException("ACL 2.0 deletes resource policies, not accounts");
        }
        AclMetadata metadata = request == null ? null : request.getMetaData();
        this.validateIdentity(metadata);
        DeleteAclRequestHeader header = new DeleteAclRequestHeader(metadata.getPrincipal(), this.resourceKey(metadata));
        header.setPolicyType(this.policyType(metadata.getPolicyType()));
        return this.invokeSync(RequestCode.AUTH_DELETE_ACL, header, new DeleteAclResult());
    }

    @Override
    public GetAclsResult getAllAcls(GetAcls2Request request) throws Exception {
        if (this.aclVersion() == CreateRemotingConfig.AclVersion.V1) {
            return this.getAllAclsV1(request);
        }
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.AUTH_LIST_ACL,
            new ListAclsRequestHeader(null, null)));
        GetAclsResult result = this.buildResult(response, new GetAclsResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        if (response.getBody() == null || response.getBody().length == 0) {
            // Broker listAcl omits the body when its ACL list is empty.
            result.setData(new ArrayList<>());
            return result;
        }
        List<AclInfo> acls = RemotingSerializable.decodeList(response.getBody(), AclInfo.class);
        if (acls == null) {
            throw new IllegalStateException("RocketMQ returned an invalid ACL list");
        }
        List<AclMetadata> entries = new ArrayList<>();
        for (AclInfo acl : acls) {
            if (acl == null || acl.getPolicies() == null) {
                throw new IllegalStateException("RocketMQ returned an invalid ACL policy list");
            }
            for (AclInfo.PolicyInfo policy : acl.getPolicies()) {
                if (policy == null || policy.getEntries() == null) {
                    throw new IllegalStateException("RocketMQ returned invalid ACL policy entries");
                }
                for (AclInfo.PolicyEntryInfo entry : policy.getEntries()) {
                    entries.add(this.toMetadata(acl.getSubject(), policy.getPolicyType(), entry));
                }
            }
        }
        entries.sort(Comparator.comparing(AclMetadata::nodeUnique));
        result.setData(entries);
        return result;
    }

    private AclMetadata toMetadata(String subject, String policy, AclInfo.PolicyEntryInfo entry) {
        if (entry == null || StringUtils.isBlank(entry.getResource())) {
            throw new IllegalStateException("RocketMQ returned an invalid ACL entry");
        }
        AclMetadata metadata = new AclMetadata();
        metadata.setPrincipal(subject);
        metadata.setPolicyType(this.policyType(policy));
        metadata.setActions(this.actions(entry.getActions()));
        metadata.setPermissionType(this.decision(entry.getDecision()));
        metadata.setSourceIps(entry.getSourceIps() == null ? new ArrayList<>() : new ArrayList<>(entry.getSourceIps()));
        String resource = entry.getResource();
        if ("*".equals(resource)) {
            metadata.setResourceType("Any");
            metadata.setResourceName("*");
        } else {
            int separator = resource.indexOf(':');
            if (separator <= 0) {
                throw new IllegalStateException("RocketMQ returned an invalid ACL resource");
            }
            metadata.setResourceType(resource.substring(0, separator));
            metadata.setResourceName(resource.substring(separator + 1));
        }
        this.validateIdentity(metadata);
        this.resourceKey(metadata);
        metadata.setResourceType(ResourceType.getByName(metadata.getResourceType()).getName());
        return metadata;
    }

    private void validateIdentity(AclMetadata metadata) {
        if (metadata instanceof RocketMQAclAccountMetadata) {
            throw new IllegalArgumentException("ACL 1.0 account metadata cannot be sent to ACL 2.0");
        }
        if (metadata == null || metadata.getPrincipal() == null || !metadata.getPrincipal().startsWith("User:")
            || StringUtils.isBlank(metadata.getPrincipal().substring(5))) {
            throw new IllegalArgumentException("ACL principal must be User:<existing username>");
        }
        if (metadata.getOperation() != null || metadata.getPatternType() != null || metadata.getHost() != null) {
            throw new IllegalArgumentException("RocketMQ ACL uses actions, sourceIps and a resourceName with optional trailing wildcard");
        }
    }

    private String resourceKey(AclMetadata metadata) {
        ResourceType type = ResourceType.getByName(metadata.getResourceType());
        String name = metadata.getResourceName();
        if (type == null || type == ResourceType.UNKNOWN || StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("ACL resource type and name are required");
        }
        if (type == ResourceType.ANY) {
            if (!"*".equals(name)) {
                throw new IllegalArgumentException("Any resource type requires resourceName=*");
            }
            return "*";
        }
        if (!name.equals(name.trim()) || name.indexOf('*') >= 0 && name.indexOf('*') != name.length() - 1) {
            throw new IllegalArgumentException("Only a single trailing resource wildcard is supported");
        }
        return type.getName() + ":" + name;
    }

    private String policyType(String policy) {
        if (policy == null || "Custom".equalsIgnoreCase(policy)) {
            return "Custom";
        }
        if ("Default".equalsIgnoreCase(policy)) {
            return "Default";
        }
        throw new IllegalArgumentException("ACL policy type must be Custom or Default");
    }

    private String decision(String permission) {
        if ("Allow".equalsIgnoreCase(permission)) {
            return "Allow";
        }
        if ("Deny".equalsIgnoreCase(permission)) {
            return "Deny";
        }
        throw new IllegalArgumentException("ACL permissionType must be Allow or Deny");
    }

    private List<String> actions(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("ACL actions must not be empty");
        }
        List<String> actions = new ArrayList<>();
        for (String value : values) {
            Action action = Action.getByName(value);
            if (action == null || action == Action.UNKNOWN || action == Action.ANY) {
                throw new IllegalArgumentException("Invalid RocketMQ ACL action");
            }
            if (!actions.contains(action.getName())) {
                actions.add(action.getName());
            }
        }
        return actions;
    }

    private CreateAclResult createAclV1(CreateAclRequest request) throws Exception {
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
        return this.buildResult(this.invokeSync(command), new CreateAclResult());
    }

    private DeleteAclResult deleteAclV1(DeleteAclRequest request) throws Exception {
        RocketMQAclAccountMetadata account = this.account(request == null ? null : request.getMetaData());
        if (!request.isDeleteAccount()) {
            throw new IllegalArgumentException("ACL 1.0 deletion removes an entire account; explicitly set deleteAccount=true");
        }
        RemotingCommand command = RemotingCommand.createRequestCommand(DELETE_ACL_CONFIG, null);
        command.addExtField("accessKey", account.getAccessKey());
        return this.buildResult(this.invokeSync(command), new DeleteAclResult());
    }

    private GetAclsResult getAllAclsV1(GetAcls2Request request) throws Exception {
        // Supported by 4.9.4, but removed in 4.9.8/5.2.0. Never substitute code 52 (version information).
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(GET_BROKER_CLUSTER_ACL_CONFIG, null));
        if (response.getCode() == ResponseCode.REQUEST_CODE_NOT_SUPPORTED) {
            throw new UnsupportedOperationException(
                "This ACL 1.0 Broker does not expose account-list RPC; automatic ACL synchronization is unsupported");
        }
        GetAclsResult result = this.buildResult(response, new GetAclsResult());
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
