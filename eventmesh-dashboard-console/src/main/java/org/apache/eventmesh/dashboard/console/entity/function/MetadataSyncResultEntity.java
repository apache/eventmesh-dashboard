package org.apache.eventmesh.dashboard.console.entity.function;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;

import lombok.Data;

@Data
public class MetadataSyncResultEntity extends BaseEntity {

    private MetadataType metadataType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private boolean isFast = false;

    private String resultData;

}
