package org.apache.eventmesh.dashboard.core.metadata.result;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;

import lombok.Data;


@Data
public class MetadataSyncResult {

    private String key;

    private BaseSyncBase baseSyncBase;

    private MetadataType metadataType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private SyncErrorType syncErrorType;

    private FirstToWhom firstToWhom = FirstToWhom.NOT;

    private boolean success = false;


}
