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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert;

import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectorMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RegistryMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.entity.RegistryEntity2MetadataConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.entity.RuntimeEntity2MetadataConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.entity.TopicEntity2MetadataConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.ClientMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.ClusterMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.ConfigMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.ConnectionMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.RegistryMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.RuntimeMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.TopicMetadata2EntityConverter;

public class ConverterFactory {
    public static Converter<?, ?> createConverter(Class<?> sourceType) {
        if (sourceType == MetaEntity.class) {
            return new RegistryMetadata2EntityConverter();
        } else if (sourceType == RegistryMetadata.class) {
            return new RegistryEntity2MetadataConverter();
        } else if (sourceType == RuntimeEntity.class) {
            return new RuntimeEntity2MetadataConverter();
        } else if (sourceType == RuntimeMetadata.class) {
            return new RuntimeMetadata2EntityConverter();
        } else if (sourceType == TopicMetadata.class) {
            return new TopicMetadata2EntityConverter();
        } else if (sourceType == TopicEntity.class) {
            return new TopicEntity2MetadataConverter();
        } else if (sourceType == ClientMetadata.class) {
            return new ClientMetadata2EntityConverter();
        } else if (sourceType == ClusterMetadata.class) {
            return new ClusterMetadata2EntityConverter();
        } else if (sourceType == ConfigMetadata.class) {
            return new ConfigMetadata2EntityConverter();
        } else if (sourceType == ConnectorMetadata.class) {
            return new ConnectionMetadata2EntityConverter();
        } else if (sourceType == GroupMetadata.class) {
            return new ConnectionMetadata2EntityConverter();
        }
        // Add more conditions here for other types
        throw new IllegalArgumentException("No converter found for type " + sourceType);
    }
}
