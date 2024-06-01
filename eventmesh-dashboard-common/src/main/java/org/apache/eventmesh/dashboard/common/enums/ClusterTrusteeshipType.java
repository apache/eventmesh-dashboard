package org.apache.eventmesh.dashboard.common.enums;

public enum ClusterTrusteeshipType {

    //
    FIRE_AND_FORGET_TRUSTEESHIP,

    // 发现 or 长时间没有上线 cluster sync db
    TRUSTEESHIP,

    REVERSE,

    NO_TRUSTEESHIP;
    
}
