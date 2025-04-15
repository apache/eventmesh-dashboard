package org.apache.eventmesh.dashboard.common.enums;

public enum ClusterOwnType {

    INDEPENDENCE,

    SHARE,

    OVERALL_SHARE,

    ORGANIZATION_SHARE,

    ;

    public boolean isIndependence() {
        return this == INDEPENDENCE;
    }
}
