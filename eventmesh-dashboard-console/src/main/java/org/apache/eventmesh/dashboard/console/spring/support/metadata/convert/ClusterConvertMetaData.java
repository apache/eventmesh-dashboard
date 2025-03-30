package org.apache.eventmesh.dashboard.console.spring.support.metadata.convert;

import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper
public interface ClusterConvertMetaData extends ConvertMetaData<ClusterEntity, ClusterMetadata> {

    ClusterConvertMetaData INSTANCE = Mappers.getMapper(ClusterConvertMetaData.class);

    ClusterEntity toEntity(ClusterMetadata meta);

    ClusterMetadata toMetaData(ClusterEntity entity);
}
