package org.apache.eventmesh.dashboard.core.metadata.result;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;

import lombok.Data;


@Data
public class MetadataSyncResult {

    private Object syncObject;

    private MetadataType metadataType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private boolean isFast = false;



}
