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

package org.apache.eventmesh.dashboard.console.service.health.impl;

import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.mapper.health.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.mapper.runtime.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.topic.TopicMapper;
import org.apache.eventmesh.dashboard.console.modle.vo.health.InstanceLiveProportionVo;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthDataServiceDatabaseImpl implements HealthDataService {

    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Autowired
    private TopicMapper topicMapper;


    @Override
    public InstanceLiveProportionVo getInstanceLiveProportion(Long clusterId, Integer instanceType) {
        InstanceLiveProportionVo instanceLiveProportionVo = new InstanceLiveProportionVo();
        switch (instanceType) {
            case 2:
                instanceLiveProportionVo = this.getRuntimeLiveProportion(clusterId);
                break;
            case 3:
                instanceLiveProportionVo = this.getTopicLiveProportion(clusterId);
                break;
            default:
                break;
        }
        return instanceLiveProportionVo;
    }

    public InstanceLiveProportionVo getTopicLiveProportion(Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        Integer topicNum = topicMapper.selectTopicNumByCluster(topicEntity);
        List<TopicEntity> topicEntityList = topicMapper.selectTopicByCluster(topicEntity);
        int abnormalNum = 0;
        for (TopicEntity n : topicEntityList) {
            if (CheckResultCache.getINSTANCE().getLastHealthyCheckResult("topic", n.getId()) == 0) {
                abnormalNum++;
            }
        }
        return new InstanceLiveProportionVo(abnormalNum, topicNum);
    }


    public InstanceLiveProportionVo getRuntimeLiveProportion(Long clusterId) {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterId);
        Integer topicNum = runtimeMapper.getRuntimeNumByCluster(runtimeEntity);
        List<RuntimeEntity> runtimeEntities = runtimeMapper.selectRuntimeByCluster(runtimeEntity);
        int abnormalNum = 0;
        for (RuntimeEntity n : runtimeEntities) {
            if (CheckResultCache.getINSTANCE().getLastHealthyCheckResult("runtime", n.getId()) == 0) {
                abnormalNum++;
            }
        }
        return new InstanceLiveProportionVo(abnormalNum, topicNum);
    }


    @Override
    public List<HealthCheckResultEntity> getInstanceLiveStatusHistory(Integer type, Long instanceId, Timestamp startTime) {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
        healthCheckResultEntity.setType(type);
        healthCheckResultEntity.setTypeId(instanceId);
        healthCheckResultEntity.setCreateTime(startTime);
        return healthCheckResultMapper.getInstanceLiveStatusHistory(healthCheckResultEntity);
    }

    @Override
    public HealthCheckResultEntity insertHealthCheckResult(HealthCheckResultEntity healthCheckResultEntity) {
        healthCheckResultMapper.insert(healthCheckResultEntity);
        return healthCheckResultEntity;
    }

    @Override
    public void batchInsertHealthCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        if (healthCheckResultEntityList.isEmpty()) {
            return;
        }
        healthCheckResultMapper.batchInsert(healthCheckResultEntityList);
    }

    @Override
    public void batchInsertNewCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        if (healthCheckResultEntityList.isEmpty()) {
            return;
        }
        healthCheckResultMapper.insertNewChecks(healthCheckResultEntityList);
    }

    @Override
    public List<HealthCheckResultEntity> selectAll() {
        return healthCheckResultMapper.selectAll();
    }

    @Override
    public List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTypeAndTypeId(HealthCheckResultEntity entity) {
        return healthCheckResultMapper.selectByClusterIdAndTypeAndTypeId(entity);
    }

    @Override
    public void batchUpdateCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        healthCheckResultMapper.batchUpdate(healthCheckResultEntityList);
    }

    @Override
    public void batchUpdateCheckResultByClusterIdAndTypeAndTypeId(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        if (healthCheckResultEntityList.isEmpty()) {
            return;
        }
        List<HealthCheckResultEntity> entitiesNeedToBeUpdate = healthCheckResultMapper.getIdsNeedToBeUpdateByClusterIdAndTypeAndTypeId(
            healthCheckResultEntityList);
        if (entitiesNeedToBeUpdate.isEmpty()) {
            return;
        }
        entitiesNeedToBeUpdate.forEach(entity -> {
            healthCheckResultEntityList.forEach(updateEntity -> {
                if (entity.getClusterId().equals(updateEntity.getClusterId()) && entity.getType().equals(updateEntity.getType())
                    && entity.getTypeId().equals(updateEntity.getTypeId())) {
                    entity.setState(updateEntity.getState());
                    entity.setResultDesc(updateEntity.getResultDesc());
                }
            });
        });
        healthCheckResultMapper.batchUpdate(entitiesNeedToBeUpdate);
    }


    @Override
    public List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTimeRange(Long clusterId, Timestamp startTime, Timestamp endTime) {
        return healthCheckResultMapper.selectByClusterIdAndCreateTimeRange(clusterId, startTime, endTime);
    }
}
