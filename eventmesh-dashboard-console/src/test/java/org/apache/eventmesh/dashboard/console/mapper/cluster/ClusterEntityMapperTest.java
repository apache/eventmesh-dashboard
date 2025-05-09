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

package org.apache.eventmesh.dashboard.console.mapper.cluster;


import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.databuild.BuildFullSceneData;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
//@Sql(scripts = {"classpath:eventmesh-dashboard.sql"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ClusterEntityMapperTest {


    @Autowired
    private ClusterMapper clusterMapper;


    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;

    private final BuildFullSceneData buildFullSceneData = new BuildFullSceneData();

    private static final AtomicLong nameIndex = new AtomicLong();

    private ClusterEntity clusterEntity = new ClusterEntity();

    @Before
    public void init() {
        this.clusterEntity = createClusterEntity();
        this.clusterMapper.insertCluster(clusterEntity);
    }

    public static ClusterEntity createClusterEntity(){
        return createClusterEntity(ClusterType.EVENTMESH_CLUSTER);
    }

    public static ClusterEntity createClusterEntity(ClusterType clusterType) {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setOrganizationId(1L);
        clusterEntity.setName("test-" + nameIndex.incrementAndGet());
        clusterEntity.setClusterType(clusterType);
        clusterEntity.setVersion("");
        clusterEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        clusterEntity.setFirstToWhom(FirstToWhom.NOT);
        clusterEntity.setReplicationType(ReplicationType.NOT);
        clusterEntity.setDeployStatusType(DeployStatusType.CREATE_WAIT);
        clusterEntity.setClusterOwnType(ClusterOwnType.INDEPENDENCE);
        clusterEntity.setResourcesConfigId(1L);
        clusterEntity.setDeployScriptId(1L);

        clusterEntity.setDescription("");
        clusterEntity.setConfig("");
        clusterEntity.setAuthType("");
        clusterEntity.setJmxProperties("");

        return clusterEntity;
    }


    @Test
    public void test_queryRelationClusterByClusterIdAndType() {
        this.buildFullSceneData.buildReplica(ClusterType.STORAGE_REDIS_CLUSTER);

    }

    @Test
    public void test_batch_cluster() {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            clusterEntityList.add(createClusterEntity());
        }
        this.clusterMapper.batchInsert(clusterEntityList);
        clusterEntityList.forEach(clusterEntity -> {
            ClusterEntity newClusterEntity = this.clusterMapper.queryByClusterId(clusterEntity);
            Assert.assertNotNull(newClusterEntity);
        });
        List<ClusterEntity> newClusterEntity = this.clusterMapper.queryAllCluster();
        Assert.assertEquals(11, newClusterEntity.size());
    }

    @Test
    public void test_insert_cluster() {
        ClusterEntity newClusterEntity = this.clusterMapper.queryByClusterId(clusterEntity);
        Assert.assertNotNull(newClusterEntity);
    }

    public void test_updateNumByClusterId() {

        this.clusterMapper.insertCluster(clusterEntity);

        clusterMapper.updateNumByClusterId(clusterEntity);
    }
}
