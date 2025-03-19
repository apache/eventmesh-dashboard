package org.apache.eventmesh.dashboard.console.modle.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import lombok.Data;

/**
 *
 */
@Data
public class CreateClusterBySimpleDataDTO {

    private String name;

    private Long configGatherId;

    private ClusterTrusteeshipType trusteeshipType;

    private ClusterType clusterType;

    private String version;

    private String jmxProperties;

    private String description;

    private Integer authType;

    private Integer runState;

}
