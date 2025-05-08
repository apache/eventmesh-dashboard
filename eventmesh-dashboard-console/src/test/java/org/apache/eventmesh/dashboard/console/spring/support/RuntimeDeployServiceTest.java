/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;
import org.apache.eventmesh.dashboard.console.service.deploy.PortService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.console.spring.support.RuntimeDeployService.AbstractRuntimeServiceTask;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.instrument.util.IOUtils;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeDeployServiceTest {


    private RuntimeDeployService runtimeDeployService = new RuntimeDeployService();


    @Mock
    private ClusterService clusterService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ClusterRelationshipService clusterRelationshipService;

    @Mock
    private DeployScriptService deployScriptService;

    @Mock
    private ResourcesConfigService resourcesConfigService;

    @Mock
    private ConfigService configService;


    private RuntimeEntity runtimeEntity = new RuntimeEntity();

    private MockedStatic<SDKManage> sdkManageMockedStatic;

    @Mock
    private SDKManage sdkManageMock;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private PortService portService;


    @Before
    public void init() throws IllegalAccessException {
        runtimeEntity.setClusterId(10L);
        runtimeEntity.setId(10000L);

        FieldUtils.writeField(runtimeDeployService, "clusterService", clusterService, true);
        FieldUtils.writeField(runtimeDeployService, "runtimeService", runtimeService, true);
        FieldUtils.writeField(runtimeDeployService, "clusterRelationshipService", clusterRelationshipService, true);
        FieldUtils.writeField(runtimeDeployService, "deployScriptService", deployScriptService, true);
        FieldUtils.writeField(runtimeDeployService, "resourcesConfigService", resourcesConfigService, true);
        FieldUtils.writeField(runtimeDeployService, "configService", configService, true);
        FieldUtils.writeField(runtimeDeployService, "portService", portService, true);

        sdkManageMockedStatic = Mockito.mockStatic(SDKManage.class);
        sdkManageMockedStatic.when(SDKManage::getInstance).thenReturn(sdkManageMock);

        Mockito.when(sdkManageMock.getClient(Mockito.any(), Mockito.any())).thenReturn(kubernetesClient);


    }


    @Test
    public void test_CREATE_WAIT() throws IllegalAccessException {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(1L);
        Mockito.when(clusterService.queryClusterById(Mockito.any())).thenReturn(clusterEntity);

        ClusterEntity kubeClusterEntity = new ClusterEntity();
        kubeClusterEntity.setId(2L);
        Mockito.when(clusterService.queryRelationshipClusterByClusterIdAndType(Mockito.any())).thenReturn(kubeClusterEntity);

        ResourcesConfigEntity resourcesConfigEntity = new ResourcesConfigEntity();
        resourcesConfigEntity.setCpuNum(1F);
        resourcesConfigEntity.setMemNum(2F);
        resourcesConfigEntity.setDiskNum(3F);
        Mockito.when(resourcesConfigService.queryResourcesById(Mockito.any())).thenReturn(resourcesConfigEntity);

        DeployScriptEntity deployScriptEntity = new DeployScriptEntity();
        deployScriptEntity.setId(1L);
        String content = IOUtils.toString(RuntimeDeployServiceTest.class.getResourceAsStream("/kubernetes/EventMesh-runtime.yaml"));
        deployScriptEntity.setContent(content);
        Mockito.when(deployScriptService.queryById(Mockito.any())).thenReturn(deployScriptEntity);

        Mockito.when(portService.getPorts(Mockito.any())).thenReturn(List.of(100 + "", 101 + "", 102 + "", 103 + ""));

        List<ConfigEntity> configEntityList = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setConfigName("config" + i);
            configEntity.setConfigValue("config" + i);
            configEntityList.add(configEntity);
        }
        Mockito.when(configService.queryByClusterAndInstanceId(Mockito.any())).thenReturn(configEntityList);
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(10L);
            runtimeEntity.setId(1L);
            runtimeEntity.setClusterType(ClusterType.EVENTMESH);
            runtimeEntityList.add(runtimeEntity);
        }
        Mockito.when(runtimeService.queryMetaRuntimeByStorageClusterId(Mockito.any())).thenReturn(runtimeEntityList);

        runtimeEntity.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_WAIT);
        AbstractRuntimeServiceTask abstractRuntimeServiceTask = runtimeDeployService.createTask(runtimeEntity);
        FieldUtils.writeField(abstractRuntimeServiceTask, "runtimeEntity", runtimeEntity, true);
        abstractRuntimeServiceTask.run();
    }

    @Test
    public void test_CREATE_UNINSTALL() throws IllegalAccessException {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(1L);
        Mockito.when(clusterService.queryClusterById(Mockito.any())).thenReturn(clusterEntity);

        ClusterEntity kubeClusterEntity = new ClusterEntity();
        kubeClusterEntity.setId(2L);
        Mockito.when(clusterService.queryRelationshipClusterByClusterIdAndType(Mockito.any())).thenReturn(kubeClusterEntity);

        runtimeEntity.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        runtimeEntity.setDeployStatusType(DeployStatusType.UNINSTALL);
        AbstractRuntimeServiceTask abstractRuntimeServiceTask = runtimeDeployService.createTask(runtimeEntity);
        FieldUtils.writeField(abstractRuntimeServiceTask, "runtimeEntity", runtimeEntity, true);
        abstractRuntimeServiceTask.run();
    }
}
