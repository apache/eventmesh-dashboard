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

import org.apache.eventmesh.dashboard.common.model.metadata.RegistryMetadata;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.console.service.registry.RegistryDataService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegistryMetadataHandlerToDbImpl implements MetadataHandler<RegistryMetadata> {

    @Autowired
    private RegistryDataService registryDataService;

    @Override
    public void addMetadata(RegistryMetadata meta) {
        registryDataService.insert(new MetaEntity(meta));
    }

    @Override
    public void addMetadata(List<RegistryMetadata> meta) {
        List<MetaEntity> entityList = meta.stream()
            .map(MetaEntity::new)
            .collect(Collectors.toList());
        registryDataService.batchInsert(entityList);
    }

    @Override
    public void deleteMetadata(RegistryMetadata meta) {
        registryDataService.deactivate(new MetaEntity(meta));
    }
}
