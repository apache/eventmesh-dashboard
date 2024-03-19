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

package org.apache.eventmesh.dashboard.console.unit.runtime;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.runtime.RuntimeMapper;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestRuntimeMapper {

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Test
    public void testAddRuntimeMapper() {
        RuntimeEntity runtimeEntity = new RuntimeEntity(null, 1L, "runtime1", 2L, 1019, 1099, 1L, "null", 1, null, null, "null");
        runtimeMapper.addRuntime(runtimeEntity);
        List<RuntimeEntity> runtimeEntities = runtimeMapper.selectRuntimeByCluster(runtimeEntity);
        RuntimeEntity runtimeEntity1 = runtimeEntities.get(0);
        runtimeEntity1.setCreateTime(null);
        runtimeEntity1.setUpdateTime(null);
        Assert.assertEquals(runtimeEntity1, runtimeEntity);
    }

    @Test
    public void testUpdateRuntimeByCluster() {
        RuntimeEntity runtimeEntity = new RuntimeEntity(null, 1L, "runtime1", 2L, 1019, 1099, 1L, "null", 1, null, null, "null");
        runtimeMapper.addRuntime(runtimeEntity);
        runtimeEntity.setPort(1000);
        runtimeEntity.setJmxPort(1099);
        runtimeEntity.setStatus(0);
        runtimeMapper.updateRuntimeByCluster(runtimeEntity);
        List<RuntimeEntity> runtimeEntities = runtimeMapper.selectRuntimeByCluster(runtimeEntity);
        RuntimeEntity runtimeEntity1 = runtimeEntities.get(0);
        runtimeEntity1.setCreateTime(null);
        runtimeEntity1.setUpdateTime(null);
        Assert.assertEquals(runtimeEntity, runtimeEntity1);
    }

    @Test
    public void testDeleteRuntime() {
        RuntimeEntity runtimeEntity = new RuntimeEntity(null, 1L, "runtime1", 2L, 1019, 1099, 1L, "null", 1, null, null, "null");
        runtimeMapper.addRuntime(runtimeEntity);
        runtimeMapper.deleteRuntimeByCluster(runtimeEntity);
        List<RuntimeEntity> runtimeEntities = runtimeMapper.selectRuntimeByCluster(runtimeEntity);
        Assert.assertEquals(runtimeEntities.size(), 0);
    }
}
