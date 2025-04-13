package org.apache.eventmesh.dashboard.common.model.base;

public abstract class BaseRuntimeIdBase extends BaseClusterIdBase {

    private Long runtimeId;


    public Long getRuntimeId() {
        return runtimeId;
    }

    public void setRuntimeId(Long runtimeId) {
        this.runtimeId = runtimeId;
    }

}
