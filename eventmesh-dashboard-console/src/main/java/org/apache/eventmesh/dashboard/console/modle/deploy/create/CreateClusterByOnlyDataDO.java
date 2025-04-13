package org.apache.eventmesh.dashboard.console.modle.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.modle.OrganizationIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateClusterByOnlyDataDO extends OrganizationIdDTO {

    private ClusterType clusterType;

    private ClusterTrusteeshipType trusteeshipType;

    private FirstToWhom firstToWhom;

    private ReplicationType replicationType;

    private Long deployScriptId;

    private String clusterName;

    private Integer jmxPort;

    private String runtimeList;
}
