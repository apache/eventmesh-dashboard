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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata;

import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.common.model.metadata.RegistryMetadata;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

public class RegistryMetadata2EntityConverter implements Converter<RegistryMetadata, MetaEntity> {

    @Override
    public MetaEntity convert(RegistryMetadata source) {
        return MetaEntity.builder()
            .host(source.getHost())
            .port(source.getPort())
            .clusterId(source.getClusterId())
            .name(source.getName())
            .version(source.getVersion())
            .params(source.getParams())
            .role(source.getRole())
            .status(RecordStatus.ACTIVE.getNumber())
            .type(source.getType())
            .username(source.getUsername())
            .build();
    }

    @Override
    public String getUnique(RegistryMetadata source) {
        if (source.getRegistryAddress() != null) {
            return source.getRegistryAddress();
        } else {
            return source.getHost() + ":" + source.getPort();
        }
    }
}
