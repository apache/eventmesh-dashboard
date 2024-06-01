package org.apache.eventmesh.dashboard.common.model.metadata;

import lombok.Data;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;

@Data
public class ClusterRelationshipMetadata extends MetadataConfig {

    private ClusterType clusterType;

    private Long relationshipId;

    private ClusterType relationshipType;

    private Integer status;

    @Override
    public String getUnique() {
        return null;
    }
}
