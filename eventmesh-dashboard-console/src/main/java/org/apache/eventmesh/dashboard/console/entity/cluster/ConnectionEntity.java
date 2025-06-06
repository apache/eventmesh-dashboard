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


package org.apache.eventmesh.dashboard.console.entity.cluster;

import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;

import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * A Connection is a link from a source to a sink.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectionEntity extends BaseRuntimeIdEntity {

    private static final long serialVersionUID = 6565578252656944905L;

    /**
     * Runtime cluster id
     */
    @Schema(name = "clusterId", description = "runtime cluster id")
    private Long clusterId;

    /**
     * The type of source. Possible values are "connector" or "client".
     */
    @Schema(name = "sourceType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sourceType;

    /**
     * The id of the source.<br> It can be connectorId or clientId according to the sourceType.
     */
    @Schema(name = "sourceId", description = "connectorId or clientId")
    private Long sourceId;

    /**
     * The type of sink. Possible values are "connector" or "client".
     */
    @Schema(name = "sinkType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sinkType;

    /**
     * The id of the sink.<br> It can be connectorId or clientId according to the sinkType.
     */
    @Schema(name = "sinkId", description = "connectorId or clientId")
    private Long sinkId;

    private Long runtimeId;

    private String topic;

    private Long groupId;

    private Timestamp endTime;

    private String description;

}
