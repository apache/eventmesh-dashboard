package org.apache.eventmesh.dashboard.console.entity.function;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataSyncResultEntity extends BaseEntity {


    private Long syncId;

    private MetadataType metadataType;

    private String errorType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private boolean isFast = false;

    private SyncErrorType syncErrorType;

    private String resultData;

}
