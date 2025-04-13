package org.apache.eventmesh.dashboard.common.enums;

public enum ReplicationType {

    NOT,

    MAIN,

    SLAVE,

    MAIN_SLAVE,
    ;


    public boolean isNot() {
        return this == NOT;
    }

    public boolean isMain() {
        return this == MAIN;
    }

    public boolean isSlave() {
        return this == SLAVE;
    }


    public boolean isMainSlave() {
        return this == MAIN_SLAVE;
    }
}
