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

package org.apache.eventmesh.dashboard.console.entity.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = "status")
public class ClusterEntity extends BaseEntity {

    private String name;

    private String registryAddress;

    private String bootstrapServers;

    private String eventmeshVersion;

    private String clientProperties;

    private String jmxProperties;

    private String regProperties;

    private String description;

    private Integer authType;

    private Integer runState;

    private Integer status;

    private Integer storeType;

    public ClusterEntity(ClusterMetadata source) {
        if (source.getClusterName() != null && !source.getClusterName().isEmpty()) {
            setAuthType(source.getAuthType());
            setBootstrapServers(source.getBootstrapServers());
            setClientProperties(source.getClientProperties());
            setRegistryAddress(source.getRegistryAddress());
            setEventmeshVersion(source.getEventmeshVersion());
            setJmxProperties(source.getJmxProperties());
            setRegProperties(source.getRegProperties());
            setDescription(source.getDescription());
            setAuthType(source.getAuthType());
            setRunState(source.getRunState());
            setStoreType(source.getStoreType().getNumber());
            setName(source.getClusterName());
        } else {
            throw new RuntimeException("cluster name is empty");
        }
    }
}
