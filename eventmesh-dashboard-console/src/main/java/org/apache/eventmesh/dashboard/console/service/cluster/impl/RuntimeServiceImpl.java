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


import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RuntimeServiceImpl implements RuntimeService {

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;


    @Override
    public RuntimeEntity queryRuntimeEntityById(RuntimeEntity runtimeEntity) {
        return this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);
    }

    @Override
    public List<RuntimeEntity> getRuntimeByClusterRelationship(RuntimeEntity runtimeEntity) {
        return this.runtimeMapper.getRuntimeByClusterRelationship(runtimeEntity);
    }



    @Override
    public List<RuntimeEntity> queryOnlyRuntimeByClusterId(RuntimeEntity runtimeEntity){
        return null;
    }

    @Override
    public Map<Long, List<RuntimeEntity>> queryMetaRuntimeByClusterId(RuntimeEntity runtimeEntity) {
        List<RuntimeEntity> runtimeEntityList = this.getRuntimeByClusterRelationship(runtimeEntity);

        Map<Long, List<RuntimeEntity>> runtimeEntityMap = new HashMap<Long, List<RuntimeEntity>>();
        runtimeEntityList.forEach(entity -> {
            runtimeEntityMap.computeIfAbsent(entity.getClusterId(), k -> new ArrayList<>()).add(entity);
        });
        return runtimeEntityMap;
    }

    @Override
    public List<RuntimeEntity> getRuntimeToFrontByClusterId(RuntimeEntity runtimeEntity) {
        List<RuntimeEntity> runtimeByClusterId = runtimeMapper.getRuntimesToFrontByCluster(runtimeEntity);
        runtimeByClusterId.forEach(n -> {
            n.setStatus(CheckResultCache.getINSTANCE().getLastHealthyCheckResult("runtime", n.getId()));
        });
        return runtimeByClusterId;
    }


    @Override
    public void batchInsert(List<RuntimeEntity> runtimeEntities) {
        runtimeMapper.batchInsert(runtimeEntities);
    }

    @Override
    public List<RuntimeEntity> selectAll() {
        return runtimeMapper.selectAll();
    }

    @Override
    public List<RuntimeEntity> selectByUpdateTime(RuntimeEntity runtimeEntity) {
        return runtimeMapper.selectByUpdateTime(runtimeEntity);
    }

    @Override
    public List<RuntimeEntity> selectByHostPort(RuntimeEntity runtimeEntity) {
        return runtimeMapper.selectByHostPort(runtimeEntity);
    }

    @Override
    public void insertRuntime(RuntimeEntity runtimeEntity) {
        runtimeMapper.addRuntime(runtimeEntity);
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
