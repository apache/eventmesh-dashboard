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


package org.apache.eventmesh.dashboard.console.mapstruct.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterTreeDO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateClusterDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.BatchCreateClusterDataDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryClusterByOrganizationIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryRelationClusterByClusterIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryTreeByClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.SimpleCreateClusterDataDTO;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 */
@Mapper
public interface ClusterControllerMapper {

    ClusterControllerMapper INSTANCE = Mappers.getMapper(ClusterControllerMapper.class);


    QueryClusterTreeDO queryTreeByClusterId(QueryTreeByClusterIdDTO data);

    ClusterEntity queryClusterByOrganizationIdAndType(QueryClusterByOrganizationIdAndTypeDTO dto);

    ClusterEntity queryRelationClusterByClusterIdAndType(QueryRelationClusterByClusterIdAndTypeDTO dto);

    ClusterEntity createCluster(CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO);

    ClusterEntity toClusterEntity(ClusterIdDTO clusterIdDTO);

    ClusterEntity toClusterEntity(SimpleCreateClusterDataDTO data);

    ClusterEntity toClusterEntity(BatchCreateClusterDataDTO data);

    ClusterEntity createClusterDTO(CreateClusterDTO createClusterDTO);

    List<ClusterEntity> simpleCreateClusterDataToClusterEntity(List<SimpleCreateClusterDataDTO> data);

}
