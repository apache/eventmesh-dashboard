/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
