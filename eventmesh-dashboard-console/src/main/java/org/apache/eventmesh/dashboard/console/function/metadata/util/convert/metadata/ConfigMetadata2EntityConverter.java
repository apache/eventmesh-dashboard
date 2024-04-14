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

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

public class ConfigMetadata2EntityConverter implements Converter<ConfigMetadata, ConfigEntity> {

    @Override
    public ConfigEntity convert(ConfigMetadata source) {
        return ConfigEntity.builder()
            .configName(source.getConfigKey())
            .configValue(source.getConfigValue())
            .clusterId(source.getClusterId())
            .edit(1)
            .businessType("")
            .instanceId(source.getInstanceId())
            .description("")
            .instanceType(source.getInstanceType())
            .isDefault(0)
            .startVersion("")
            .endVersion("")
            .eventmeshVersion("")
            .diffType(0)
            .isModify(0)
            .status(1)
            .build();
    }

    @Override
    public String getUnique(ConfigMetadata source) {
        return null;
    }
}
