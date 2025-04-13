package org.apache.eventmesh.dashboard.console.modle.cluster;

import org.apache.eventmesh.dashboard.common.enums.ReplicationType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateClusterByCompleteDataDTO extends CreateClusterBySimpleDataDTO {


    private Long k8sClusterId;

    private Long deployScriptId;

    private Long resourcesId;

    private Long createNum;

    private ReplicationType replicationType;
}
