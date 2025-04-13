package org.apache.eventmesh.dashboard.console.service.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;

public interface DeployScriptService {


    void insert(DeployScriptEntity deployScriptEntity);


    void update(DeployScriptEntity deployScriptEntity);


    void deleteById(DeployScriptEntity deployScriptEntity);


    DeployScriptEntity queryById(DeployScriptEntity deployScriptEntity);

    void queryByName(DeployScriptEntity deployScriptEntity);
}
