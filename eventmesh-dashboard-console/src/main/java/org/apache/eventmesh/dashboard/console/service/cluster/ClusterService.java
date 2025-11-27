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


package org.apache.eventmesh.dashboard.console.service.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.QO.cluster.QueryRelationClusterByClusterIdListAndType;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.GetClusterBaseMessageVO;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * cluster data service
 */
public interface ClusterService {


    boolean nameExist(ClusterEntity clusterEntity);

    ClusterEntity queryClusterById(ClusterEntity clusterEntity);

    List<ClusterEntity> queryClusterListByClusterList(List<ClusterEntity> clusterEntityList);

    List<ClusterEntity> queryClusterByOrganizationIdAndType(ClusterEntity clusterEntity);


    List<ClusterEntity> queryRelationClusterByClusterIdAndType(ClusterEntity clusterEntity);

    List<ClusterEntity> queryRelationClusterByClusterIdListAndType(
        QueryRelationClusterByClusterIdListAndType queryRelationClusterByClusterIdListAndType);

    List<ClusterEntity> queryStorageClusterByEventMeshId(ClusterEntity clusterEntity);

    ClusterEntity queryRelationshipClusterByClusterIdAndType(ClusterEntity clusterEntity);

    List<ClusterEntity> queryStorageByClusterId(ClusterEntity clusterEntity);

    List<ClusterEntity> queryAllSubClusterByClusterId(ClusterEntity clusterEntity);

    void createCluster(ClusterEntity clusterEntity);

    GetClusterBaseMessageVO getClusterBaseMessage(ClusterIdDTO clusterIdDTO);


    Map<String, Integer> queryHomeClusterData(ClusterIdDTO clusterIdDTO);

    Integer batchInsert(List<ClusterEntity> clusterEntities, ClusterEntity clusterEntity);

    List<ClusterEntity> selectAll();

    List<ClusterEntity> selectNewlyIncreased(ClusterEntity clusterEntity);

    void createClusterInfo(ClusterEntity mainClusterEntity, List<Pair<ClusterEntity, List<RuntimeEntity>>> clusterEntityListPair,
        ClusterRelationshipEntity clusterRelationshipEntity);

    void insertCluster(ClusterEntity cluster);

    void insertClusterAndRelationship(ClusterEntity cluster, ClusterRelationshipEntity clusterRelationshipEntity);

    void createTheEntireCluster(ClusterEntity clusterEntity, ClusterRelationshipEntity clusterRelationshipEntity,
        List<RuntimeEntity> runtimeEntityList);


    Long createTheEventCluster(List<ClusterEntity> clusterEntityList, List<Pair<ClusterEntity, ClusterEntity>> clusterListRelationshipList,
        List<Pair<ClusterEntity, List<RuntimeEntity>>> clusterAndRuntimeList);

    List<ClusterEntity> selectAllCluster();


    Integer updateClusterById(ClusterEntity cluster);

    Integer deactivate(ClusterEntity cluster);

    List<ClusterEntity> queryByUpdateTime(ClusterEntity clusterEntity);

    Deque<Integer> getIndex(ClusterEntity clusterEntity);
}
