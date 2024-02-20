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
import org.apache.eventmesh.dashboard.console.mapper.health.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthDataServiceDatabaseImpl implements HealthDataService {

    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;

    @Override
    public HealthCheckResultEntity insertHealthCheckResult(HealthCheckResultEntity healthCheckResultEntity) {
        healthCheckResultMapper.insert(healthCheckResultEntity);
        return healthCheckResultEntity;
    }

    @Override
    public void batchInsertHealthCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        healthCheckResultMapper.batchInsert(healthCheckResultEntityList);
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
        List<HealthCheckResultEntity> idsNeedToBeUpdate = healthCheckResultMapper.getIdsNeedToBeUpdateByClusterIdAndTypeAndTypeId(
            healthCheckResultEntityList);
        idsNeedToBeUpdate.forEach(entity -> {
            healthCheckResultEntityList.forEach(updateEntity -> {
                if (entity.getClusterId().equals(updateEntity.getClusterId()) && entity.getType().equals(updateEntity.getType())
                    && entity.getTypeId().equals(updateEntity.getTypeId())) {
                    updateEntity.setId(entity.getId());
                }
            });
        });
        healthCheckResultMapper.batchUpdate(healthCheckResultEntityList);
    }


    @Override
    public List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTimeRange(Long clusterId, Timestamp startTime, Timestamp endTime) {
        return healthCheckResultMapper.selectByClusterIdAndCreateTimeRange(clusterId, startTime, endTime);
    }
}
