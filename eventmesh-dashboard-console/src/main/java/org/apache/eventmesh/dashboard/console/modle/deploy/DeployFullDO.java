package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;

import java.util.List;

import lombok.Data;

@Data
public class DeployFullDO {

    private List<DeployClusterDO> deployClusterDOList;


    private List<Object> kubernetesList;

    private List<DeployScriptEntity> deployScriptEntityList;
}
