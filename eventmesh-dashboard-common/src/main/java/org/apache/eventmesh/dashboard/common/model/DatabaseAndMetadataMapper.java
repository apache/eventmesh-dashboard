package org.apache.eventmesh.dashboard.common.model;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseAndMetadataMapper {


    private MetadataType metaType;

    private Class<?> databaseHandlerClass;

    private Class<?> metadataHandlerClass;

    private ConvertMetaData<?, ?> convertMetaData;


}
