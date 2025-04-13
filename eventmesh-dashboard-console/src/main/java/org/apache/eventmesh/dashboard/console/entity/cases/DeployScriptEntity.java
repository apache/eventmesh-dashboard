package org.apache.eventmesh.dashboard.console.entity.cases;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseOrganizationEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeployScriptEntity extends BaseOrganizationEntity {

    private String name;

    private String version;

    private ClusterType clusterType;

    private String description;

    private String startRuntimeVersion;

    private String endRuntimeVersion;

    private String content;


}
