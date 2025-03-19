package org.apache.eventmesh.dashboard.console.mapstruct.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.resouce.QueryResourceByObjectTypeDTO;

import org.mapstruct.factory.Mappers;

/**
 *
 */
public interface ResourceConfigControllerMapper {

    ResourceConfigControllerMapper INSTANCE = Mappers.getMapper(ResourceConfigControllerMapper.class);


    ResourcesConfigEntity queryResourcesConfigByObjectType(QueryResourceByObjectTypeDTO queryResourceByObjectTypeDTO);

}
