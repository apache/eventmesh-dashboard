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

package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ActiveCreateControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateEventMeshSpaceDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateRuntimeDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEntireClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.active.CreateTheEventClusterDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("organization/activeCreate")
public class ActiveCreateController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterService clusterService;


    /**
     * 分 eventmesh 集群创建
     *
     * @param
     */
    @PostMapping("createEventMeshSpace")
    public Long createEventMeshSpace(@RequestBody @Validated CreateEventMeshSpaceDTO dto) {
        ClusterEntity clusterEntity = ActiveCreateControllerMapper.INSTANCE.createEventMeshSpace(dto);
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

    @PostMapping("createCluster")
    public Long createCluster(@RequestBody @Validated CreateClusterDTO dto) {
        ClusterEntity clusterEntity = ActiveCreateControllerMapper.INSTANCE.createCluster(dto);
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntity.setClusterOwnType(ClusterOwnType.INDEPENDENCE);
        clusterEntity.setAuthType("");
        clusterEntity.setVersion("");
        clusterEntity.setRuntimeIndex(Integer.valueOf(0));
        clusterEntity.setDeployStatusType(DeployStatusType.CREATE_SUCCESS);
        clusterEntity.setResourcesConfigId(0L);
        clusterEntity.setDeployScriptId(0L);
        clusterEntity.setDeployScriptName("");
        clusterEntity.setDeployScriptVersion("");
        ClusterRelationshipEntity relationshipEntity = new ClusterRelationshipEntity();
        clusterService.insertClusterAndRelationship(clusterEntity, relationshipEntity);
        return clusterEntity.getId();
    }

    @PostMapping("createRuntime")
    public void createRuntime(@RequestBody @Validated CreateRuntimeDTO dto) {
        RuntimeEntity runtimeEntity = ActiveCreateControllerMapper.INSTANCE.createRuntime(dto);
        runtimeService.insertRuntime(runtimeEntity);
    }


    @PostMapping("createTheEntireCluster")
    public Long createTheEntireCluster(@RequestBody @Validated CreateTheEntireClusterDTO dto) {
        ClusterEntity clusterEntity = ActiveCreateControllerMapper.INSTANCE.createCluster(dto.getCreateClusterDTO());

        List<RuntimeEntity> runtimeEntityList = ActiveCreateControllerMapper.INSTANCE.createRuntimeList(dto.getCreateRuntimeDTOList());

        ClusterRelationshipEntity relationshipEntity = new ClusterRelationshipEntity();
        relationshipEntity.setClusterId(clusterEntity.getId());
        this.clusterService.createTheEntireCluster(clusterEntity, relationshipEntity, runtimeEntityList);
        return 1L;
    }

    /**
     * TODO 先完成 数据 直接录入的实现，
     *      在完成 通过 API 调用获得 broker config 补充 broker 与 cluster 信息，以及 cluster 组织关系
     *      保留 两套机制，还是只留下 通过 API 获得信息的机制？
     * @param createTheEventClusterDTO
     * @return
     */
    @PostMapping("createTheEventCluster")
    public Long createTheEventCluster(@RequestBody CreateTheEventClusterDTO createTheEventClusterDTO) {
        ActiveCreateDTOHandler activeCreateDTOHandler = new ActiveCreateDTOHandler();
        activeCreateDTOHandler.handler(createTheEventClusterDTO);
        this.clusterService.createTheEventCluster(activeCreateDTOHandler.getClusterEntityList(),
            activeCreateDTOHandler.getClusterListRelationshipList(),activeCreateDTOHandler.getClusterAndRuntimeList());

        return activeCreateDTOHandler.getEventSapaceClusterEntity().getId();
    }
}
