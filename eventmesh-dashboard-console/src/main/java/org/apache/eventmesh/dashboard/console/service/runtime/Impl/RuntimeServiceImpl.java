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

package org.apache.eventmesh.dashboard.console.service.runtime.Impl;

import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.runtime.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.service.runtime.RuntimeService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RuntimeServiceImpl implements RuntimeService {

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Override
    public void batchInsert(List<RuntimeEntity> runtimeEntities) {
        runtimeMapper.batchInsert(runtimeEntities);
    }

    @Override
    public List<RuntimeEntity> selectAll() {
        return runtimeMapper.selectAll();
    }

    @Override
    public List<RuntimeEntity> getRuntimeByClusterId(Long clusterId) {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterId);
        return runtimeMapper.selectRuntimeByCluster(runtimeEntity);
    }

    @Override
    public void addRuntime(RuntimeEntity runtimeEntity) {
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
}
