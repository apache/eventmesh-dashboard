/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateRuntimeDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEntireClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEventClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEventClusterDTO.CreateCapStorageClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEventClusterDTO.RuntimeClusterDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson2.JSON;

import lombok.Builder;
import lombok.Data;

public class ActiveCreateControllerTest {

    private static final List<ClusterTrusteeshipType> clusterTrusteeshipArrangeType =
        List.of(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE, ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE,
            ClusterTrusteeshipType.NO_TRUSTEESHIP);

    private static final List<FirstToWhom> firstToWhomArrangeType = List.of(FirstToWhom.DASHBOARD, FirstToWhom.RUNTIME, FirstToWhom.NOT);

    private static final List<Matrix> matrixList = new ArrayList<>();

    private static Matrix matrix = new Matrix(ClusterTrusteeshipType.TRUSTEESHIP, FirstToWhom.DASHBOARD);


    private static final ClusterGroup eventGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.EVENTMESH_CLUSTER).brokerClusterType(ClusterType.EVENTMESH_RUNTIME)
            .metaClusterType(ClusterType.EVENTMESH_META_NACOS).build();

    private static final ClusterGroup rocketMQGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER).brokerClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER)
            .metaClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER).build();

    private static final ClusterGroup kafkaGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.STORAGE_KAFKA_CLUSTER).brokerClusterType(ClusterType.STORAGE_KAFKA_BROKER)
            .metaClusterType(ClusterType.STORAGE_KAFKA_ZK).build();

    private static final ClusterGroup eventJvmGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.EVENTMESH_JVM_CLUSTER).brokerClusterType(ClusterType.EVENTMESH_JVM_RUNTIME)
            .metaClusterType(ClusterType.EVENTMESH_JVM_META).build();

    private static final ClusterGroup jvmStorageGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.STORAGE_JVM_CLUSTER).brokerClusterType(ClusterType.STORAGE_JVM_BROKER)
            .metaClusterType(ClusterType.STORAGE_JVM_META).build();

    private static final ClusterGroup jvmCapStorageGroup =
        ClusterGroup.builder().mainClusterType(ClusterType.STORAGE_JVM_CAP_CLUSTER).brokerClusterType(ClusterType.STORAGE_JVM_CAP_BROKER)
            .metaClusterType(ClusterType.STORAGE_JVM_CAP_META).build();


    private ExecuteData executeData;



    /**
     * 创建四套 clusterType 1. jvm cap 2. jvm main 3. kafka cap 4. rocketmq main 5. jvm eventmesh 6. eventmesh
     */
    @Before
    public void init() {
        clusterTrusteeshipArrangeType.forEach(clusterTrusteeshipType -> {
            firstToWhomArrangeType.forEach(firstToWhomType -> {
                matrixList.add(Matrix.builder().clusterTrusteeshipType(clusterTrusteeshipType).firstToWhom(firstToWhomType).build());
            });
        });
    }


    @Test
    public void test_allJvm() {
        this.executeData = ExecuteData.builder().matrix(matrix).eventmeshClusterGroup(eventJvmGroup).storageClusterGroup(jvmStorageGroup).build();
        CreateTheEventClusterDTO createTheEventClusterDTO = this.createTheEntireCluster();
        String string = JSON.toJSONString(createTheEventClusterDTO);
        System.out.println(string);
    }

    @Test
    public void test_allJvm_all_matrix() {
        matrixList.forEach(value -> {
            matrix = value;
            this.test_allJvm();
        });
    }

    @Test
    public void test_allCapJvm() {
        this.executeData = ExecuteData.builder().matrix(matrix).eventmeshClusterGroup(eventJvmGroup).storageClusterGroup(jvmCapStorageGroup).build();
        this.createTheEntireCluster();
    }

    @Test
    public void test_rocketmq() {
        this.executeData = ExecuteData.builder().matrix(matrix).eventmeshClusterGroup(eventJvmGroup).storageClusterGroup(rocketMQGroup).build();
        this.createTheEntireCluster();
    }

    @Test
    public void test_kafka() {
        this.executeData = ExecuteData.builder().matrix(matrix).eventmeshClusterGroup(eventJvmGroup).storageClusterGroup(kafkaGroup).build();
        this.createTheEntireCluster();
    }


    /**
     * TODO eventmesh 不支持 topic 路由，不支持多 storage meta ，所以 cap
     *       支持 一个 eventmesh 集群对应一个 cap 集群
     *       支持 多个 eventmesh 集群对应一个 cap 集群
     *       cap 集群模式的 托管校验，只能用 一对一的模式
     */
    public void inp_cap_createTheEventCluster() {

    }

    /**
     * TODO 构建 主从集群。 只要 eventmesh 配置 存储集群的 meta 地址就行了
     *      支持
     *      一个 eventmesh 集群对应 一个 主从集群
     *      一个 eventmesh 集群对应 多个 主从集群
     *      多个 eventmesh 集群对应 1个 主从集群
     *      多个 eventmesh 集群对应 多个 注册集群
     */
    public void inp_main_createTheEventCluster() {
        CreateTheEventClusterDTO createTheEventClusterDTO = new CreateTheEventClusterDTO();
    }

    public CreateTheEventClusterDTO createTheEntireCluster() {
        CreateTheEventClusterDTO createTheEventClusterDTO = this.createTheEventClusterDTO();
        createTheEventClusterDTO.setEventClusterList(this.createMainClusterDTO(this.executeData.getEventmeshClusterGroup()));

        ClusterFramework clusterFramework =
            ClusterSyncMetadataEnum.getClusterFramework(this.executeData.getStorageClusterGroup().getMainClusterType());
        if (clusterFramework.isCAP()) {
            createTheEventClusterDTO.setCapStorageClusters(this.createCapStorageClusterDTO(this.executeData.getStorageClusterGroup()));
        } else {
            createTheEventClusterDTO.setMonomerStorageClusters(this.createMainClusterDTO(this.executeData.getStorageClusterGroup()));
        }
        return createTheEventClusterDTO;
    }


    private CreateTheEventClusterDTO createTheEventClusterDTO() {
        CreateTheEventClusterDTO createTheEventClusterDTO = new CreateTheEventClusterDTO();
        createTheEventClusterDTO.setOrganizationId(1L);
        // 构建 eventmesh space
        CreateClusterDTO eventSpaceDTO = new CreateClusterDTO();
        eventSpaceDTO.setOrganizationId(1L);
        eventSpaceDTO.setClusterType(ClusterType.EVENTMESH);
        eventSpaceDTO.setName("event-space-" + System.nanoTime());
        eventSpaceDTO.setDescription("event-space");
        createTheEventClusterDTO.setEventSpace(eventSpaceDTO);
        return createTheEventClusterDTO;
    }


    private RuntimeClusterDTO createMainClusterDTO(ClusterGroup eventGroup) {
        RuntimeClusterDTO runtimeClusterDTO = new RuntimeClusterDTO();
        runtimeClusterDTO.setCreateClusterDTO(this.createClusterDTO(eventGroup, matrix, eventGroup.getMainClusterType()));

        CLusterBuildData cLusterBuildData = CLusterBuildData.builder().clusterType(eventGroup.getBrokerClusterType()).brokerCount(10).build();

        runtimeClusterDTO.setBrokerClusterList(this.buildOneSingleCluster(cLusterBuildData));
        cLusterBuildData = CLusterBuildData.builder().clusterType(eventGroup.getMetaClusterType()).brokerCount(10).build();
        runtimeClusterDTO.setMetaClusterList(this.buildOneSingleCluster(cLusterBuildData));
        return runtimeClusterDTO;
    }


    private CreateCapStorageClusterDTO createCapStorageClusterDTO(ClusterGroup eventGroup) {
        CreateCapStorageClusterDTO createCapStorageClusterDTO = new CreateCapStorageClusterDTO();

        CreateClusterDTO createClusterDTO = new CreateClusterDTO();
        createCapStorageClusterDTO.setCreateClusterDTO(createClusterDTO);

        CLusterBuildData cLusterBuildData = CLusterBuildData.builder().clusterType(eventGroup.getBrokerClusterType()).brokerCount(10).build();
        createCapStorageClusterDTO.setBrokerClusterList(this.buildOneSingleCluster(cLusterBuildData));

        cLusterBuildData = CLusterBuildData.builder().clusterType(eventGroup.getMetaClusterType()).brokerCount(3).build();
        createCapStorageClusterDTO.setMetaClusterList(this.buildOneSingleCluster(cLusterBuildData));

        CreateRuntimeDTO createRuntimeDTO = new CreateRuntimeDTO();
        createCapStorageClusterDTO.setPrometheusRuntime(createRuntimeDTO);

        return createCapStorageClusterDTO;
    }

    private CreateClusterDTO createClusterDTO(ClusterGroup eventGroup, Matrix matrix, ClusterType clusterType) {
        CreateClusterDTO eventCluster = new CreateClusterDTO();
        eventCluster.setClusterType(clusterType);
        eventCluster.setName(clusterType.name() + "-" + System.nanoTime());
        eventCluster.setDescription("event-cluster");
        eventCluster.setTrusteeshipArrangeType(matrix.getClusterTrusteeshipType());
        eventCluster.setFirstToWhom(matrix.getFirstToWhom());
        return eventCluster;
    }

    private CreateRuntimeDTO createRuntimeDTO(CLusterBuildData cLusterBuildData ,ReplicationType replicationType, int index) {
        CreateRuntimeDTO createRuntimeDTO = new CreateRuntimeDTO();
        if(Objects.nonNull(cLusterBuildData.getExecuteData())){

        }else {
            createRuntimeDTO.setHost("127.0.0."+index);
            createRuntimeDTO.setPort(8080);
            createRuntimeDTO.setName(cLusterBuildData.getClusterType() + "-" + index + "-" + System.nanoTime());
            createRuntimeDTO.setReplicationType(replicationType);
        }
        return createRuntimeDTO;
    }


    private CreateTheEntireClusterDTO buildOneSingleCluster(CLusterBuildData buildData) {
        return this.buildSingleCluster(buildData).get(0);
    }

    private List<CreateTheEntireClusterDTO> buildSingleCluster(CLusterBuildData buildData) {
        List<CreateTheEntireClusterDTO> createTheEntireClusterDTOList = new ArrayList<>();
        for (int j = 0; j <= buildData.getClusterCount(); j++) {
            CreateTheEntireClusterDTO createTheEntireClusterDTO = new CreateTheEntireClusterDTO();
            createTheEntireClusterDTOList.add(createTheEntireClusterDTO);
            createTheEntireClusterDTO.setCreateClusterDTO(this.createClusterDTO(null, matrix, buildData.getClusterType()));

            List<CreateRuntimeDTO> createRuntimeDTOList = new ArrayList<>();
            createTheEntireClusterDTO.setCreateRuntimeDTOList(createRuntimeDTOList);
            for (int i = 0; i <= buildData.getBrokerCount(); i++) {
                createRuntimeDTOList.add(this.createRuntimeDTO(buildData,ReplicationType.NOT, i));
            }
        }
        return createTheEntireClusterDTOList;
    }


    @Data
    @Builder
    static class CLusterBuildData {

        private ClusterType clusterType;

        private int clusterCount = 1;

        private int brokerCount = 1;

        private ExecuteData executeData;
    }

    @Data
    @Builder
    static class Matrix {

        private ClusterTrusteeshipType clusterTrusteeshipType;

        private FirstToWhom firstToWhom;
    }


    @Data
    @Builder
    static class ClusterGroup {

        private ClusterType mainClusterType = ClusterType.EVENTMESH_JVM_CLUSTER;

        private ClusterType brokerClusterType = ClusterType.EVENTMESH_JVM_RUNTIME;

        private ClusterType metaClusterType = ClusterType.EVENTMESH_JVM_META;
    }


    @Data
    @Builder
    static class ExecuteData {

        private Matrix matrix;

        private ClusterGroup eventmeshClusterGroup;

        private ClusterGroup storageClusterGroup;
    }

}
