package org.apache.eventmesh.dashboard.common.model.base;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseSyncBase extends BaseClusterIdBase {


    private ClusterTrusteeshipType trusteeshipType;

    private FirstToWhom firstToWhom;

    private FirstToWhom firstSyncState;

    private ReplicationType replicationType;

    private SyncErrorType syncErrorType;

    private String config;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTimestamp;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTimestamp;


    private LocalDateTime startTimestamp;

    public boolean isCluster() {
        return true;
    }

}
