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


import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.modle.DO.clusterRelationship.QueryListByClusterIdAndTypeDO;
import org.apache.eventmesh.dashboard.console.modle.DO.runtime.QueryRuntimeByBigExpandClusterDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.ClusterAllMetadataDO;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeServiceImpl implements RuntimeService {

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;


    @Autowired
    private ClusterMapper clusterMapper;


    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;

    @Override
    public RuntimeEntity queryRuntimeEntityById(RuntimeEntity runtimeEntity) {
        return this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);
    }


    @Override
    public List<RuntimeEntity> queryRuntimeByBigExpandCluster(QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO) {
        return this.runtimeMapper.queryRuntimeByBigExpandCluster(queryRuntimeByBigExpandClusterDO);
    }

    @Override
    public List<RuntimeEntity> queryMetaRuntimeByStorageClusterId(QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO) {
        // 通过 storage cluster id ， 获得 main cluster id
        // 通过 子集群 id  获得 主(多) 集群 下面的其他集群
        return this.runtimeMapper.queryClusterRuntimeOnClusterSpecifyByClusterId(queryRuntimeByBigExpandClusterDO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClusterAllMetadataDO queryAllByClusterId(RuntimeEntity runtimeEntity, boolean isRuntime, boolean isRelationship) {

        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(runtimeEntity.getClusterId());
        clusterEntity = this.clusterMapper.queryByClusterId(clusterEntity);

        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        List<ClusterEntity> definitionList = new ArrayList<>();
        definitionList.add(clusterEntity);
        for (int i = 0; i < 5; i++) {
            List<ClusterEntity> relationshipList = this.clusterMapper.queryRelationshipClusterByClusterIdAndType(definitionList);
            definitionList.clear();
            relationshipList.forEach(entity -> {
                if (entity.getClusterType().isRuntime()) {
                    definitionList.add(entity);
                }
                clusterEntityList.add(entity);
            });
            if (definitionList.isEmpty()) {
                break;
            }
        }

        ClusterAllMetadataDO clusterAllMetadata = new ClusterAllMetadataDO();
        clusterAllMetadata.setClusterEntityList(clusterEntityList);
        if (isRuntime) {
            List<RuntimeEntity> runtimeEntityList = this.runtimeMapper.queryRuntimeByClusterId(clusterEntityList);
            clusterAllMetadata.setRuntimeEntityList(runtimeEntityList);
        }
        if (isRelationship) {
            QueryListByClusterIdAndTypeDO data = QueryListByClusterIdAndTypeDO.builder().build();
            List<ClusterRelationshipEntity> relationshipEntityList =
                this.clusterRelationshipMapper.queryClusterRelationshipEntityListByClusterId(data);
            clusterAllMetadata.setClusterRelationshipEntityList(relationshipEntityList);
        }
        return clusterAllMetadata;
    }

    @Override
    public List<RuntimeEntity> queryRuntimeToFrontByClusterId(RuntimeEntity runtimeEntity) {
        List<RuntimeEntity> runtimeByClusterId = runtimeMapper.getRuntimesToFrontByCluster(runtimeEntity);
        runtimeByClusterId.forEach(n -> {
            n.setStatus(CheckResultCache.getINSTANCE().getLastHealthyCheckResult("runtime", n.getId()));
        });
        return runtimeByClusterId;
    }


    @Override
    public List<RuntimeEntity> queryRuntimeToFrontByClusterIdList(List<ClusterEntity> clusterEntityList) {
        return this.runtimeMapper.queryRuntimeToFrontByClusterIdList(clusterEntityList);
    }

    @Override
    public void batchInsert(List<RuntimeEntity> runtimeEntities) {
        runtimeMapper.batchInsert(runtimeEntities);
    }

    @Override
    public Integer batchUpdate(List<RuntimeEntity> runtimeEntities) {
        return 0;
    }

    @Override
    public void batchUpdateDeployStatusType(List<RuntimeEntity> runtimeEntitieList) {
        this.runtimeMapper.batchUpdateDeployStatusType(runtimeEntitieList);
    }

    @Override
    public void batchUpdateDeployStatusType(List<RuntimeEntity> runtimeEntitieList, DeployStatusType deployStatusType) {
        this.runtimeMapper.batchUpdateDeployStatusType(runtimeEntitieList, deployStatusType);
    }


    @Override
    public List<RuntimeEntity> selectAll() {
        return runtimeMapper.queryAll();
    }

    @Override
    public List<RuntimeEntity> queryByUpdateTime(RuntimeEntity runtimeEntity) {
        return runtimeMapper.queryByUpdateTime(runtimeEntity);
    }

    @Override
    public void insertRuntimeByClusterData(RuntimeEntity runtimeEntity) {
        this.runtimeMapper.insertRuntime(runtimeEntity);
        // copy config ， rocketmq 集群 copy topic
    }

    @Override
    public void insertRuntime(RuntimeEntity runtimeEntity) {
        runtimeMapper.insertRuntime(runtimeEntity);
    }

    @Override
    public void updateRuntimeByCluster(RuntimeEntity runtimeEntity) {
        runtimeMapper.updateRuntimeByCluster(runtimeEntity);
    }

    @Override
    public void deleteRuntimeByCluster(RuntimeEntity runtimeEntity) {
        runtimeMapper.deleteRuntimeByCluster(runtimeEntity);
    }

    @Override
    public void deactivate(RuntimeEntity runtimeEntity) {
        runtimeMapper.deactivate(runtimeEntity);
    }
}
