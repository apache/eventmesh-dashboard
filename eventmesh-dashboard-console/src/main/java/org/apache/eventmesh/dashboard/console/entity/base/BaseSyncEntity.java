package org.apache.eventmesh.dashboard.console.entity.base;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 此类 runtime 与 cluster 共用
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseSyncEntity extends BaseClusterIdEntity {


    private ClusterTrusteeshipType trusteeshipType;

    private FirstToWhom firstToWhom;

    private FirstToWhom firstSyncState;

    private ReplicationType replicationType;

    private SyncErrorType syncErrorType;

    private DeployStatusType deployStatusType;

    private Long resourcesConfigId;

    private Long deployScriptId;

    private Long deployScriptName;

    private Long deployScriptVersion;


    /**
     * 上线时间
     */
    private LocalDateTime onlineTimestamp;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTimestamp;


    private LocalDateTime startTimestamp;

}
