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

import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.service.remoting.kafka.AclRemotingService;

import org.apache.kafka.clients.admin.CreateAclsOptions;
import org.apache.kafka.clients.admin.DeleteAclsOptions;
import org.apache.kafka.clients.admin.DescribeAclsOptions;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Basic Kafka ACL operations; SDK request construction and transport remain in the official client. */
public class KafkaAclRemotingService extends AbstractKafkaRemotingService implements AclRemotingService {

    @Override
    public CreateAclResult createAcl(CreateAclRequest request) throws Exception {
        AclBinding binding = this.binding(request == null ? null : request.getMetaData());
        this.awaitResult(this.getClient().createAcls(List.of(binding), new CreateAclsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        CreateAclResult result = new CreateAclResult();
        result.setCode(200);
        return result;
    }

    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest request) throws Exception {
        if (request != null && request.isDeleteAccount()) {
            throw new IllegalArgumentException("Kafka ACL deletion targets a binding, not an account");
        }
        // A concrete binding's filter preserves literal '*' values and cannot broaden to ANY/MATCH.
        AclBindingFilter filter = this.binding(request == null ? null : request.getMetaData()).toFilter();
        this.awaitResult(this.getClient().deleteAcls(List.of(filter), new DeleteAclsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        DeleteAclResult result = new DeleteAclResult();
        result.setCode(200);
        return result;
    }

    @Override
    public GetAclsResult getAllAcls(GetAcls2Request request) throws Exception {
        AclBindingFilter filter = this.filter(request == null ? null : request.getMetaData());
        Collection<AclBinding> bindings = this.awaitResult(this.getClient()
            .describeAcls(filter, new DescribeAclsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).values());
        if (bindings == null) {
            throw new IllegalStateException("Kafka ACL response is missing");
        }
        List<AclMetadata> entries = new ArrayList<>();
        for (AclBinding binding : bindings) {
            AclMetadata entry = new AclMetadata();
            entry.setResourceType(binding.pattern().resourceType().name());
            entry.setResourceName(binding.pattern().name());
            entry.setPatternType((int) binding.pattern().patternType().code());
            entry.setPrincipal(binding.entry().principal());
            entry.setHost(binding.entry().host());
            entry.setOperation((int) binding.entry().operation().code());
            entry.setPermissionType(binding.entry().permissionType().name());
            entries.add(entry);
        }
        entries.sort(Comparator.comparing(AclMetadata::getResourceType)
            .thenComparing(AclMetadata::getResourceName)
            .thenComparing(AclMetadata::getPatternType)
            .thenComparing(AclMetadata::getPrincipal)
            .thenComparing(AclMetadata::getHost)
            .thenComparing(AclMetadata::getOperation)
            .thenComparing(AclMetadata::getPermissionType));
        GetAclsResult result = new GetAclsResult();
        result.setCode(200);
        result.setData(entries);
        return result;
    }

    private AclBinding binding(AclMetadata metadata) {
        if (metadata == null || metadata.getResourceType() == null || metadata.getPatternType() == null
            || metadata.getOperation() == null || metadata.getPermissionType() == null) {
            throw new IllegalArgumentException("All seven ACL binding fields are required");
        }
        this.requireName(metadata.getResourceName(), "resourceName");
        String principal = this.requireName(metadata.getPrincipal(), "principal");
        this.requireName(metadata.getHost(), "host");
        if (principal.indexOf(':') <= 0 || principal.endsWith(":")) {
            throw new IllegalArgumentException("principal must have type:name format, for example User:alice");
        }
        AclBindingFilter filter = this.filter(metadata);
        if (filter.patternFilter().resourceType() == ResourceType.ANY
            || !filter.patternFilter().patternType().isSpecific()
            || filter.entryFilter().operation() == AclOperation.ANY
            || filter.entryFilter().permissionType() == AclPermissionType.ANY) {
            throw new IllegalArgumentException("Create/delete requires concrete ACL values; ANY and MATCH are query-only");
        }
        return new AclBinding(new ResourcePattern(filter.patternFilter().resourceType(), metadata.getResourceName(),
            filter.patternFilter().patternType()), new AccessControlEntry(principal, metadata.getHost(),
            filter.entryFilter().operation(), filter.entryFilter().permissionType()));
    }

    private AclBindingFilter filter(AclMetadata metadata) {
        if (metadata == null) {
            return AclBindingFilter.ANY;
        }
        if (metadata.getPolicyType() != null || metadata.getActions() != null || metadata.getSourceIps() != null) {
            throw new IllegalArgumentException("RocketMQ policy fields are not supported by Kafka ACLs");
        }
        ResourceType resource = metadata.getResourceType() == null ? ResourceType.ANY : ResourceType.fromString(metadata.getResourceType());
        PatternType pattern = metadata.getPatternType() == null ? PatternType.ANY
            : PatternType.fromCode(this.code(metadata.getPatternType(), "patternType"));
        AclOperation operation = metadata.getOperation() == null ? AclOperation.ANY
            : AclOperation.fromCode(this.code(metadata.getOperation(), "operation"));
        AclPermissionType permission = metadata.getPermissionType() == null ? AclPermissionType.ANY
            : AclPermissionType.fromString(metadata.getPermissionType());
        if (resource == ResourceType.UNKNOWN || pattern == PatternType.UNKNOWN || operation == AclOperation.UNKNOWN
            || permission == AclPermissionType.UNKNOWN) {
            throw new IllegalArgumentException("Unknown Kafka ACL enum value");
        }
        if (metadata.getResourceName() != null) {
            this.requireName(metadata.getResourceName(), "resourceName");
        }
        if (metadata.getPrincipal() != null) {
            this.requireName(metadata.getPrincipal(), "principal");
        }
        if (metadata.getHost() != null) {
            this.requireName(metadata.getHost(), "host");
        }
        return new AclBindingFilter(new ResourcePatternFilter(resource, metadata.getResourceName(), pattern),
            new AccessControlEntryFilter(metadata.getPrincipal(), metadata.getHost(), operation, permission));
    }

    private byte code(int value, String field) {
        if (value < 0 || value > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid Kafka ACL " + field + " code");
        }
        return (byte) value;
    }
}
