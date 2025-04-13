package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

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
public class CreateRuntimeByDeployScriptHandler extends AbstractCreateExampleHandler<CreateRuntimeByDeployScriptDO> {

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
    public void setResourcesConfigService(ResourcesConfigService resourcesConfigService) {
        this.resourcesConfigService = resourcesConfigService;
    }

    @Override
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void init() {

    }

    @Override
    public void handler(CreateRuntimeByDeployScriptDO createRuntimeByDeployScriptDO) {
        this.buildData(createRuntimeByDeployScriptDO);
        Deque<Integer> linkedList = null;
        if (this.clusterType.isStorage()) {
            linkedList = this.clusterService.getIndex(this.clusterEntity);
        }
        this.createRuntimeEntity(createRuntimeByDeployScriptDO.getReplicationType(),
            Objects.isNull(linkedList) ? Integer.valueOf(0) : linkedList.poll());
        this.runtimeService.batchInsert(this.runtimeEntityList);
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        if (this.clusterType.isMetaAndRuntime()) {
            // 获得所有 节点，因为需要
            this.runtimeService.queryOnlyRuntimeByClusterId(runtimeEntity);
            return;
        }
        if (this.clusterType.isRuntime()) {
            //
            Map<Long, List<RuntimeEntity>> metaRuntimeByClusterId = this.runtimeService.queryMetaRuntimeByClusterId(runtimeEntity);
        }
        this.createExample();
        if (this.clusterType.isRuntime()) {
            return;
        }
        if (this.clusterType.isMetaAndRuntime()) {
            // 获得所有 节点
            return;
        }

        if (this.clusterType.isMeta()) {
            //
            if (this.clusterFramework.isCAP()) {
                //
            }

            return;
        }

    }

}
