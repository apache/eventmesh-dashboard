package org.apache.eventmesh.dashboard.console.modle.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateClusterByDeployScriptDO extends CreateRuntimeByDeployScriptDO {


    private String name;

    private Long configGatherId;

    private ClusterTrusteeshipType trusteeshipType;

    private ClusterType clusterType;

    private String version;

    private String jmxProperties;

    private String description;

    private Integer authType;

    private Integer runState;

    private Long k8sClusterId;

    private Long deployScriptId;

    private Long resourcesId;

}
