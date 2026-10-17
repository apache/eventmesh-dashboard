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

import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConnectionConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.SnapshotMetadataHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

/** Console's persisted connection configuration; this does not deploy connectors. */
@Component
public class ConnectionSnapshotHandler implements SnapshotMetadataHandler<ConnectionMetadata> {

    private volatile Map<Long, ConnectionMetadata> connections = Map.of();

    @Override
    public synchronized void synchronize(List<ConnectionMetadata> snapshot, ColonyDO<ClusterDO> colony) {
        Map<Long, ConnectionMetadata> next = new LinkedHashMap<>();
        for (ConnectionMetadata metadata : snapshot) {
            if (Objects.equals(metadata.getStatus(), 1L) && !Objects.equals(metadata.getIsDelete(), 1)
                && Objects.isNull(metadata.getEndTime())) {
                Objects.requireNonNull(metadata.getId(), "Connection ID");
                Objects.requireNonNull(metadata.getClusterId(), "Connection cluster ID");
                Objects.requireNonNull(metadata.getRuntimeId(), "Connection runtime ID");
                if (next.put(metadata.getId(), copy(metadata)) != null) {
                    throw new IllegalArgumentException("Duplicate connection ID");
                }
            }
        }
        this.connections = Map.copyOf(next);
    }

    public List<ConnectionMetadata> list() {
        return this.list(null, null);
    }

    /** Null scope arguments mean all clusters or runtimes. Returned objects are defensive copies. */
    public List<ConnectionMetadata> list(Long clusterId, Long runtimeId) {
        List<ConnectionMetadata> result = new ArrayList<>();
        this.connections.values().forEach(metadata -> {
            if ((Objects.isNull(clusterId) || Objects.equals(clusterId, metadata.getClusterId()))
                && (Objects.isNull(runtimeId) || Objects.equals(runtimeId, metadata.getRuntimeId()))) {
                result.add(copy(metadata));
            }
        });
        return result;
    }

    private static ConnectionMetadata copy(ConnectionMetadata metadata) {
        return ConnectionConvertMetaData.INSTANCE.toMetaData(ConnectionConvertMetaData.INSTANCE.toEntity(metadata));
    }
}
