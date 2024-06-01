package org.apache.eventmesh.dashboard.console.service.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;

import java.util.List;

public interface ClusterRelationshipService {


    Integer addClusterRelationshipEntry(ClusterRelationshipEntity clusterRelationshipEntity);

    Integer relieveRelationship(ClusterRelationshipEntity clusterRelationshipEntity);

    List<ClusterRelationshipEntity> selectAll();

    List<ClusterRelationshipEntity> selectNewlyIncreased(ClusterRelationshipEntity clusterRelationshipEntity);

}
