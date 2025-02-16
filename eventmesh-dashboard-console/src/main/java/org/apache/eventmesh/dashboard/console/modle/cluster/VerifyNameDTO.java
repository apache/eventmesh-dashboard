package org.apache.eventmesh.dashboard.console.modle.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import lombok.Data;

@Data
public class VerifyNameDTO {


    private Integer organizationId;

    private ClusterType clusterType;

    private String clusterName;
}
