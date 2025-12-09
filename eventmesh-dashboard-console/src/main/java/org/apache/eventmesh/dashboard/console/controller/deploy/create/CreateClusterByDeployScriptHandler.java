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

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.model.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateClusterByDeployScriptHandler implements UpdateHandler<CreateClusterByDeployScriptDO> {

    private final List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
    private ClusterService clusterService;
    private ConfigService configService;
    private RuntimeService runtimeService;
    private ClusterRelationshipService clusterRelationshipService;
    private ClusterEntity clusterEntity;
    private ClusterFramework clusterFramework;
    private ClusterType clusterType;
    private ReplicationType replicationType;

    @Override
    public void init() {

    }

    private void handlerMetadata(ClusterEntity clusterEntity) {
        this.clusterType = clusterEntity.getClusterType();
        this.replicationType = clusterEntity.getReplicationType();
        this.clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType());
    }

    /**
     * 只支持 runtime 与 meta 集群的创建，不支持 集群空间的创建 </p> 如何支持 eventmesh space create， script 模式不适合。
     */
    @Override
    public void handler(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        this.clusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByDeployScript(createClusterByDeployScriptDO);

        this.clusterService.insertCluster(this.clusterEntity);
        this.handlerMetadata(this.clusterEntity);
        if (Objects.nonNull(createClusterByDeployScriptDO.getConfigGatherId())) {
            configService.copyConfig(createClusterByDeployScriptDO.getConfigGatherId(), this.clusterEntity.getId());
        }
        if (this.clusterFramework.isMainSlave()) {
            this.mainSlaveHandler(createClusterByDeployScriptDO, this.clusterEntity);
        } else {
            this.ordinaryRuntime(createClusterByDeployScriptDO);
        }

        this.runtimeService.batchInsert(this.runtimeEntityList);
    }

    private void ordinaryRuntime(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        Deque<Integer> linkedList = null;
        if (this.clusterType.isStorage()) {
            clusterEntity.setRuntimeIndex(createClusterByDeployScriptDO.getCreateNum());
            linkedList = this.clusterService.getIndex(this.clusterEntity);
        }
        for (int i = 0; i < createClusterByDeployScriptDO.getCreateNum(); i++) {
            this.createRuntimeEntity(this.clusterEntity, replicationType,
                Objects.isNull(linkedList) ? Integer.valueOf(0) : linkedList.pop());
        }
    }

    private void mainSlaveHandler(CreateClusterByDeployScriptDO createClusterByDeployScriptDO, ClusterEntity clusterEntity) {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        for (int i = 0; i < createClusterByDeployScriptDO.getCreateNum(); i++) {
            ClusterEntity newClusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByDeployScript(createClusterByDeployScriptDO);
            clusterEntityList.add(newClusterEntity);
        }
        this.clusterService.batchInsert(clusterEntityList, clusterEntity);
        clusterEntityList.forEach(entity -> {
            this.createRuntimeEntity(clusterEntity, ReplicationType.MAIN, 0);
            if (createClusterByDeployScriptDO.getReplicationType().isMainSlave()) {
                this.createRuntimeEntity(clusterEntity, ReplicationType.SLAVE, 1);
            }
        });
    }

    private void createRuntimeEntity(ClusterEntity clusterEntity, ReplicationType replicationType, int index) {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterEntity.getClusterId());
        runtimeEntity.setClusterType(clusterEntity.getClusterType());
        runtimeEntity.setDeployScriptId(clusterEntity.getDeployScriptId());
        runtimeEntity.setResourcesConfigId(clusterEntity.getResourcesConfigId());
        runtimeEntity.setFirstToWhom(FirstToWhom.NOT);
        runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        runtimeEntity.setName(clusterEntity.getName() + "_" + index);
        runtimeEntity.setHost("127.0.0.1");
        runtimeEntity.setPort(8080);
        runtimeEntity.setReplicationType(replicationType);
        runtimeEntity.setRuntimeIndex(index);
        runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_FULL_WAIT);
        this.runtimeEntityList.add(runtimeEntity);
    }

}
