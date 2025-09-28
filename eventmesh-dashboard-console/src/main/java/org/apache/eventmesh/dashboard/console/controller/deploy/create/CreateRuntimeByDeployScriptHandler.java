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


package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByDeployScriptDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CreateRuntimeByDeployScriptHandler implements UpdateHandler<CreateRuntimeByDeployScriptDTO> {


    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void init() {

    }

    /**
     * 支持 多 复杂的主从架构
     */
    @Override
    public void handler(CreateRuntimeByDeployScriptDTO createRuntimeByDeployScriptDTO) {
        ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(createRuntimeByDeployScriptDTO);
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        RuntimeEntity runtimeEntity = ClusterCycleControllerMapper.INSTANCE.createRuntimeByDeployScript(createRuntimeByDeployScriptDTO);
        if (Objects.isNull(runtimeEntity.getDeployScriptId()) && Objects.isNull(clusterEntity.getDeployScriptId())) {
            log.error("create runtime by deploy script id is null");
            return;
        }

        if (Objects.isNull(runtimeEntity.getResourcesConfigId()) && Objects.isNull(clusterEntity.getResourcesConfigId())) {
            log.error("create runtime by deploy script resources config id is null");
            return;
        }
        if (Objects.nonNull(runtimeEntity.getDeployScriptId())) {
            runtimeEntity.setDeployScriptId(createRuntimeByDeployScriptDTO.getDeployScriptId());
        }

        if (Objects.nonNull(runtimeEntity.getResourcesConfigId())) {
            runtimeEntity.setResourcesConfigId(createRuntimeByDeployScriptDTO.getResourcesConfigId());
        }
        ReplicationType replicationType = createRuntimeByDeployScriptDTO.getReplicationType();
        Deque<Integer> linkedList = new ArrayDeque<>();
        if (!Objects.equals(replicationType, ReplicationType.SLAVE) && clusterEntity.getClusterType().isStorage()) {
            clusterEntity.setRuntimeIndex(1);
            linkedList = this.clusterService.getIndex(clusterEntity);
        } else if (Objects.equals(replicationType, ReplicationType.SLAVE)) {
            linkedList.add(1);
        } else {
            linkedList.add(0);
        }

        runtimeEntity.setClusterType(clusterEntity.getClusterType());
        runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        runtimeEntity.setReplicationType(replicationType);
        runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_DATA_ING);
        runtimeEntity.setRuntimeIndex(linkedList.poll());

        this.runtimeService.insertRuntimeByClusterData(runtimeEntity);

    }

}
