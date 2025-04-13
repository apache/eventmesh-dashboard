package org.apache.eventmesh.dashboard.console.controller.deploy.delete;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.AbstractMetadataExampleHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteCluster extends AbstractMetadataExampleHandler<ClusterEntity> {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;


    private List<RuntimeEntity> selfRuntimeList = new ArrayList<>();

    private List<RuntimeEntity> notSelfRuntimeList = new ArrayList<>();

    private

    @Override
    public void init() {

    }

    @Override
    public void handler(ClusterEntity clusterEntity) {
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        this.clusterService.deactivate(clusterEntity);
        if (clusterEntity.getClusterType().isDefinition()) {
            return;
        }

        this.runtimeService.queryOnlyRuntimeByClusterId();
        if (!clusterEntity.getTrusteeshipType().isSelf()) {

        }
    }

    public void handlerDefinition(ClusterEntity clusterEntity) {
        if (!clusterEntity.getClusterType().isDefinition()) {
            this.clusterService.deactivate(clusterEntity);
        }
    }

    public void doHandler(ClusterEntity clusterEntity) {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterEntity.getClusterId());
        List<RuntimeEntity> runtimeEntityList = this.runtimeService.queryOnlyRuntimeByClusterId(runtimeEntity);
        if (runtimeEntityList.isEmpty()) {
            return;
        }
        runtimeEntityList.forEach((value) -> {

            if (value.getTrusteeshipType().isSelf()) {

            }
        });

    }
}
