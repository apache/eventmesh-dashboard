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


package org.apache.eventmesh.dashboard.console.service.cluster.impl;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.QO.cluster.QueryRelationClusterByClusterIdListAndType;
import org.apache.eventmesh.dashboard.console.model.function.OverviewDTO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.OverviewService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class ClusterServiceImpl implements ClusterService, OverviewService {

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private ClusterMapper clusterMapper;

    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private TopicMapper topicMapper;


    @Override
    public void createCluster(ClusterEntity clusterEntity) {

    }

    @Override
    public ClusterEntity queryClusterById(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryByClusterId(clusterEntity);
    }

    @Override
    public List<ClusterEntity> queryClusterListByClusterList(List<ClusterEntity> clusterEntityList) {
        return this.clusterMapper.queryClusterListByClusterList(clusterEntityList);
    }

    @Override
    public List<ClusterEntity> queryClusterByOrganizationIdAndType(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryClusterByOrganizationIdAndType(clusterEntity);
    }

    @Override
    public List<ClusterEntity> queryRelationClusterByClusterIdAndType(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryRelationClusterByClusterIdAndType(clusterEntity);
    }

    @Override
    public List<ClusterEntity> queryRelationClusterByClusterIdListAndType(
        QueryRelationClusterByClusterIdListAndType queryRelationClusterByClusterIdListAndType) {
        return this.clusterMapper.queryRelationClusterByClusterIdListAndType(queryRelationClusterByClusterIdListAndType);
    }

    @Override
    public List<ClusterEntity> queryStorageClusterByEventMeshId(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryStorageClusterByEventMeshId(clusterEntity);
    }

    @Override
    public ClusterEntity queryRelationshipClusterByClusterIdAndType(ClusterEntity clusterEntity) {
        List<ClusterEntity> queryData = new ArrayList<>();
        queryData.add(clusterEntity);
        List<ClusterEntity> clusterEntityList = this.clusterMapper.queryRelationshipClusterByClusterIdAndType(queryData);
        return clusterEntityList.isEmpty() ? null : clusterEntityList.get(0);
    }

    @Override
    public List<ClusterEntity> queryStorageByClusterId(ClusterEntity clusterEntity) {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        this.clusterMapper.queryRelationshipClusterByClusterIdAndType(clusterEntityList).forEach(entity -> {
            if (entity.getClusterType().isStorage()) {
                clusterEntityList.add(entity);
            }
        });
        return clusterEntityList;
    }


    @Override
    public List<ClusterEntity> queryAllSubClusterByClusterId(ClusterEntity clusterEntity) {
        return null;
    }

    @Override
    public boolean nameExist(ClusterEntity clusterEntity) {
        return true;
    }

    @Override
    public GetClusterBaseMessageVO getClusterBaseMessage(ClusterIdDTO clusterIdDTO) {
        Long clusterId = clusterIdDTO.getClusterId();
        GetClusterBaseMessageVO getClusterBaseMessageVO = new GetClusterBaseMessageVO();
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setTopicNum(topicMapper.selectTopicNumByCluster(topicEntity));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConsumerGroupNum(groupMapper.getConsumerNumByCluster(groupEntity));
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConnectionNum(connectionMapper.selectConnectionNumByCluster(connectionEntity));
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setRuntimeNum(runtimeMapper.getRuntimeNumByCluster(runtimeEntity));
        return getClusterBaseMessageVO;
    }

    @Override
    public Map<String, Integer> queryHomeClusterData(ClusterIdDTO clusterIdDTO) {
        return null;
    }


    @Override
    public Integer batchInsert(List<ClusterEntity> clusterEntities, ClusterEntity clusterEntity) {
        clusterMapper.batchInsert(clusterEntities);
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();
        clusterEntities.forEach(entity -> {
            ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
            clusterRelationshipEntityList.add(clusterRelationshipEntity);

            clusterRelationshipEntityList.add(clusterRelationshipEntity);
            clusterRelationshipEntity.setClusterId(clusterEntity.getId());
            clusterRelationshipEntity.setClusterType(clusterEntity.getClusterType());
        });
        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);
        return 1;
    }

    @Override
    public List<ClusterEntity> selectAll() {
        return clusterMapper.queryAllCluster();
    }

    @Override
    public List<ClusterEntity> selectNewlyIncreased(ClusterEntity clusterEntity) {
        return clusterMapper.queryAllCluster();
    }

    @Override
    public void createClusterInfo(ClusterEntity mainClusterEntity, List<Pair<ClusterEntity, List<RuntimeEntity>>> clusterEntityListPair,
        ClusterRelationshipEntity mainClusterRelationshipEntity) {
        this.clusterMapper.insertCluster(mainClusterEntity);
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        clusterEntityListPair.forEach(entity -> {
            clusterEntityList.add(entity.getLeft());
        });
        this.clusterMapper.batchInsert(clusterEntityList);
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();
        if (Objects.nonNull(mainClusterRelationshipEntity)) {
            mainClusterRelationshipEntity.setRelationshipId(mainClusterEntity.getId());
            clusterRelationshipEntityList.add(mainClusterRelationshipEntity);
        }
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        clusterEntityListPair.forEach(entity -> {
            ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
            clusterRelationshipEntityList.add(clusterRelationshipEntity);
            clusterRelationshipEntity.setOrganizationId(mainClusterEntity.getOrganizationId());
            clusterRelationshipEntity.setClusterId(mainClusterEntity.getId());
            clusterRelationshipEntity.setClusterType(mainClusterEntity.getClusterType());
            clusterRelationshipEntity.setRelationshipId(entity.getKey().getId());
            clusterRelationshipEntity.setRelationshipType(entity.getKey().getClusterType());
            entity.getValue().forEach(value -> {
                runtimeEntityList.add(value);
                value.setClusterId(entity.getKey().getId());
                value.setClusterType(entity.getKey().getClusterType());
            });
        });
        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);
        this.runtimeMapper.batchInsert(runtimeEntityList);

    }

    @Override
    public void insertCluster(ClusterEntity cluster) {
        clusterMapper.insertCluster(cluster);
    }

    @Override
    public void insertClusterAndRelationship(ClusterEntity cluster, ClusterRelationshipEntity clusterRelationshipEntity) {
        clusterMapper.insertCluster(cluster);
        clusterRelationshipMapper.insertClusterRelationshipEntry(clusterRelationshipEntity);
    }

    @Override
    public void createTheEntireCluster(ClusterEntity clusterEntity, ClusterRelationshipEntity clusterRelationshipEntity,
        List<RuntimeEntity> runtimeEntityList) {
        this.clusterMapper.insertCluster(clusterEntity);
        this.clusterRelationshipMapper.insertClusterRelationshipEntry(clusterRelationshipEntity);
        this.runtimeMapper.batchInsert(runtimeEntityList);
    }

    @Override
    public Long createTheEventCluster(List<ClusterEntity> clusterEntityList, List<Pair<ClusterEntity, ClusterEntity>> clusterListRelationshipList,
        List<Pair<ClusterEntity, List<RuntimeEntity>>> clusterAndRuntimeList) {

        this.clusterMapper.batchInsert(clusterEntityList);

        List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();
        clusterListRelationshipList.forEach(pair -> {
            ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
            clusterRelationshipEntity.setOrganizationId(pair.getLeft().getOrganizationId());
            clusterRelationshipEntity.setClusterId(pair.getLeft().getId());
            clusterRelationshipEntity.setClusterType(pair.getLeft().getClusterType());
            clusterRelationshipEntity.setRelationshipId(pair.getRight().getId());
            clusterRelationshipEntity.setRelationshipType(pair.getRight().getClusterType());
            clusterRelationshipEntityList.add(clusterRelationshipEntity);
        });
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        clusterAndRuntimeList.forEach(pair -> {
            pair.getRight().forEach((v) -> {
                v.setClusterId(pair.getLeft().getId());
                runtimeEntityList.add(v);
            });
        });
        this.runtimeMapper.batchInsert(runtimeEntityList);
        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);
        return 0L;
    }

    @Override
    public List<ClusterEntity> selectAllCluster() {
        return clusterMapper.queryAllCluster();
    }


    @Override
    public Integer updateClusterById(ClusterEntity cluster) {
        return clusterMapper.updateClusterById(cluster);
    }

    @Override
    public Integer deactivate(ClusterEntity cluster) {
        return clusterMapper.deactivate(cluster);
    }

    @Override
    public Object overview(OverviewDTO overviewDTO) {
        return null;
    }

    @Override
    public List<ClusterEntity> queryByUpdateTime(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryClusterByUpdate(clusterEntity);
    }

    @Override
    public LinkedList<Integer> getIndex(ClusterEntity clusterEntity) {
        ClusterEntity before = this.clusterMapper.lockByClusterId(clusterEntity);
        this.clusterMapper.updateNumByClusterId(clusterEntity);
        LinkedList<Integer> indexList = new LinkedList<>();
        for (int i = before.getRuntimeIndex(); i <= before.getRuntimeIndex() + clusterEntity.getRuntimeIndex(); i++) {
            indexList.add(i);
        }
        return indexList;
    }
}
