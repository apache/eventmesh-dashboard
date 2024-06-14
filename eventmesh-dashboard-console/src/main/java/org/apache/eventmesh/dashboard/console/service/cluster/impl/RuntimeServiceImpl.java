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

import java.util.List;

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
    public List<RuntimeEntity> selectRuntimeToFrontByClusterId(RuntimeEntity runtimeEntity) {
        List<RuntimeEntity> runtimeByClusterId = runtimeMapper.selectRuntimesToFrontByCluster(runtimeEntity);
        runtimeByClusterId.forEach(n -> {
            n.setStatus(CheckResultCache.getINSTANCE().getLastHealthyCheckResult("runtime", n.getId()));
        });
        return runtimeByClusterId;
    }


    @Override
    public Integer batchInsert(List<RuntimeEntity> runtimeEntities) {
        return runtimeMapper.batchInsert(runtimeEntities);
    }

    @Override
    public List<RuntimeEntity> selectAll() {
        return runtimeMapper.selectAll();
    }

    @Override
    public List<RuntimeEntity> selectByHostPort(RuntimeEntity runtimeEntity) {
        return runtimeMapper.selectByHostPort(runtimeEntity);
    }

    @Override
    public void insertRuntime(RuntimeEntity runtimeEntity) {
        runtimeMapper.insertRuntime(runtimeEntity);
    }

    @Override
    public Integer updateRuntimeByCluster(RuntimeEntity runtimeEntity) {
        return runtimeMapper.updateRuntimeByCluster(runtimeEntity);
    }

    @Override
    public Integer deleteRuntimeByCluster(RuntimeEntity runtimeEntity) {
        return runtimeMapper.deleteRuntimeByCluster(runtimeEntity);
    }

    @Override
    public Integer deactivate(RuntimeEntity runtimeEntity) {
        return runtimeMapper.deactivate(runtimeEntity);
    }
}
