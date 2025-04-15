package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import java.util.List;

import lombok.Data;

@Data
public class ClusterAllMetadataDO {

    private List<ClusterEntity> clusterEntityList;

    private List<RuntimeEntity> runtimeEntityList;

    private List<ClusterRelationshipEntity> clusterRelationshipEntityList;

}
