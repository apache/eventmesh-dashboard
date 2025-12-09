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
import org.apache.eventmesh.dashboard.console.service.metadata.ClientDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.ConfigDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.ConsumeOffsetDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.GroupDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.GroupMemberDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.RuntimeDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicOffsetDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConfigConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConsumeOffsetConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.GroupConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.GroupMemberConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.NetConnectionMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.TopicConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.TopicOffsetConvertMetaData;
import org.apache.eventmesh.dashboard.service.remoting.ClientRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.ConsumeOffsetRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.GroupMemberRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.MetaRuntimeRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicOffsetRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import lombok.Getter;

@Getter
public enum DatabaseAndMetadataType {

    RUNTIME(DatabaseAndMetadataMapper.builder().metaType(MetadataType.RUNTIME).databaseHandlerClass(RuntimeDataMetadataHandler.class)
        .metadataHandlerClass(MetaRuntimeRemotingService.class).convertMetaData(RuntimeConvertMetaData.INSTANCE).build()),

    TOPIC(DatabaseAndMetadataMapper.builder().metaType(MetadataType.TOPIC).databaseHandlerClass(TopicDataMetadataHandler.class)
        .metadataHandlerClass(TopicRemotingService.class).convertMetaData(TopicConvertMetaData.INSTANCE).build()),


    TOPIC_OFFSET(DatabaseAndMetadataMapper.builder().metaType(MetadataType.TOPIC_OFFSET).databaseHandlerClass(TopicOffsetDataMetadataHandler.class)
        .metadataHandlerClass(TopicOffsetRemotingService.class).convertMetaData(TopicOffsetConvertMetaData.INSTANCE).build()),

    CONSUME_OFFSET(
        DatabaseAndMetadataMapper.builder().metaType(MetadataType.CONSUME_OFFSET).databaseHandlerClass(ConsumeOffsetDataMetadataHandler.class)
            .metadataHandlerClass(ConsumeOffsetRemotingService.class).convertMetaData(ConsumeOffsetConvertMetaData.INSTANCE).build()),

    GROUP(DatabaseAndMetadataMapper.builder().metaType(MetadataType.GROUP).databaseHandlerClass(GroupDataMetadataHandler.class)
        .metadataHandlerClass(GroupRemotingService.class).convertMetaData(GroupConvertMetaData.INSTANCE).build()),

    GROUP_MEMBER(DatabaseAndMetadataMapper.builder().metaType(MetadataType.GROUP_MEMBER).databaseHandlerClass(GroupMemberDataMetadataHandler.class)
        .metadataHandlerClass(GroupMemberRemotingService.class).convertMetaData(GroupMemberConvertMetaData.INSTANCE).build()),

    CONFIG(DatabaseAndMetadataMapper.builder().metaType(MetadataType.CONFIG).databaseHandlerClass(ConfigDataMetadataHandler.class)
        .metadataHandlerClass(ConfigRemotingService.class).convertMetaData(ConfigConvertMetaData.INSTANCE).build()),

    CLIENT(DatabaseAndMetadataMapper.builder().metaType(MetadataType.CLIENT).databaseHandlerClass(ClientDataMetadataHandler.class)
        .metadataHandlerClass(ClientRemotingService.class).convertMetaData(ConfigConvertMetaData.INSTANCE).build()),

    NET_CONNECT(DatabaseAndMetadataMapper.builder().metaType(MetadataType.NET_CONNECT).databaseHandlerClass(ClientDataMetadataHandler.class)
        .metadataHandlerClass(ClientRemotingService.class).convertMetaData(NetConnectionMetaData.INSTANCE).build()),

    ;


    private DatabaseAndMetadataMapper databaseAndMetadataMapper;


    DatabaseAndMetadataType(DatabaseAndMetadataMapper databaseAndMetadataMapper) {
        this.databaseAndMetadataMapper = databaseAndMetadataMapper;
    }

}
