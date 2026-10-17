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


package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.SnapshotMetadataHandler;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.kafka.AclRemotingService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

/** Owns only database bindings and bindings successfully created during this process lifetime. */
@Component
public class KafkaAclSnapshotHandler implements SnapshotMetadataHandler<AclMetadata> {

    private final Function<ClusterMetadata, AclRemotingService> services;

    private final Map<Long, Map<Binding, AclMetadata>> owned = new HashMap<>();

    public KafkaAclSnapshotHandler() {
        this(metadata -> Remoting2Manage.getInstance().createRemotingService(AclRemotingService.class, metadata));
    }

    public KafkaAclSnapshotHandler(Function<ClusterMetadata, AclRemotingService> services) {
        this.services = Objects.requireNonNull(services);
    }

    @Override
    public synchronized void synchronize(List<AclMetadata> snapshot, ColonyDO<ClusterDO> colony) {
        Map<Long, Map<Binding, AclMetadata>> desired = new LinkedHashMap<>();
        Map<Long, Map<Binding, AclMetadata>> deleted = new LinkedHashMap<>();
        for (AclMetadata metadata : snapshot) {
            Long clusterId = Objects.requireNonNull(metadata.getClusterId(), "ACL cluster ID");
            ColonyDO<ClusterDO> target = colony.getAllColonyDO().get(clusterId);
            if (target == null) {
                throw new IllegalStateException("ACL cluster is not registered yet: " + clusterId);
            }
            if (!kafka(target.getClusterType())) {
                continue;
            }
            Binding binding = binding(metadata);
            Map<Long, Map<Binding, AclMetadata>> destination = Objects.equals(metadata.getStatus(), 1L)
                && !Objects.equals(metadata.getIsDelete(), 1) ? desired : deleted;
            destination.computeIfAbsent(clusterId, ignored -> new LinkedHashMap<>()).put(binding, metadata);
        }
        Set<Long> clusters = new HashSet<>(this.owned.keySet());
        clusters.addAll(desired.keySet());
        clusters.addAll(deleted.keySet());
        for (Long clusterId : clusters) {
            ColonyDO<ClusterDO> target = colony.getAllColonyDO().get(clusterId);
            if (target == null) {
                throw new IllegalStateException("ACL cluster is not registered yet: " + clusterId);
            }
            if (kafka(target.getClusterType())) {
                reconcile(target, desired.getOrDefault(clusterId, Map.of()),
                    deleted.getOrDefault(clusterId, Map.of()));
            }
        }
    }

    private void reconcile(ColonyDO<ClusterDO> target, Map<Binding, AclMetadata> desired, Map<Binding, AclMetadata> deleted) {
        ClusterMetadata cluster = target.getClusterDO().getClusterInfo();
        try {
            AclRemotingService service = this.services.apply(cluster);
            GetAclsResult result = service.getAllAcls(new GetAcls2Request());
            success(result);
            Set<Binding> remote = new HashSet<>();
            for (AclMetadata metadata : Objects.requireNonNull(result.getData(), "Kafka ACL query data")) {
                remote.add(binding(metadata));
            }
            Map<Binding, AclMetadata> managed = this.owned.computeIfAbsent(cluster.getId(), ignored -> new HashMap<>());
            for (Map.Entry<Binding, AclMetadata> entry : desired.entrySet()) {
                if (!remote.contains(entry.getKey())) {
                    CreateAclRequest request = new CreateAclRequest();
                    request.setMetaData(entry.getKey().metadata());
                    success(service.createAcl(request));
                }
                // Record each acknowledgement, including successful operations before a later failure.
                managed.put(entry.getKey(), entry.getValue());
            }
            Map<Binding, AclMetadata> obsolete = new HashMap<>(managed);
            obsolete.putAll(deleted);
            obsolete.keySet().removeAll(desired.keySet());
            for (Map.Entry<Binding, AclMetadata> entry : obsolete.entrySet()) {
                if (remote.contains(entry.getKey())) {
                    DeleteAclRequest request = new DeleteAclRequest();
                    request.setMetaData(entry.getKey().metadata());
                    success(service.deleteAcl(request));
                }
                managed.remove(entry.getKey());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka ACL synchronization failed for cluster " + cluster.getId(), exception);
        }
    }

    private static void success(GlobalResult<?> result) {
        if (result == null || !Objects.equals(result.getCode(), 200)) {
            throw new IllegalStateException("Kafka ACL operation did not succeed", result == null ? null : result.getThrowable());
        }
    }

    private static boolean kafka(ClusterType type) {
        return type == ClusterType.STORAGE_KAFKA_BROKER || type == ClusterType.STORAGE_KAFKA_RAFT;
    }

    private static Binding binding(AclMetadata metadata) {
        return new Binding(metadata.getPrincipal(), metadata.getHost(), metadata.getOperation(), metadata.getPermissionType(),
            metadata.getResourceType(), metadata.getResourceName(), metadata.getPatternType());
    }

    private record Binding(String principal, String host, Integer operation, String permission,
        String resourceType, String resourceName, Integer patternType) {

        private AclMetadata metadata() {
            AclMetadata metadata = new AclMetadata();
            metadata.setPrincipal(this.principal);
            metadata.setHost(this.host);
            metadata.setOperation(this.operation);
            metadata.setPermissionType(this.permission);
            metadata.setResourceType(this.resourceType);
            metadata.setResourceName(this.resourceName);
            metadata.setPatternType(this.patternType);
            return metadata;
        }
    }
}
