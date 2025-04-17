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


package org.apache.eventmesh.dashboard.console.model.domain;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain.DataHandler;
import org.apache.eventmesh.dashboard.console.domain.metadata.MetadataAllDO;
import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.core.cluster.ClusterBaseDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeBaseDO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

public class ClusterMetadataDomainTest {

    public ClusterMetadataDomain clusterMetadataDomain = new ClusterMetadataDomain();

    private MetadataAllDO metadataAllDO;

    private MetadataAllDO deleteMetadataAllDO;

    private ClusterEntity clusterEntity = new ClusterEntity();

    private List<ClusterEntity> clusterEntityList = new ArrayList<>();

    private List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();

    private List<RuntimeEntity> runtimeEntityList = new ArrayList<>();

    private AtomicLong atomicLong = new AtomicLong(1L);


    @Before
    public void init() {
        clusterMetadataDomain.rootClusterDHO();
        clusterMetadataDomain.useBuildConfig();
        clusterMetadataDomain.isConsoleModel();
        clusterMetadataDomain.setHandler(new DataHandler() {
            @Override
            public void registerRuntime(RuntimeEntity runtimeEntity, RuntimeBaseDO runtimeBaseDO, ColonyDO colonyDO) {
                ClusterType clusterType = runtimeEntity.getClusterType();
                System.out.println("registerRuntime clusterType : " + clusterType);
            }

            @Override
            public void unRegisterRuntime(RuntimeEntity runtimeEntity, RuntimeBaseDO runtimeBaseDO, ColonyDO colonyDO) {
                ClusterType clusterType = runtimeEntity.getClusterType();
                System.out.println("unRegisterRuntime clusterType : " + clusterType);
            }

            @Override
            public void registerCluster(ClusterEntity clusterEntity, ClusterBaseDO clusterBaseDO, ColonyDO colonyDO) {
                ClusterType clusterType = clusterEntity.getClusterType();
                System.out.println("registerCluster clusterType : " + clusterType);
            }

            @Override
            public void unRegisterCluster(ClusterEntity clusterEntity, ClusterBaseDO clusterBaseDO, ColonyDO colonyDO) {
                ClusterType clusterType = clusterEntity.getClusterType();
                System.out.println("clusterType clusterType : " + clusterType);
            }
        });
        metadataAllDO =
            MetadataAllDO.builder().clusterEntityList(clusterEntityList).clusterRelationshipEntityList(clusterRelationshipEntityList)
                .runtimeEntityList(runtimeEntityList).build();
        this.test_EventMesh_Cluster();
    }

    public Long getId() {
        return atomicLong.getAndIncrement();
    }

    private void handler() {
        this.clusterMetadataDomain.handlerMetadata(this.metadataAllDO);
        this.randomDelete();
    }

    private void randomDelete() {

        deleteMetadataAllDO =
            MetadataAllDO.builder()
                .runtimeEntityList(this.deleteEntity(this.runtimeEntityList)).build();
        this.clusterMetadataDomain.handlerMetadata(this.deleteMetadataAllDO);

        deleteMetadataAllDO =
            MetadataAllDO.builder()
                .clusterEntityList(this.deleteEntity(this.clusterEntityList)).build();
        this.clusterMetadataDomain.handlerMetadata(this.deleteMetadataAllDO);

    }

    private <T> T deleteEntity(Object object) {
        List<BaseIdEntity> baseIdEntities = (List<BaseIdEntity>) object;
        Random random = new Random();
        List<BaseIdEntity> deleteBaseIdEntity = new ArrayList<>();
        baseIdEntities.forEach((value) -> {
            if (random.nextInt(100) < 10) {
                value.setStatus(0L);
                deleteBaseIdEntity.add(value);
            }
        });
        return (T) deleteBaseIdEntity;
    }

    private void createClusterRelationshipEntity(ClusterEntity mainCluster, ClusterEntity clusterEntity) {
        ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterRelationshipEntity.setClusterId(mainCluster.getId());
        clusterRelationshipEntity.setClusterType(mainCluster.getClusterType());

        clusterRelationshipEntity.setRelationshipId(clusterEntity.getId());
        clusterRelationshipEntity.setRelationshipType(clusterEntity.getClusterType());
        this.clusterRelationshipEntityList.add(clusterRelationshipEntity);
    }

    @Test
    public void test_EventMesh_Cluster() {

        clusterEntity.setId(this.getId());
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntityList.add(clusterEntity);

        this.createCluster(clusterEntity, ClusterType.EVENTMESH_META_NACOS);
        this.createCluster(clusterEntity, ClusterType.EVENTMESH_META_NACOS);

        this.createCluster(clusterEntity, ClusterType.EVENTMESH_RUNTIME);
        this.createCluster(clusterEntity, ClusterType.EVENTMESH_RUNTIME);

    }

    public void createRuntime(ClusterEntity cluster) {
        for (int i = 0; i < 5; i++) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setId(this.getId());
            runtimeEntity.setClusterId(cluster.getId());
            runtimeEntity.setClusterType(cluster.getClusterType());
            runtimeEntity.setStatus(1L);
            this.runtimeEntityList.add(runtimeEntity);
        }
    }

    @Test
    public void test_RockerMQ_Cluster() {
        ClusterEntity clusterEntity = this.createCluster(this.clusterEntity, ClusterType.STORAGE_ROCKETMQ_CLUSTER);

        this.createCluster(clusterEntity, ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        this.createCluster(clusterEntity, ClusterType.STORAGE_ROCKETMQ_NAMESERVER);

        // 创建 broker cluster

        ClusterEntity runtimeCluster = this.createCluster(clusterEntity, ClusterType.STORAGE_ROCKETMQ_BROKER);
        this.createCluster(runtimeCluster, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        this.createCluster(runtimeCluster, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);

        // 创建 broker cluster
        runtimeCluster = this.createCluster(clusterEntity, ClusterType.STORAGE_ROCKETMQ_BROKER);
        this.createCluster(runtimeCluster, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        this.createCluster(runtimeCluster, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);

        this.handler();

    }

    @Test
    public void test_Kafka_Cluster() {
        ClusterEntity clusterEntity = this.createCluster(this.clusterEntity, ClusterType.STORAGE_KAFKA_CLUSTER);

        this.createCluster(clusterEntity, ClusterType.STORAGE_KAFKA_ZK);

        // 创建 broker cluster
        this.createCluster(clusterEntity, ClusterType.STORAGE_KAFKA_BROKER);
        this.handler();
    }

    public ClusterEntity createCluster(ClusterEntity mainCluster, ClusterType clusterType) {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(this.getId());
        clusterEntity.setClusterType(clusterType);
        clusterEntity.setStatus(1L);
        clusterEntityList.add(clusterEntity);
        this.createClusterRelationshipEntity(mainCluster, clusterEntity);
        if (!Objects.equals(clusterType.getAssemblyBusiness(), ClusterType.DEFINITION)) {
            this.createRuntime(clusterEntity);
        }
        return clusterEntity;
    }

    private static class TestDataWrapper {

    }
}
