package org.apache.eventmesh.dashboard.console.controller.deploy.relationship;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

public class RelationshipHandler implements UpdateHandler<ClusterRelationshipEntity> {

    private ClusterService clusterService;

    private ClusterRelationshipService clusterRelationshipService;

    private RuntimeService runtimeService;


    @Override
    public void init() {

    }

    @Override
    public void handler(ClusterRelationshipEntity clusterRelationshipEntity) {
        //  只关心 meta 集群
        clusterRelationshipService.addClusterRelationshipEntry(clusterRelationshipEntity);

        if (clusterRelationshipEntity.getRelationshipType().isMeta()) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterRelationshipEntity.getClusterId());
            runtimeService.queryOnlyRuntimeByClusterId(runtimeEntity);
        }
    }
}
