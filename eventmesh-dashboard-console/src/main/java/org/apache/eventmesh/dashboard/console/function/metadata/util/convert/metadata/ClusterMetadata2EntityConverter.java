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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

public class ClusterMetadata2EntityConverter implements Converter<ClusterMetadata, ClusterEntity> {

    @Override
    public ClusterEntity convert(ClusterMetadata source) {
        if (source.getClusterName() != null && !source.getClusterName().isEmpty()) {
            return ClusterEntity.builder()
                .status(1)
                .authType(source.getAuthType())
                .bootstrapServers(source.getBootstrapServers())
                .clientProperties(source.getClientProperties())
                .registryAddress(source.getRegistryAddress())
                .eventmeshVersion(source.getEventmeshVersion())
                .jmxProperties(source.getJmxProperties())
                .regProperties(source.getRegProperties())
                .description(source.getDescription())
                .authType(source.getAuthType())
                .runState(source.getRunState())
                .storeType(source.getStoreType().getNumber())
                .name(source.getClusterName())
                .build();
        } else {
            throw new RuntimeException("cluster name is empty");
        }
    }

    @Override
    public String getUnique(ClusterMetadata source) {
        return source.getClusterName();
    }

}
