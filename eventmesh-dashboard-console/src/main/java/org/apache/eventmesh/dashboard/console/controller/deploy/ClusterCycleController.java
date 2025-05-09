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


package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateClusterByDeployScriptHandler;
import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateRuntimeByDeployScriptHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.cluster.VerifyNameDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByEventMesh;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByDeployScriptDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByOnlyDataDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 1. 用户首页列表
 * <p>
 * 2. 集群首页概要 kubernetes 集群在 eventmesh 集群里面创建的 只属于这个集群 kubernetes在创建的时候可以设为独立集群 如果创建集群
 * <p>
 * 1. 全托管创建 因为全托管创建，不需要用户管理任何东西。
 * <p>
 * 2. 全托管共享集群 创建时绑定集群流程
 * <p>
 * 1.创建 kubernetes 集群
 * <p>
 * 2. 创建 storage 集群
 * <p>
 * 3. 创建 meta 集群
 * <p>
 * 4. 创建 runtime 集群
 * <p>
 * 5. 创建时 进行绑定 3. 已经 deploy config 创建，比较麻烦。这种 kubernetes 资源不会共享
 * <p>
 * 1. 提供 kubernetes集群配置，多个
 * <p>
 * 2. 选择 storage deploy config  or  storage id or name
 * <p>
 * 3. 选择 meta deploy config or  meta id or name
 * <p>
 * 4. 选择 runtime deploy config or runtime idor name
 * <p>
 * 5. 点击 创建，提供部署流程 4. 先创建，后绑定
 * <p>
 * 1. 直接创建
 * <p>
 * 2. 在内部进行绑定 5. 配置一个kubernetes集群，可以定时校验 deploy config 的效果。校验的目的是什么 创建节点的时候，先看自己生是否有 kubernetes。 然后检查上级cluster 是否有 可用的 kubernetes
 */
@RestController
@RequestMapping("organization/clusterCycleDeploy")
public class ClusterCycleController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private CreateRuntimeByDeployScriptHandler createRuntimeByDeployScriptHandler;


    @Autowired
    private CreateClusterByDeployScriptHandler createClusterByDeployScriptHandler;


    /**
     * @param verifyNameDTO
     * @return
     */
    public String verifyName(VerifyNameDTO verifyNameDTO) {

        return "";
    }

    @PostMapping("createRuntimeByOnlyDataHandler")
    public void createRuntimeByOnlyDataHandler(@RequestBody @Validated CreateRuntimeByOnlyDataDO createRuntimeByOnlyDataDO) {
        RuntimeEntity runtimeEntity = ClusterCycleControllerMapper.INSTANCE.createRuntimeByOnlyDataHandler(createRuntimeByOnlyDataDO);
        runtimeService.insertRuntime(runtimeEntity);
    }

    @PostMapping("createRuntimeByDeployScript")
    public void createRuntimeByDeployScript(@RequestBody @Validated CreateRuntimeByDeployScriptDTO createRuntimeByDeployScriptDTO) {
        this.createRuntimeByDeployScriptHandler.handler(createRuntimeByDeployScriptDTO);
    }

    @PostMapping("createClusterByDeployScript")
    public void createClusterByDeployScript(@RequestBody @Validated CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        this.createClusterByDeployScriptHandler.handler(createClusterByDeployScriptDO);
    }

    /**
     * 分 eventmesh 集群创建
     *
     * @param createClusterByEventMesh
     */
    @PostMapping("createEventMeshClusterByOnlyData")
    public Long createEventMeshClusterByOnlyData(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
        ClusterEntity clusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByEventMesh(createClusterByEventMesh);
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntity.setClusterOwnType(ClusterOwnType.NOT);
        clusterEntity.setAuthType("");
        clusterEntity.setVersion("");
        clusterEntity.setRuntimeIndex(0);
        clusterEntity.setTrusteeshipType(ClusterTrusteeshipType.NOT);
        clusterEntity.setFirstToWhom(FirstToWhom.NOT);
        clusterEntity.setDeployStatusType(DeployStatusType.CREATE_SUCCESS);
        clusterEntity.setResourcesConfigId(0L);
        clusterEntity.setDeployScriptId(0L);
        clusterEntity.setDeployScriptName("");
        clusterEntity.setDeployScriptVersion("");
        clusterService.insertCluster(clusterEntity);
        return clusterEntity.getId();
    }

    @PostMapping("createClusterByEventMesh")
    public Long createClusterByEventMesh(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
        ClusterEntity clusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByEventMesh(createClusterByEventMesh);
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntity.setClusterOwnType(ClusterOwnType.INDEPENDENCE);
        clusterEntity.setAuthType("");
        clusterEntity.setVersion("");
        clusterEntity.setRuntimeIndex(0);
        clusterEntity.setDeployStatusType(DeployStatusType.CREATE_SUCCESS);
        clusterEntity.setResourcesConfigId(0L);
        clusterEntity.setDeployScriptId(0L);
        clusterEntity.setDeployScriptName("");
        clusterEntity.setDeployScriptVersion("");
        ClusterRelationshipEntity relationshipEntity = new ClusterRelationshipEntity();
        clusterService.insertClusterAndRelationship(clusterEntity, relationshipEntity);
        return clusterEntity.getId();

    }


    @PostMapping("pauseCluster")
    public void pauseCluster(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

    @PostMapping("pauseRuntime")
    public void pauseRuntime(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

    @PostMapping("relationship")
    public void relationship(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

    @PostMapping("unrelationship")
    public void unrelationship(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

    @PostMapping("uninstallCluster")
    public void uninstallCluster(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

    @PostMapping("uninstallRuntime")
    public void uninstallRuntime(@RequestBody @Validated CreateClusterByEventMesh createClusterByEventMesh) {
    }

}
