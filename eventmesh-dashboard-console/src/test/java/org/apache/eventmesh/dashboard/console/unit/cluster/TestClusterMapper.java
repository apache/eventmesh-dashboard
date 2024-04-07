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

package org.apache.eventmesh.dashboard.console.unit.cluster;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestClusterMapper {

    @Autowired
    private ClusterMapper clusterMapper;


    @Test
    public void testSelectAllCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, 0, null, null, 0);
        ClusterEntity clusterEntity1 =
            new ClusterEntity(null, "c1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, 0, null, null, 0);
        clusterMapper.addCluster(clusterEntity);
        clusterMapper.addCluster(clusterEntity1);
        List<ClusterEntity> clusterEntities = clusterMapper.selectAllCluster();
        Assert.assertEquals(clusterEntities.size(), 2);
    }

    @Test
    public void testSelectClusterById() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, 0, null, null, 0);
        clusterMapper.addCluster(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        clusterEntity1.setCreateTime(null);
        clusterEntity1.setUpdateTime(null);
        Assert.assertEquals(clusterEntity1, clusterEntity);
    }

    @Test
    public void testUpdateCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, 0, null, null, 0);
        clusterMapper.addCluster(clusterEntity);
        clusterEntity.setDescription("nothing");
        clusterEntity.setName("cl2");
        clusterEntity.setAuthType(1);
        clusterEntity.setBootstrapServers("1999");
        clusterEntity.setClientProperties("nothing");
        clusterEntity.setEventmeshVersion("1.10.0");
        clusterEntity.setJmxProperties("nothing");
        clusterEntity.setRegistryNameList("1.23.18");
        clusterEntity.setRunState(1);
        clusterEntity.setRegProperties("nothing");
        clusterMapper.updateClusterById(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        clusterEntity1.setCreateTime(null);
        clusterEntity1.setUpdateTime(null);
        Assert.assertEquals(clusterEntity1, clusterEntity);
    }

    @Test
    public void testDeleteCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, 0, null, null, 0);
        clusterMapper.addCluster(clusterEntity);
        clusterMapper.deactivate(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        Assert.assertEquals(clusterEntity1, null);
    }
}
