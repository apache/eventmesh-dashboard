package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
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
        metadataSyncManage.register(clusterDO.getClusterInfo());
    }

    @Override
    public void unRegisterCluster(ClusterEntity clusterEntity, ClusterDO clusterDO, ColonyDO<ClusterDO> colonyDO) {
        healthService.unRegisterCluster(clusterEntity.getClusterId());

    }

}
