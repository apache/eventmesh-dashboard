package org.apache.eventmesh.dashboard.console.modle.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateRuntimeByDeployScriptDO extends ClusterIdDTO {


    private Long deployScriptId;

    private Long resourcesConfigId;

    private Integer createNum;

    private ReplicationType replicationType;
}
