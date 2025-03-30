package org.apache.eventmesh.dashboard.common.model;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseAndMetadataMapper {


    private MetadataType metaType;

    private Class<?> databaseClass;

    private Class<?> metadataClass;

    private ConvertMetaData<?, ?> convertMetaData;


}
