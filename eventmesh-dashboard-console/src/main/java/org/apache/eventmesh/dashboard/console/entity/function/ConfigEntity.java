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


package org.apache.eventmesh.dashboard.console.entity.function;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseClusterIdEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigEntity extends BaseClusterIdEntity {


    private String businessType;

    private Long retrospectId;

    /**
     * config type 0:runtime,1:storage,2:connector,3:topic
     */
    private MetadataType instanceType;

    private Long instanceId;

    private String configType;

    private String configName;

    private String configValue;

    private String startVersion;

    private String eventmeshVersion;

    private String endVersion;

    private Integer diffType;

    private String description;

    private Integer edit;

    private Integer isDefault;

    private Integer isModify;

    private Integer alreadyUpdate;


    public boolean matchVersion(String eventmeshVersion) {
        return true;
    }
}
