package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;

import java.util.List;

import lombok.Data;


@Data
public class DeployMetadataDO {


    private List<DeployClusterDO> deployClusterDOList;

    private List<ClusterRelationshipEntity> relationshipList;

    private List<Object> kubernetesList;

}
