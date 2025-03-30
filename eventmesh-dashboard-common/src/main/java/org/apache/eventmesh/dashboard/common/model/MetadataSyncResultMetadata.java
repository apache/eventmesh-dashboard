package org.apache.eventmesh.dashboard.common.model;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;

public class MetadataSyncResultMetadata {


    private MetadataType metadataType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private boolean isFast = false;

    private String resultData;
}
