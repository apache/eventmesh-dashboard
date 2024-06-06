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

package org.apache.eventmesh.dashboard.console.entity.client;

import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "status")
@SuperBuilder
public class ClientEntity extends BaseEntity {

    private static final long serialVersionUID = 8204133370609215856L;

    /**
     * Primary key
     */
    @Schema(name = "id", description = "primary key")
    private Long id;

    private String name;

    private String platform;

    /**
     * programing language of client
     */
    @Schema(name = "language", example = "java")
    private String language;

    /**
     * process id
     */
    @Schema(name = "pid", description = "process id")
    private Long pid;

    private String host;

    private Integer port;

    /**
     * protocol used to connect to runtime.
     */
    @Schema(name = "protocol", example = "http", allowableValues = {"http", "grpc", "tcp"})
    private String protocol;

    /**
     * 0: not active, 1: active
     *
     * @see RecordStatus
     */
    @Schema(name = "status", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer status;

    /**
     * csv format config id list.<br> Example value: 1,2,7<br> This field is updated when the configuration is modified via the web API, but is not
     * used during the configuration retrieval process.
     */
    private String configIds;

    private String description;

    /**
     * The time when the client is terminated.
     */
    private Timestamp endTime;

    public ClientEntity(ClientMetadata source) {
        setName(source.getName());
        setPlatform(source.getPlatform());
        setLanguage(source.getLanguage());
        setPid(source.getPid());
        setHost(source.getHost());
        setPort(source.getPort());
        setClusterId(source.getClusterId());
        setProtocol(source.getProtocol());
        setDescription("");
        setConfigIds("");
        setStatus(1);
    }

    public void setStatusEntity(RecordStatus status) {
        this.status = status.getNumber();
    }
}
    