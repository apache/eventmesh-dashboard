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

package org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class ClusterAndRuntimeOfRelationshipDO {

    private ClusterEntity clusterEntity;

    private List<ClusterEntity> clusterEntityList;

    private List<RuntimeEntity> runtimeEntityList;

    private List<ClusterRelationshipEntity> clusterRelationshipEntityList;

    private List<Triple<ClusterRelationshipEntity, ClusterEntity, ClusterEntity>> clusterRelationshipTripleList = new ArrayList<>();

    private List<Pair<ClusterEntity, List<ClusterEntity>>> clusterEntityPairleList = new ArrayList<>();

    private List<Pair<ClusterEntity, List<RuntimeEntity>>> runtimeEntityPairList = new ArrayList<>();

    private Map<MetadataType, Map<Long, List<ConfigEntity>>> configMap;

    private Map<MetadataType, Map<Long, List<TopicEntity>>> topicMap;

    private List<Pair<RuntimeEntity, ResourcesConfigEntity>> resourcesConfigEntityList;

    private List<Pair<ClusterEntity, List<ResourcesConfigEntity>>> clusterResourcesList;

    private List<Pair<ClusterEntity, Pair<RuntimeEntity, ResourcesConfigEntity>>> resourceData;

    private Map<ClusterType, List<RuntimeEntity>> clusterTypeMapMap;

    public Map<Long, List<RuntimeEntity>> getRuntimeEntityByClusterType(ClusterType clusterType) {
        if (Objects.isNull(clusterTypeMapMap)) {
            this.clusterTypeMapMap = this.runtimeEntityList.stream().collect(Collectors.groupingBy(RuntimeEntity::getClusterType));

        }
        List<RuntimeEntity> list = this.clusterTypeMapMap.get(clusterType);
        if (Objects.isNull(list)) {
            return null;
        }
        return list.stream().collect(Collectors.groupingBy(RuntimeEntity::getClusterId));
    }
}
