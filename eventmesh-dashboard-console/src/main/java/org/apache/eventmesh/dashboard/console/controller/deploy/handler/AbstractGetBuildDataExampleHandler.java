package org.apache.eventmesh.dashboard.console.controller.deploy.handler;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractGetBuildDataExampleHandler<T> extends AbstractBuildDataExampleHandler<T> {


    @Autowired
    protected ConfigService configService;

    @Autowired
    protected ClusterService clusterService;

    @Autowired
    protected ClusterMetadataDomain clusterMetadataDomain;

    @Autowired
    protected ResourcesConfigService resourcesConfigService;

    @Autowired
    protected RuntimeService runtimeService;

    /**
     * 主要用于 runtime 与 main slave cluster 的 build
     *
     * @param clusterIdDTO
     */
    public void buildData(ClusterIdDTO clusterIdDTO) {
        ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(clusterIdDTO);
        this.clusterEntity = this.clusterService.queryClusterById(clusterEntity);

        this.buildData(clusterEntity);
    }

    /**
     * 主要用于 runtime 与 main slave cluster 的 build
     *
     * @param clusterEntity
     */
    public void buildData(ClusterEntity clusterEntity) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setClusterId(configEntity.getId());
        configEntity.setConfigType("");
        configEntity.setInstanceId(0L);
        configEntityList = configService.queryByClusterAndInstanceId(configEntity);

        ClusterEntity k8sClusterEntity = new ClusterEntity();
        this.k8sClusterEntity = clusterService.queryClusterById(k8sClusterEntity);
        ClusterMetadata clusterMetadata = new ClusterMetadata();
        clusterMetadata.setId(k8sClusterEntity.getId());
        clusterMetadata.setClusterType(k8sClusterEntity.getClusterType());
        this.kubernetesClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, clusterMetadata.getUnique());

        clusterMetadataDomain.getMetaColonyDO(configEntity.getId());
    }

    public abstract void setConfigService(ConfigService configService);

    public abstract void setClusterService(ClusterService clusterService);

    public abstract void setClusterMetadataDomain(ClusterMetadataDomain clusterMetadataDomain);

    public abstract void setResourcesConfigService(ResourcesConfigService resourcesConfigService);

    public abstract void setRuntimeService(RuntimeService runtimeService);
}
