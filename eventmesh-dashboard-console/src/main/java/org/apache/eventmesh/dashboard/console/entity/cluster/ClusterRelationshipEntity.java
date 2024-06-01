package org.apache.eventmesh.dashboard.console.entity.cluster;


import lombok.Data;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

@Data
public class ClusterRelationshipEntity extends BaseEntity {

    private ClusterType clusterType;

    private Long relationshipId;

    private ClusterType relationshipType;

    private Integer status;
}
