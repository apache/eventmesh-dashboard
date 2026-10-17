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
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.SnapshotMetadataHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ClusterRelationshipSnapshotHandler implements SnapshotMetadataHandler<ClusterRelationshipMetadata> {

    private Set<Edge> applied = Set.of();

    @Override
    public synchronized void synchronize(List<ClusterRelationshipMetadata> snapshot, ColonyDO<ClusterDO> colony) {
        Set<Edge> desired = new HashSet<>();
        Set<Edge> removed = new HashSet<>(this.applied);
        Map<Long, Long> parents = new HashMap<>();
        for (ClusterRelationshipMetadata metadata : snapshot) {
            Edge edge = edge(metadata);
            if (active(metadata)) {
                validate(edge, colony);
                Long previous = parents.put(edge.child(), edge.parent());
                if (previous != null && !previous.equals(edge.parent())) {
                    throw new IllegalArgumentException("A cluster cannot have multiple parents");
                }
                desired.add(edge);
            } else {
                removed.add(edge);
            }
        }
        for (Long child : parents.keySet()) {
            Set<Long> visited = new HashSet<>();
            for (Long current = child; current != null; current = parents.get(current)) {
                if (!visited.add(current)) {
                    throw new IllegalArgumentException("Cluster relationships cannot form a cycle");
                }
            }
        }
        removed.removeAll(desired);
        removed.forEach(edge -> unlink(edge, colony));
        desired.forEach(edge -> colony.relationship(edge.parent(), edge.child()));
        this.applied = Set.copyOf(desired);
    }

    /** Also used by the existing initial topology loader. */
    public static void apply(ClusterRelationshipMetadata metadata, ColonyDO<ClusterDO> colony) {
        Edge edge = edge(metadata);
        if (active(metadata)) {
            validate(edge, colony);
            colony.relationship(edge.parent(), edge.child());
        } else {
            unlink(edge, colony);
        }
    }

    private static boolean active(ClusterRelationshipMetadata metadata) {
        return Objects.equals(metadata.getStatus(), 1L) && !Objects.equals(metadata.getIsDelete(), 1);
    }

    private static Edge edge(ClusterRelationshipMetadata metadata) {
        return new Edge(Objects.requireNonNull(metadata.getClusterId(), "Parent cluster ID"),
            Objects.requireNonNull(metadata.getRelationshipId(), "Child cluster ID"),
            Objects.requireNonNull(metadata.getClusterType(), "Parent cluster type"),
            Objects.requireNonNull(metadata.getRelationshipType(), "Child cluster type"));
    }

    private static void validate(Edge edge, ColonyDO<ClusterDO> colony) {
        ColonyDO<ClusterDO> parent = colony.getAllColonyDO().get(edge.parent());
        ColonyDO<ClusterDO> child = colony.getAllColonyDO().get(edge.child());
        if (parent == null || child == null) {
            throw new IllegalStateException("Relationship endpoints are not registered yet");
        }
        if (edge.parent().equals(edge.child()) || parent.getClusterType() != edge.parentType() || child.getClusterType() != edge.childType()) {
            throw new IllegalArgumentException("Relationship endpoint types do not match registered topology");
        }
    }

    private static void unlink(Edge edge, ColonyDO<ClusterDO> colony) {
        ColonyDO<ClusterDO> parent = colony.getAllColonyDO().get(edge.parent());
        ColonyDO<ClusterDO> child = colony.getAllColonyDO().get(edge.child());
        if (parent != null && child != null) {
            colony.unRelationship(edge.parent(), edge.child());
            if (Objects.equals(child.getSuperiorId(), edge.parent())) {
                child.setSuperiorId(null);
            }
        }
    }

    private record Edge(Long parent, Long child, ClusterType parentType, ClusterType childType) {
    }
}
