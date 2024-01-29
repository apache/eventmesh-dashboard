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

package org.apache.eventmesh.dashboard.console.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;


public class ConnectionResponse implements Serializable {

    private static final long serialVersionUID = -7317308457824435889L;

    @Schema(name = "id", description = "primary key of table connection")
    private Long id;

    @Schema(name = "sourceType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sourceType;

    @Schema(name = "sourceId", description = "connectorId or clientId")
    private Long sourceId;

    @Schema(name = "sourceStatus", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer sourceStatus;

    //    @Schema(name = "sourceConfigList", description = "source config list")
    //    private List<ConfigEntity> sourceConfigList;

    @Schema(name = "sinkType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sinkType;

    @Schema(name = "sinkId", description = "connectorId or clientId")
    private Long sinkId;

    @Schema(name = "sinkStatus", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer sinkStatus;

    //    @Schema(name = "sinkConfigList", description = "sink config list")
    //    private List<ConfigEntity> sinkConfigList;

    private Long runtimeId;

    @Schema(name = "status", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer status;

    @Schema(name = "topic", description = "related topic name from storage")
    private String topic;

    private Long groupId;

    private String groupName;

    private String description;
}
