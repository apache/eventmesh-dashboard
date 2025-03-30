package org.apache.eventmesh.dashboard.common.model.base;


public class BaseOrganizationBase {

    private Long id;

    private Long status;

    private Long organizationId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public String getUnique() {
        return this.getClass().getSimpleName() + "-" + this.id.toString();
    }
}
