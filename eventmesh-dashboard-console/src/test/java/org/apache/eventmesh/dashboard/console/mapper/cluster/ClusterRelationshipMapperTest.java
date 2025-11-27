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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.databuild.BuildFullSceneData;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.model.DO.clusterRelationship.QueryListByClusterIdAndTypeDO;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
@Sql(scripts = {"classpath:eventmesh-dashboard.sql"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ClusterRelationshipMapperTest {

    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;


    @Autowired
    private ClusterMapper clusterMapper;

    private BuildFullSceneData buildFullSceneData = new BuildFullSceneData();

    private static final AtomicLong nameIndex = new AtomicLong(2);

    private ClusterRelationshipEntity clusterRelationshipEntity;


    @Test
    public void test_queryListByClusterIdAndType() {
        this.buildFullSceneData.build(ClusterType.EVENTMESH_CLUSTER);
        this.clusterMapper.batchInsert(this.buildFullSceneData.getClusterEntityList());
        this.buildFullSceneData.createClusterRelationshipEntity();
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = this.buildFullSceneData.getClusterRelationshipEntityList();

        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);

        QueryListByClusterIdAndTypeDO data = new QueryListByClusterIdAndTypeDO();
        data.setClusterId(this.buildFullSceneData.getClusterTripleList().get(0).getLeft().getId());
        List<ClusterRelationshipEntity> newClusterRelationshipEntityList = this.clusterRelationshipMapper.queryListByClusterIdAndType(data);

        data.setRelationshipTypeList(List.of(ClusterType.EVENTMESH_RUNTIME));
        newClusterRelationshipEntityList = this.clusterRelationshipMapper.queryListByClusterIdAndType(data);

        newClusterRelationshipEntityList.size();
    }

    @Test
    public void test_selectNewlyIncreased() {
        this.test_batchClusterRelationshipEntry();

        ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterRelationshipEntity.setUpdateTime(LocalDateTime.now().plusHours(1L).truncatedTo(ChronoUnit.MILLIS));
        List<ClusterRelationshipEntity> clusterRelationshipEntityList =
            this.clusterRelationshipMapper.queryNewlyIncreased(clusterRelationshipEntity);
        Assert.assertEquals(0, clusterRelationshipEntityList.size());

        clusterRelationshipEntity.setUpdateTime(LocalDateTime.now().minusHours(1L).truncatedTo(ChronoUnit.MILLIS));
        clusterRelationshipEntityList = this.clusterRelationshipMapper.queryNewlyIncreased(clusterRelationshipEntity);
        Assert.assertEquals(10, clusterRelationshipEntityList.size());
    }

    @Test
    public void test_relieveRelationship() {
        this.test_insert_ClusterRelationshipEntry();
        this.clusterRelationshipMapper.relieveRelationship(this.clusterRelationshipEntity);
        ClusterRelationshipEntity clusterRelationshipEntity = this.clusterRelationshipMapper.queryById(this.clusterRelationshipEntity);
        Assert.assertEquals(3, clusterRelationshipEntity.getStatus(), 1L);

    }

    @Test
    public void test_batchClusterRelationshipEntry() {
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            clusterRelationshipEntityList.add(this.buildClusterRelationshipEntity());
        }
        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);
        clusterRelationshipEntityList = this.clusterRelationshipMapper.queryAll(null);
        Assert.assertEquals(10, clusterRelationshipEntityList.size());

    }

    @Test
    public void test_insert_ClusterRelationshipEntry() {
        clusterRelationshipEntity = this.buildClusterRelationshipEntity();
        this.clusterRelationshipMapper.insertClusterRelationshipEntry(clusterRelationshipEntity);
        clusterRelationshipEntity = this.clusterRelationshipMapper.queryById(clusterRelationshipEntity);
        Assert.assertNotNull(clusterRelationshipEntity);
    }

    public static ClusterRelationshipEntity buildClusterRelationshipEntity(ClusterEntity clusterEntity, ClusterEntity relationshipEntity) {
        ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterRelationshipEntity.setOrganizationId(clusterEntity.getOrganizationId());
        clusterRelationshipEntity.setClusterId(clusterEntity.getId());
        clusterRelationshipEntity.setClusterType(clusterEntity.getClusterType());
        clusterRelationshipEntity.setRelationshipId(relationshipEntity.getId());
        clusterRelationshipEntity.setRelationshipType(relationshipEntity.getClusterType());
        return clusterRelationshipEntity;

    }

    public ClusterRelationshipEntity buildClusterRelationshipEntity() {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setOrganizationId(1L);
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntity.setId(1L);
        ClusterEntity relationshipEntity = new ClusterEntity();
        relationshipEntity.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        relationshipEntity.setId(nameIndex.incrementAndGet());
        return buildClusterRelationshipEntity(clusterEntity, relationshipEntity);
    }


}
