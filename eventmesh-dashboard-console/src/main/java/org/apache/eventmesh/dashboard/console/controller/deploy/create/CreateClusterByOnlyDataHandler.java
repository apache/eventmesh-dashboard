package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByOnlyDataDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateClusterByOnlyDataHandler implements UpdateHandler<CreateClusterByOnlyDataDO> {

    private ClusterService clusterService;

    private RuntimeService runtimeService;


    @Override
    public void init() {

    }

    @Override
    public void handler(CreateClusterByOnlyDataDO o) {
        ClusterEntity clusterEntity = ClusterCycleControllerMapper.INSTANCE.createClusterByOnlyDataHandler(o);
    }
}
