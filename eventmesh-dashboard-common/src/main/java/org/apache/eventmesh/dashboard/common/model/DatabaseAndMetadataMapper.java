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


package org.apache.eventmesh.dashboard.common.model;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;

import java.util.Map;
import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseAndMetadataMapper {


    private MetadataType metaType;

    private Class<?> databaseHandlerClass;

    private Class<?> metadataHandlerClass;

    private Map<ClusterType, Class<?>> metadataHandlerClassByClusterType;

    private ConvertMetaData<?, ?> convertMetaData;

    public Class<?> resolveMetadataHandlerClass(ClusterType clusterType) {
        if (Objects.isNull(clusterType) || Objects.isNull(this.metadataHandlerClassByClusterType)) {
            return this.metadataHandlerClass;
        }
        return this.metadataHandlerClassByClusterType.getOrDefault(clusterType, this.metadataHandlerClass);
    }


}
