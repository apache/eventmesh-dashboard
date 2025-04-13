package org.apache.eventmesh.dashboard.console.controller.deploy.delete;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.AbstractBuildDataExampleHandler;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.client.KubernetesClient;

@Component
public class DeleteRuntime extends AbstractBuildDataExampleHandler<RuntimeEntity> {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private DeployScriptService deployScriptService;

    private KubernetesClient kubernetesClient;

    @Override
    public void init() {

    }

    @Override
    public void handler(RuntimeEntity runtimeEntity) {
        RuntimeEntity newRuntimeEntity = this.runtimeService.queryRuntimeEntityById(runtimeEntity);
        this.runtimeService.deactivate(runtimeEntity);
        if (!Objects.equals(newRuntimeEntity.getTrusteeshipType(), ClusterTrusteeshipType.SELF)) {
            return;
        }
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(newRuntimeEntity.getClusterId());
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);

        this.handlerMetadata(clusterEntity);
        //
        List<RuntimeEntity> runtimeEntityList;
        if (this.clusterFramework.isCAP()) {
            DeployScriptEntity deployScriptEntity = new DeployScriptEntity();
            deployScriptEntity.setId(clusterEntity.getDeployScriptId());
            this.deployScriptService.queryById(deployScriptEntity);
            runtimeEntityList = this.runtimeService.getRuntimeToFrontByClusterId(newRuntimeEntity);

        }
        if (this.clusterType.isMeta()) {
            // 获得所有 存储 集群
            this.clusterService.queryStorageByClusterId(clusterEntity).forEach((value) -> {
                DeployScriptEntity deployScriptEntity = new DeployScriptEntity();
                deployScriptEntity.setId(value.getDeployScriptId());
                this.deployScriptService.queryById(deployScriptEntity);
                runtimeEntityList = this.runtimeService.getRuntimeToFrontByClusterId(newRuntimeEntity);
            });

        }


    }
}
