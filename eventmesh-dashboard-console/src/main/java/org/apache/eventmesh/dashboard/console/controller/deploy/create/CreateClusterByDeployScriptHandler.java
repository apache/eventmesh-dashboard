package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

    @Override
    public void handler(CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        this.clusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByDeployScript(createClusterByDeployScriptDO);

        this.clusterService.insertCluster(this.clusterEntity);
        this.handlerMetadata(this.clusterEntity);
        configService.copyConfig(createClusterByDeployScriptDO.getConfigGatherId(), this.clusterEntity.getId());
        ConfigEntity configEntity = new ConfigEntity();
        this.configEntityList = configService.queryByClusterAndInstanceId(configEntity);
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
            linkedList = this.clusterService.getIndex(this.clusterEntity);
        }
        for (int i = 0; i < createClusterByDeployScriptDO.getCreateNum(); i++) {
            this.createRuntimeEntity(this.clusterEntity, replicationType,
                Objects.isNull(linkedList) ? Integer.valueOf(0) : linkedList.poll());
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

}
