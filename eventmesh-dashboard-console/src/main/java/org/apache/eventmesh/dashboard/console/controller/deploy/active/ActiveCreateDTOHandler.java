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

package org.apache.eventmesh.dashboard.console.controller.deploy.active;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseSyncEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ActiveCreateControllerMapper;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateRuntimeDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEntireClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEventClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEventClusterDTO.BaseCreateTheEntireClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEventClusterDTO.CreateCapStorageClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEventClusterDTO.MainStorageClusterDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.active.CreateTheEventClusterDTO.RuntimeClusterDTO;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public class ActiveCreateDTOHandler {

    private final List<ClusterEntity> clusterEntityList = new ArrayList<>();

    private final List<Pair<ClusterEntity, ClusterEntity>> clusterListRelationshipList = new ArrayList<>();

    private final List<Pair<ClusterEntity, List<RuntimeEntity>>> clusterAndRuntimeList = new ArrayList<>();

    private ClusterEntity eventSapaceClusterEntity;

    private Long organizationId;

    private String organizationName;

    private ClusterEntity createClusterEntity(CreateClusterDTO createClusterDTO) {
        ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.createClusterDTO(createClusterDTO);
        this.fillBaseSyncEntity(clusterEntity);
        clusterEntity.setClusterOwnType(ClusterOwnType.NOT);
        clusterEntity.setConfig("");
        clusterEntity.setAuthType("none");
        clusterEntity.setJmxProperties("");
        clusterEntity.setConfig("");
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType());
        if (clusterFramework.isCAP()) {
            clusterEntity.setReplicationType(ReplicationType.NOT);
        }
        this.clusterEntityList.add(clusterEntity);
        return clusterEntity;
    }

    private void fillBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
        if (Objects.isNull(baseSyncEntity.getTrusteeshipType())) {
            baseSyncEntity.setTrusteeshipType(ClusterTrusteeshipType.NO_TRUSTEESHIP);
        }
        if (Objects.isNull(baseSyncEntity.getFirstToWhom())) {
            baseSyncEntity.setFirstToWhom(FirstToWhom.NOT);
        }
        baseSyncEntity.setOrganizationId(organizationId);
        baseSyncEntity.setVersion("");
        baseSyncEntity.setReplicationType(ReplicationType.NOT);
        baseSyncEntity.setDeployStatusType(DeployStatusType.SETTLE);
        baseSyncEntity.setResourcesConfigId(0L);
        baseSyncEntity.setDeployScriptId(0L);
        baseSyncEntity.setDeployScriptName("");
        baseSyncEntity.setDeployScriptVersion("");
    }

    private RuntimeEntity createRuntimeEntity(ClusterEntity clusterEntity, CreateRuntimeDTO createRuntimeDTO) {
        return ActiveCreateControllerMapper.INSTANCE.createRuntime(createRuntimeDTO);
    }


    private void createTheEntireClusterDTO(ClusterEntity superior, CreateTheEntireClusterDTO dto) {
        ClusterEntity clusterEntity = this.createClusterEntity(dto.getCreateClusterDTO());
        this.clusterListRelationshipList.add(Pair.of(superior, clusterEntity));

        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        dto.getCreateRuntimeDTOList().forEach(runtimeDTO -> {
            runtimeDTO.setFirstToWhom(clusterEntity.getFirstToWhom());
            runtimeDTO.setClusterTrusteeshipType(clusterEntity.getTrusteeshipType());

            RuntimeEntity runtimeEntity = this.createRuntimeEntity(clusterEntity, runtimeDTO);
            runtimeEntity.setOrganizationId(organizationId);
            runtimeEntity.setClusterType(clusterEntity.getClusterType());
            runtimeEntity.setVersion(clusterEntity.getVersion());
            runtimeEntity.setTrusteeshipType(clusterEntity.getTrusteeshipType());
            runtimeEntity.setFirstToWhom(runtimeDTO.getFirstToWhom());
            runtimeEntity.setKubernetesClusterId(0L);
            runtimeEntity.setDeployStatusType(DeployStatusType.SETTLE);
            runtimeEntity.setResourcesConfigId(0L);
            runtimeEntity.setDeployScriptId(0L);
            runtimeEntity.setCreateScriptContent("");
            runtimeEntity.setAuthType("");
            runtimeEntity.setJmxPort(1);

            runtimeEntityList.add(runtimeEntity);
        });
        this.clusterAndRuntimeList.add(Pair.of(clusterEntity, runtimeEntityList));

    }

    private ClusterEntity baseCreateTheEntireClusterDTO(ClusterEntity superior, BaseCreateTheEntireClusterDTO dto) {
        ClusterEntity clusterEntity = this.createClusterEntity(dto.getCreateClusterDTO());
        if (Objects.nonNull(dto.getPrometheusRuntime())) {
            RuntimeEntity runtimeEntity = this.createRuntimeEntity(clusterEntity, dto.getPrometheusRuntime());
            this.clusterAndRuntimeList.add(Pair.of(clusterEntity, List.of(runtimeEntity)));
        }

        return clusterEntity;
    }

    private void runtimeClusterDTO(ClusterEntity superior, RuntimeClusterDTO runtimeClusterDTO) {
        if (Objects.isNull(runtimeClusterDTO)) {
            return;
        }
        ClusterEntity clusterEntity = this.baseCreateTheEntireClusterDTO(superior, runtimeClusterDTO);
        this.clusterListRelationshipList.add(Pair.of(superior, clusterEntity));
        this.createTheEntireClusterDTO(clusterEntity, runtimeClusterDTO.getMetaClusterList());
        this.createTheEntireClusterDTO(clusterEntity, runtimeClusterDTO.getBrokerClusterList());
    }


    private void createCapStorageClusterDTO(ClusterEntity superior, CreateCapStorageClusterDTO createCapStorageClusterDTO) {
        if (Objects.isNull(createCapStorageClusterDTO)) {
            return;
        }
        ClusterEntity clusterEntity = this.baseCreateTheEntireClusterDTO(superior, createCapStorageClusterDTO);
        this.clusterListRelationshipList.add(Pair.of(superior, clusterEntity));
        this.createTheEntireClusterDTO(clusterEntity, createCapStorageClusterDTO.getMetaClusterList());
        this.createTheEntireClusterDTO(clusterEntity, createCapStorageClusterDTO.getBrokerClusterList());
    }

    private void createMainStorageClusterDTO(ClusterEntity superior, MainStorageClusterDTO mainStorageClusterDTO) {
        if (Objects.isNull(mainStorageClusterDTO)) {
            return;
        }
        ClusterEntity clusterEntity = this.baseCreateTheEntireClusterDTO(superior, mainStorageClusterDTO);
        mainStorageClusterDTO.getMetaClusterList().forEach(metaClusterDTO -> {
            this.createTheEntireClusterDTO(clusterEntity, metaClusterDTO);
        });
        mainStorageClusterDTO.getBrokerClusterList().forEach(mainClusterDTO -> {
            ClusterEntity cluster = this.baseCreateTheEntireClusterDTO(clusterEntity, mainClusterDTO);
            mainClusterDTO.getClusterList().forEach(clusterDTO -> {
                this.createTheEntireClusterDTO(cluster, clusterDTO);
            });
        });
    }

    /**
     * 通過 runtime 信息 得到 meta 信息，然後通過
     *
     * @param createTheEventClusterDTO
     */
    public void handler(CreateTheEventClusterDTO createTheEventClusterDTO) {
        this.organizationId = createTheEventClusterDTO.getOrganizationId();
        this.eventSapaceClusterEntity = this.createClusterEntity(createTheEventClusterDTO.getEventSpace());

        this.runtimeClusterDTO(eventSapaceClusterEntity, createTheEventClusterDTO.getEventClusterList());

        this.runtimeClusterDTO(eventSapaceClusterEntity, createTheEventClusterDTO.getMonomerStorageClusters());

        this.createCapStorageClusterDTO(eventSapaceClusterEntity, createTheEventClusterDTO.getCapStorageClusters());

        this.createMainStorageClusterDTO(eventSapaceClusterEntity, createTheEventClusterDTO.getMainStorageClusters());

    }
}
