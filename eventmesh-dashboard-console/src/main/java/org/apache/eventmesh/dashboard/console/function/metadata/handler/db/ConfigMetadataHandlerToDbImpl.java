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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandler;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.ConfigMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConfigMetadataHandlerToDbImpl implements MetadataHandler<ConfigMetadata> {

    @Autowired
    private ConfigService configService;

    Converter<ConfigMetadata, ConfigEntity> converter = new ConfigMetadata2EntityConverter();

    @Override
    public void addMetadata(ConfigMetadata meta) {
        configService.addConfig(converter.convert(meta));
    }

    @Override
    public void addMetadata(List<ConfigMetadata> meta) {
        configService.batchInsert(converter.convert(meta));
    }

    @Override
    public void deleteMetadata(ConfigMetadata meta) {
        configService.deleteConfig(converter.convert(meta));
    }
}

