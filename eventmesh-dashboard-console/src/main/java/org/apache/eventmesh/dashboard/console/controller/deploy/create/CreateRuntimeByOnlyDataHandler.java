package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByOnlyDataDO;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateRuntimeByOnlyDataHandler implements UpdateHandler<CreateRuntimeByOnlyDataDO> {

    private RuntimeService runtimeService;


    @Override
    public void init() {

    }

    @Override
    public void handler(CreateRuntimeByOnlyDataDO createRuntimeByOnlyDataDO) {
        RuntimeEntity runtimeEntity = ClusterCycleControllerMapper.INSTANCE.createRuntimeByOnlyDataHandler(createRuntimeByOnlyDataDO);
        runtimeService.insertRuntime(runtimeEntity);
    }
}
