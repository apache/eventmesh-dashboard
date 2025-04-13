package org.apache.eventmesh.dashboard.console.mapstruct.deploy;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByOnlyDataDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByOnlyDataDO;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ClusterCycleControllerMapper {

    ClusterCycleControllerMapper INSTANCE = Mappers.getMapper(ClusterCycleControllerMapper.class);


    RuntimeEntity createRuntimeByOnlyDataHandler(CreateRuntimeByOnlyDataDO createRuntimeByOnlyDataDO);


    ClusterEntity createClusterByOnlyDataHandler(CreateClusterByOnlyDataDO createClusterByOnlyDataDO);

}
