package org.apache.eventmesh.dashboard.console.spring.support.metadata.convert;

import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;


import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper
public interface RuntimeConvertMetaData extends ConvertMetaData<RuntimeEntity, RuntimeMetadata> {

    RuntimeConvertMetaData INSTANCE = Mappers.getMapper(RuntimeConvertMetaData.class);

    RuntimeEntity toEntity(RuntimeMetadata meta);

    RuntimeMetadata toMetaData(RuntimeEntity entity);
}
