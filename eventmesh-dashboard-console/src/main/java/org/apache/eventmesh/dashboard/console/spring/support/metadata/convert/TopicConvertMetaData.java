package org.apache.eventmesh.dashboard.console.spring.support.metadata.convert;

import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper
public interface TopicConvertMetaData extends ConvertMetaData<TopicEntity, TopicMetadata> {

    TopicConvertMetaData INSTANCE = Mappers.getMapper(TopicConvertMetaData.class);

    TopicEntity toEntity(TopicMetadata meta);

    TopicMetadata toMetaData(TopicEntity entity);
}
