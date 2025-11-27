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

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.databuild.BuildFullMetadata;
import org.apache.eventmesh.dashboard.console.databuild.BuildFullSceneData;
import org.apache.eventmesh.dashboard.console.databuild.BuildFullSceneData.ClusterFrameworkData;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.model.DO.runtime.QueryRuntimeByBigExpandClusterDO;

import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Assert;
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
public class RuntimeEntityMapperTest {

    @Autowired
    private ClusterMapper clusterMapper;

    @Autowired
    private RuntimeMapper runtimeMapper;

    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;

    private final BuildFullSceneData buildFullSceneData = new BuildFullSceneData();

    private static final Map<Long,AtomicLong> aliasIndex = new HashMap<>();


    @Test
    public void mock_jvm() {
        BuildFullMetadata buildFullMetadata = BuildFullMetadata.builder().
            clusterTypeSet(Set.of(ClusterType.EVENTMESH_JVM_CLUSTER, ClusterType.STORAGE_JVM_CLUSTER)).
            firstToWhomSet(Set.of(FirstToWhom.DASHBOARD, FirstToWhom.RUNTIME, FirstToWhom.NOT)).eventMeshEnabled(true).build();
        buildFullSceneData.buildBySupplement(buildFullMetadata);
        List<ClusterEntity> clusterEntityList = buildFullSceneData.getClusterEntityList();
        this.clusterMapper.batchInsert(clusterEntityList);
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = buildFullSceneData.createClusterRelationshipEntity();
        this.clusterRelationshipMapper.batchClusterRelationshipEntry(clusterRelationshipEntityList);
        List<RuntimeEntity> runtimeEntityList = buildFullSceneData.createRuntimeEntity();
        this.runtimeMapper.batchInsert(runtimeEntityList);

    }

    @Test
    public void test_queryClusterRuntimeOnClusterSpecifyByClusterId() {
        buildFullSceneData.build(ClusterType.EVENTMESH_CLUSTER);
        clusterMapper.batchInsert(buildFullSceneData.getClusterEntityList());
        clusterRelationshipMapper.batchClusterRelationshipEntry(buildFullSceneData.createClusterRelationshipEntity());
        runtimeMapper.batchInsert(buildFullSceneData.createRuntimeEntity());

        buildFullSceneData.getClusterTripleList().forEach(triple -> {
            final Set<Long> idSet =
                buildFullSceneData.getRuntimeEntityListByClusterFrameworkData(triple.getRight()).stream().map(RuntimeEntity::getClusterId)
                    .collect(Collectors.toSet());
            triple.getMiddle().forEach(entity -> {
                test_queryClusterRuntimeOnClusterSpecifyByClusterId(entity.getClusterEntity(),
                    triple.getLeft().getClusterType().getRuntimeClusterType(), idSet);
            });
            final Set<Long> idSet1 =
                buildFullSceneData.getRuntimeEntityListByClusterFrameworkData(triple.getMiddle()).stream().map(RuntimeEntity::getClusterId)
                    .collect(Collectors.toSet());
            triple.getRight().forEach(entity -> {
                test_queryClusterRuntimeOnClusterSpecifyByClusterId(entity.getClusterEntity(), triple.getLeft().getClusterType().getMetaClusterType(),
                    idSet1);
            });

        });
    }

    private void test_queryClusterRuntimeOnClusterSpecifyByClusterId(ClusterEntity clusterEntity, List<ClusterType> clusterTypeList,
        Set<Long> idSet) {
        QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO =
            QueryRuntimeByBigExpandClusterDO.builder()
                .followClusterId(clusterEntity.getId())
                .queryClusterTypeList(clusterTypeList).build();
        List<RuntimeEntity> runtimeEntityList = runtimeMapper.queryClusterRuntimeOnClusterSpecifyByClusterId(queryRuntimeByBigExpandClusterDO);
        Set<Long> neIdSet = runtimeEntityList.stream().map(RuntimeEntity::getClusterId).collect(Collectors.toSet());
        Assert.assertEquals(idSet, neIdSet);
    }

    @Test
    public void test_queryRuntimeByBigExpandCluster() {
        buildFullSceneData.build(ClusterType.EVENTMESH_CLUSTER);
        clusterMapper.batchInsert(buildFullSceneData.getClusterEntityList());
        clusterRelationshipMapper.batchClusterRelationshipEntry(buildFullSceneData.createClusterRelationshipEntity());
        runtimeMapper.batchInsert(buildFullSceneData.createRuntimeEntity());

        List<Triple<ClusterEntity, List<ClusterFrameworkData>, List<ClusterFrameworkData>>> triples =
            buildFullSceneData.getClusterTripleList(ClusterType.EVENTMESH_CLUSTER);
        List<RuntimeEntity> allEntityList = new ArrayList<>();
        triples.forEach(triple -> {
            AtomicReference<List<RuntimeEntity>> oldRef = new AtomicReference<>();
            triple.getRight().forEach(triple1 -> {
                QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO =
                    QueryRuntimeByBigExpandClusterDO.builder()
                        .followClusterId(triple1.getClusterEntity().getId())
                        .mainClusterType(ClusterType.EVENTMESH_CLUSTER)
                        .storageClusterTypeList(ClusterType.getStorageCluster())
                        .storageMetaClusterTypeList(ClusterType.getStorageMetaCluster())
                        .build();
                List<RuntimeEntity> runtimeEntityList = runtimeMapper.queryRuntimeByBigExpandCluster(queryRuntimeByBigExpandClusterDO);
                allEntityList.addAll(runtimeEntityList);
                if (Objects.nonNull(oldRef.get())) {
                    Assert.assertEquals(oldRef.get(), runtimeEntityList);
                }
                oldRef.set(runtimeEntityList);
            });

        });


    }


    @Test
    public void test_queryRuntimeByClusterId() {
        buildFullSceneData.build(ClusterType.EVENTMESH_CLUSTER);
        clusterMapper.batchInsert(buildFullSceneData.getClusterEntityList());

        List<RuntimeEntity> runtimeEntityList = buildFullSceneData.createRuntimeEntity();
        runtimeMapper.batchInsert(runtimeEntityList);
        AtomicInteger atomicInteger = new AtomicInteger();
        List<RuntimeEntity> list = new ArrayList<>();
        buildFullSceneData.getClusterTripleList().forEach(triple -> {
            List<ClusterEntity> clusterEntityList = new ArrayList<>();
            triple.getMiddle().forEach(clusterFrameworkData -> {
                clusterFrameworkData.addClusterEntity(clusterEntityList);
            });
            triple.getRight().forEach(clusterFrameworkData -> {
                clusterFrameworkData.addClusterEntity(clusterEntityList);
            });
            List<RuntimeEntity> runtimeEntityList1 = this.runtimeMapper.queryRuntimeByClusterId(clusterEntityList);
            list.addAll(runtimeEntityList1);
        });
        Assert.assertEquals(list.size(), runtimeEntityList.size());
    }


    @Test
    public void test_batch() {
        ClusterEntity clusterEntity = ClusterEntityMapperTest.createClusterEntity();
        clusterEntity.setId(1L);
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RuntimeEntity runtimeEntity = createRuntimeEntity(clusterEntity);
            runtimeEntityList.add(runtimeEntity);
        }
        this.runtimeMapper.batchInsert(runtimeEntityList);
        runtimeEntityList = this.runtimeMapper.queryAll();
        Assert.assertEquals(10, runtimeEntityList.size());
    }

    @Test
    public void test_insert() {
        ClusterEntity clusterEntity = ClusterEntityMapperTest.createClusterEntity();
        clusterEntity.setId(1L);
        RuntimeEntity runtimeEntity = createRuntimeEntity(clusterEntity);
        runtimeEntity.setClusterId(clusterEntity.getId());
        this.runtimeMapper.insertRuntime(runtimeEntity);
        runtimeEntity = this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);

        Assert.assertNotNull(runtimeEntity);
        this.runtimeMapper.insertRuntime(runtimeEntity);
        RuntimeEntity newRuntimeEntity = this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);
        // TODO h2 不支持 MySQL ON DUPLICATE KEY UPDATE
        //Assert.assertNotEquals(runtimeEntity.getOnlineTimestamp(), newRuntimeEntity.getOnlineTimestamp());
    }

    public static RuntimeEntity createRuntimeEntity(ClusterEntity clusterEntity) {
        AtomicLong nameIndex = aliasIndex.computeIfAbsent(clusterEntity.getId(), k -> new AtomicLong(2));
        RuntimeEntity runtimeEntity = ClusterDataMapperTest.INSTANCE.toRuntimeEntity(clusterEntity);
        runtimeEntity.setClusterId(clusterEntity.getId());
        runtimeEntity.setName(nameIndex.incrementAndGet() + "-----runtime");
        runtimeEntity.setHost(nameIndex.incrementAndGet() + "");
        runtimeEntity.setPort((int) nameIndex.get());
        runtimeEntity.setJmxPort((int) nameIndex.get());
        runtimeEntity.setKubernetesClusterId(nameIndex.get());
        return runtimeEntity;
    }

}
