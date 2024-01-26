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

package org.apache.eventmesh.dashboard.console.entity.connection;

import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;


/**
 * A Connection is a link from a source to a sink.
 */
@Data
public class ConnectionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(name = "id", description = "primary key")
    private Long id;

    @Schema(name = "sourceType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sourceType;

    /**
     * The type of source. Possible values are "connector" and "client".
     */
    @Schema(name = "sourceId", description = "connectorId or clientId")
    private Long sourceId;

    /**
     * 0: not active, 1: active
     */
    @Schema(name = "sourceStatus", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer sourceStatus;

    /**
     * The type of sink. Possible values are "connector" and "client".
     */
    @Schema(name = "sinkType", defaultValue = "connector", allowableValues = {"connector", "client"})
    private String sinkType;

    @Schema(name = "sinkId", description = "connectorId or clientId")
    private Long sinkId;

    /**
     * 0: not active, 1: active
     */
    @Schema(name = "sinkStatus", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer sinkStatus;

    private Long runtimeId;

    @Schema(name = "status", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer status;

    @Schema(name = "topic", description = "related topic name from storage")
    private String topic;

    private Long groupId;

    private String groupName;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionEntity that = (ConnectionEntity) o;
        return Objects.equals(sourceType, that.sourceType)
            && Objects.equals(sourceId, that.sourceId)
            && Objects.equals(sourceStatus, that.sourceStatus)

            && Objects.equals(sinkType, that.sinkType)
            && Objects.equals(sinkId, that.sinkId)
            && Objects.equals(sinkStatus, that.sinkStatus)

            && Objects.equals(runtimeId, that.runtimeId)
            && Objects.equals(status, that.status)

            && Objects.equals(topic, that.topic)
            && Objects.equals(groupId, that.groupId)
            && Objects.equals(groupName, that.groupName)

            && Objects.equals(description, that.description);
    }
}
