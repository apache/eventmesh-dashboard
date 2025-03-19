package org.apache.eventmesh.dashboard.common.enums;

import java.util.Objects;

public enum ClusterFramework {

    INDEPENDENCE,

    AP,

    CP,

    CAP,

    MAIN_SLAVE,

    PAXOS,

    RAFT,

    ZK,
    ;

    public boolean isAP() {
        return Objects.equals(this, AP);
    }

    public boolean isCP() {
        return Objects.equals(this, CP);
    }

    public boolean isMainSlave() {
        return Objects.equals(this, MAIN_SLAVE);
    }

    public boolean isCAP() {
        return Objects.equals(this, ZK) || Objects.equals(this, RAFT) || Objects.equals(this, PAXOS) || Objects.equals(this, CAP);
    }
}