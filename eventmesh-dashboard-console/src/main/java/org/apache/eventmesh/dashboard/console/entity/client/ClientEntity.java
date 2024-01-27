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

import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;

public class ClientEntity extends BaseEntity {

    private static final long serialVersionUID = 8204133370609215856L;
    @Schema(name = "id", description = "primary key")
    private Long id;

    private String name;

    private String platform;

    private String language;

    private Long pid;

    private String host;

    private Integer port;

    private String protocol;

    /**
     * 0: not active, 1: active
     */
    @Schema(name = "status", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:not active, 1:active")
    private Integer status;

    private String configIds;

    private String description;

    private Timestamp endTime;
}
    