package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.console.service.metadata.RuntimeDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.service.remoting.MetaRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import lombok.Getter;

@Getter
public enum DatabaseAndMetadataType {

    RUNTIME(DatabaseAndMetadataMapper.builder().metaType(MetadataType.RUNTIME).databaseHandlerClass(RuntimeDataMetadataHandler.class)
        .metadataHandlerClass(MetaRemotingService.class).convertMetaData(RuntimeConvertMetaData.INSTANCE).build()),

    TOPIC(DatabaseAndMetadataMapper.builder().metaType(MetadataType.TOPIC).databaseHandlerClass(TopicDataMetadataHandler.class)
        .metadataHandlerClass(TopicRemotingService.class).convertMetaData(RuntimeConvertMetaData.INSTANCE).build()),


    OFFSET(null),

    GROUP(null),

    CONFIG(null),

    CLIENT(null),
    ;


    private DatabaseAndMetadataMapper databaseAndMetadataMapper;


    DatabaseAndMetadataType(DatabaseAndMetadataMapper databaseAndMetadataMapper) {
        this.databaseAndMetadataMapper = databaseAndMetadataMapper;
    }

}
