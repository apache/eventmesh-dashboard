package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateClusterByDeployScriptHandler extends AbstractCreateExampleHandler<CreateClusterByDeployScriptDO> {

    private ClusterRelationshipService clusterRelationshipService;

    @Override
    @Autowired
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    @Autowired
    public void setClusterService(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    @Autowired
    public void setClusterMetadataDomain(ClusterMetadataDomain clusterMetadataDomain) {
        this.clusterMetadataDomain = clusterMetadataDomain;
    }

    @Override
    @Autowired
    public void setResourcesConfigService(ResourcesConfigService resourcesConfigService) {
        this.resourcesConfigService = resourcesConfigService;
    }

    @Autowired
    @Override
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void init() {

    }

    public void buildData(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {

        this.clusterEntity = this.newClusterEntity(createClusterByDeployScriptDO);
        this.clusterService.addCluster(this.clusterEntity);
        this.handlerMetadata(this.clusterEntity);
        configService.copyConfig(createClusterByDeployScriptDO.getConfigGatherId(), this.clusterEntity.getId());
        ConfigEntity configEntity = new ConfigEntity();
        this.configEntityList = configService.queryByClusterAndInstanceId(configEntity);
        ClusterEntity k8sClusterEntity = new ClusterEntity();
        this.k8sClusterEntity = clusterService.queryClusterById(k8sClusterEntity);
        ClusterMetadata clusterMetadata = new ClusterMetadata();
        clusterMetadata.setId(k8sClusterEntity.getId());
        clusterMetadata.setClusterType(k8sClusterEntity.getClusterType());
        this.kubernetesClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, clusterMetadata.getUnique());

        ResourcesConfigEntity resourcesConfigEntity = this.resourcesConfigService.queryResourcesById(null);
        this.resourcesConfigService.insertResources(resourcesConfigEntity);
        this.resourcesConfigEntity = resourcesConfigEntity;


    }

    @Override
    public void handler(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        this.buildData(createClusterByDeployScriptDO);
        if (this.clusterFramework.isMainSlave()) {
            this.mainSlaveHandler(createClusterByDeployScriptDO);
        } else {
            this.ordinaryRuntime(createClusterByDeployScriptDO);
        }

        this.runtimeService.batchInsert(this.runtimeEntityList);
        if (this.clusterType.isMeta() && this.clusterFramework.isCAP()) {
            // 生成  每个节点的 内部域名
        } else {
            //
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            Map<Long, List<RuntimeEntity>> metaRuntimeMap = this.runtimeService.queryMetaRuntimeByClusterId(runtimeEntity);
            // 生成  节点需要的 meta 地址
        }
        this.createExample();
    }

    private void ordinaryRuntime(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        Deque<Integer> linkedList = null;
        if (this.clusterType.isStorage()) {
            linkedList = this.clusterService.getIndex(this.clusterEntity);
        }
        for (int i = 0; i < createClusterByDeployScriptDO.getCreateNum(); i++) {
            this.createRuntimeEntity(replicationType, Objects.isNull(linkedList) ? Integer.valueOf(0) : linkedList.poll());
        }
    }

    private void mainSlaveHandler(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        for (int i = 0; i < createClusterByDeployScriptDO.getCreateNum(); i++) {
            ClusterEntity newClusterEntity = this.newClusterEntity(createClusterByDeployScriptDO);
            clusterEntityList.add(newClusterEntity);
        }
        this.clusterService.batchInsert(clusterEntityList, clusterEntity);
        clusterEntityList.forEach(entity -> {
            this.createRuntimeEntity(ReplicationType.MAIN, 0);
            if (createClusterByDeployScriptDO.getReplicationType().isMainSlave()) {
                this.createRuntimeEntity(ReplicationType.SLAVE, 1);
            }
        });
    }


    private ClusterEntity newClusterEntity(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setClusterType(createClusterByDeployScriptDO.getClusterType());
        clusterEntity.setTrusteeshipType(createClusterByDeployScriptDO.getTrusteeshipType());
        clusterEntity.setVersion(createClusterByDeployScriptDO.getVersion());
        clusterEntity.setReplicationType(createClusterByDeployScriptDO.getReplicationType());
        clusterEntity.setDeployScriptId(createClusterByDeployScriptDO.getDeployScriptId());
        return clusterEntity;
    }
}
