package org.apache.eventmesh.dashboard.console.controller.deploy.pause;

import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PauseRuntime implements UpdateHandler<RuntimeEntity> {

    @Autowired
    private RuntimeService runtimeService;


    @Override
    public void init() {

    }

    @Override
    public void handler(RuntimeEntity runtimeEntity) {
        RuntimeEntity newRuntimeEntity = this.runtimeService.queryRuntimeEntityById(runtimeEntity);
        if (newRuntimeEntity.getTrusteeshipType().isSelf()) {
            newRuntimeEntity.setDeployStatusType(DeployStatusType.PAUSE_WAIT);
        } else {
            newRuntimeEntity.setStatus(0L);
        }
        this.runtimeService.deactivate(runtimeEntity);


    }
}
