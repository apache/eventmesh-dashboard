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
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.modle.function.OverviewType;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.OverviewService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

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
    private RuntimeMapper runtimeMapper;

    @Autowired
    private OprGroupMapper oprGroupMapper;

    @Autowired
    private TopicMapper topicMapper;


    @Override
    public void createCluster(ClusterEntity clusterEntity) {

    }

    @Override
    public GetClusterBaseMessageVO selectClusterBaseMessage(Long clusterId) {
        GetClusterBaseMessageVO getClusterBaseMessageVO = new GetClusterBaseMessageVO();
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setTopicNum(topicMapper.selectTopicNumByCluster(topicEntity));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConsumerGroupNum(oprGroupMapper.selectConsumerNumByCluster(groupEntity));
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConnectionNum(connectionMapper.selectConnectionNumByCluster(connectionEntity));
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setRuntimeNum(runtimeMapper.selectRuntimeNumByCluster(runtimeEntity));
        return getClusterBaseMessageVO;
    }

    @Override
    public Map<String, Integer> queryHomeClusterData(Long clusterId) {
        return null;
    }


    @Override
    public Integer batchInsert(List<ClusterEntity> clusterEntities) {
        return clusterMapper.batchInsert(clusterEntities);
    }

    @Override
    public List<ClusterEntity> selectAll() {
        return clusterMapper.selectAllCluster();
    }

    @Override
    public List<ClusterEntity> selectNewlyIncreased(ClusterEntity clusterEntity) {
        return clusterMapper.selectAllCluster();
    }

    @Override
    public void insertCluster(ClusterEntity cluster) {
        clusterMapper.insertCluster(cluster);
    }

    @Override
    public List<ClusterEntity> selectAllCluster() {
        return clusterMapper.selectAllCluster();
    }

    @Override
    public ClusterEntity selectClusterById(ClusterEntity cluster) {
        return clusterMapper.selectClusterById(cluster);
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
    public Object overview(OverviewType overviewtype) {
        return null;
    }
}
