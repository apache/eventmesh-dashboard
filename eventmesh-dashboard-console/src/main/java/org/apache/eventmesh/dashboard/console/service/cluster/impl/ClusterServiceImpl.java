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
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.modle.function.OverviewDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.OverviewService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
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
    public List<ClusterEntity> queryClusterByOrganizationIdAndType(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryClusterByOrganizationIdAndType(clusterEntity);
    }

    @Override
    public List<ClusterEntity> queryRelationClusterByClusterIdAndType(ClusterEntity clusterEntity) {
        return this.clusterMapper.queryRelationClusterByClusterIdAndType(clusterEntity);
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
    public void insertCluster(ClusterEntity cluster) {
        clusterMapper.insertCluster(cluster);
    }

    @Override
    public void insertClusterAndRelationship(ClusterEntity cluster, ClusterRelationshipEntity clusterRelationshipEntity) {
        clusterMapper.insertCluster(cluster);
        clusterRelationshipMapper.insertClusterRelationshipEntry(clusterRelationshipEntity);
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
        return null;
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
