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
import java.util.List;

/** RocketMQ ACL 2.0 resource policies. Authentication/user provisioning belongs to a separate API. */
final class RocketMQAclV2ProtocolHandler implements RocketMQAclProtocolHandler {

    private final RocketMQAclRemotingService service;

    RocketMQAclV2ProtocolHandler(RocketMQAclRemotingService service) {
        this.service = service;
    }

    /** Complete replacement of one resource entry; the Broker preserves other resource entries. */
    @Override
    public CreateAclResult createAcl(CreateAclRequest request) throws Exception {
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
        return this.service.buildResult(this.service.invokeSync(command), new CreateAclResult());
    }

    /** Delete exactly one resource policy. Missing resource must never turn into deletion of all subject ACLs. */
    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest request) throws Exception {
        if (request != null && request.isDeleteAccount()) {
            throw new IllegalArgumentException("ACL 2.0 deletes resource policies, not accounts");
        }
        AclMetadata metadata = request == null ? null : request.getMetaData();
        this.validateIdentity(metadata);
        DeleteAclRequestHeader header = new DeleteAclRequestHeader(metadata.getPrincipal(), this.resourceKey(metadata));
        header.setPolicyType(this.policyType(metadata.getPolicyType()));
        return this.service.invokeSync(RequestCode.AUTH_DELETE_ACL, header, new DeleteAclResult());
    }

    @Override
    public GetAclsResult getAllAcls(GetAcls2Request request) throws Exception {
        RemotingCommand response = this.service.invokeSync(RemotingCommand.createRequestCommand(RequestCode.AUTH_LIST_ACL,
            new ListAclsRequestHeader(null, null)));
        GetAclsResult result = this.service.buildResult(response, new GetAclsResult());
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
}
