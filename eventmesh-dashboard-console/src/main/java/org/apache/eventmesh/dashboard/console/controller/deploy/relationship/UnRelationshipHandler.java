package org.apache.eventmesh.dashboard.console.controller.deploy.relationship;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

public class UnRelationshipHandler implements UpdateHandler<ClusterRelationshipEntity> {

    private ClusterService clusterService;

    private ClusterRelationshipService clusterRelationshipService;

    private RuntimeService runtimeService;

    @Override
    public void init() {

    }

    @Override
    public void handler(ClusterRelationshipEntity clusterRelationshipEntity) {

    }
}
