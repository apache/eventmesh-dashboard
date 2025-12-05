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


package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain.DataHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeDO;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;

import lombok.Setter;

@Setter
public class DefaultDataHandler implements DataHandler<RuntimeDO, ClusterDO> {


    private MetadataSyncManage metadataSyncManage;


    private Health2Service healthService;

    private KubernetesManage kubernetesManage;


    @Override
    public void registerRuntime(RuntimeEntity runtimeEntity, RuntimeDO runtimeDO, ColonyDO<ClusterDO> colonyDO) {
        RuntimeMetadata runtimeMetadata = RuntimeConvertMetaData.INSTANCE.toMetaData(runtimeEntity);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtimeMetadata, runtimeDO.getCreateSDKConfig(), runtimeMetadata.getClusterType());
        healthService.register(runtimeDO.getRuntimeMetadata());
        metadataSyncManage.register(runtimeDO.getRuntimeMetadata());
    }

    @Override
    public void unRegisterRuntime(RuntimeEntity runtimeEntity, RuntimeDO runtimeDO, ColonyDO<ClusterDO> colonyDO) {
        RuntimeMetadata runtimeMetadata = RuntimeConvertMetaData.INSTANCE.toMetaData(runtimeEntity);
        healthService.unRegister(runtimeMetadata);
        metadataSyncManage.unRegister(runtimeMetadata);
        SDKManage.getInstance().deleteClient(null, runtimeMetadata.getUnique());
    }

    @Override
    public void registerCluster(ClusterEntity clusterEntity, ClusterDO clusterDO, ColonyDO<ClusterDO> colonyDO) {
        ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterEntity.getClusterType());
        if (!clusterSyncMetadata.getClusterFramework().isCAP()) {
            return;
        }
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, clusterDO.getClusterInfo(), clusterDO.getMultiCreateSDKConfig(),
            clusterDO.getClusterInfo().getClusterType());

        healthService.register(clusterDO.getClusterInfo());
        if (clusterEntity.getClusterType() == ClusterType.KUBERNETES_RUNTIME) {
            kubernetesManage.register(clusterDO.getClusterInfo());
        } else {
            metadataSyncManage.register(clusterDO.getClusterInfo());
        }
    }

    @Override
    public void unRegisterCluster(ClusterEntity clusterEntity, ClusterDO clusterDO, ColonyDO<ClusterDO> colonyDO) {
        if (clusterEntity.getClusterType() == ClusterType.KUBERNETES_RUNTIME) {
            this.kubernetesManage.unregister(clusterDO.getClusterInfo());
        }

        healthService.unRegisterCluster(clusterEntity.getClusterId());
        SDKManage.getInstance().deleteClient(null, clusterDO.getClusterInfo().getUnique());

    }

}
