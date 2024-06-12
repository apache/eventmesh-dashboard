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

package org.apache.eventmesh.dashboard.common.model.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * This class is used to represent a piece of metadata, which can be used in create, update or delete operations to metadata service(eventmesh meta
 * center, eventmesh runtime cluster)<p> follow method should be called in init block which is used to indicate the type of metadata:
 * {@code this.setServiceTypeEnums(MetadataServiceTypeEnums.RUNTIME);}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class MetadataConfig {

    //eventmesh registry url
    private String registryAddress;
    //cluster id in database
    private Long clusterId;


    private Long id;

    /**
     * @return A string that is unique to the source, usually a url
     */
    public abstract String getUnique();
}
