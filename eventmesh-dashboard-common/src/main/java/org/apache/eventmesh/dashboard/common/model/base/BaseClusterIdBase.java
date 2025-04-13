package org.apache.eventmesh.dashboard.common.model.base;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

public abstract class BaseClusterIdBase extends BaseOrganizationBase {

    private Long clusterId;

    private ClusterType clusterType;

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public boolean isDelete() {
        return this.getStatus() == 0;
    }

    public boolean isUpdate() {
        return !this.isDelete() && !this.isInsert();
    }

    public boolean isInsert() {
        return true;
    }

}
