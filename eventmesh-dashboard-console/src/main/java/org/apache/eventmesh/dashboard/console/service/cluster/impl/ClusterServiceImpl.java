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

import org.apache.eventmesh.dashboard.console.cache.ClusterCache;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.group.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.connection.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.group.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.runtime.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.topic.TopicMapper;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.ResourceNumVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ClusterServiceImpl implements ClusterService {

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
    public GetClusterBaseMessageVO getClusterBaseMessage(Long clusterId) {
        GetClusterBaseMessageVO getClusterBaseMessageVO = new GetClusterBaseMessageVO();
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setTopicNum(topicMapper.selectTopicNumByCluster(topicEntity));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConsumerGroupNum(oprGroupMapper.getConsumerNumByCluster(groupEntity));
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setConnectionNum(connectionMapper.selectConnectionNumByCluster(connectionEntity));
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterId);
        getClusterBaseMessageVO.setRuntimeNum(runtimeMapper.getRuntimeNumByCluster(runtimeEntity));
        return getClusterBaseMessageVO;
    }

    @Override
    public ResourceNumVO getResourceNumByCluster(Long clusterId) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        Integer connectionNumByCluster = connectionMapper.selectConnectionNumByCluster(connectionEntity);
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        Integer topicNumByCluster = topicMapper.selectTopicNumByCluster(topicEntity);
        ResourceNumVO resourceNumVO = new ResourceNumVO(topicNumByCluster, connectionNumByCluster);
        return resourceNumVO;
    }


    @Override
    public void batchInsert(List<ClusterEntity> clusterEntities) {
        clusterMapper.batchInsert(clusterEntities);
        updateClusterCache();
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
    public void addCluster(ClusterEntity cluster) {
        clusterMapper.addCluster(cluster);
        updateClusterCache();
    }

    @Override
    public List<ClusterEntity> selectAllCluster() {
        return clusterMapper.selectAllCluster();
    }

    @Override
    public ClusterEntity selectClusterById(ClusterEntity cluster) {
        return clusterMapper.selectClusterById(cluster);
    }


    public List<ClusterEntity> selectIncrementCluster() {
        return null;
    }

    @Override
    public void updateClusterById(ClusterEntity cluster) {
        clusterMapper.updateClusterById(cluster);
        updateClusterCache();
    }

    @Override
    public void deactivate(ClusterEntity cluster) {
        clusterMapper.deactivate(cluster);
    }

    private void updateClusterCache() {
        List<ClusterEntity> clusters = selectAll();
        ClusterCache.getINSTANCE().syncClusters(clusters);
    }
}
