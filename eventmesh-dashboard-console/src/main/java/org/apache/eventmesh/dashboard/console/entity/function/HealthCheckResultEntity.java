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


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

// state of a health check, 0: failed, 1: passed, 2: doing check, 3: out of time, 4: not connected
@Data
@EqualsAndHashCode(callSuper = true, exclude = "resultDesc")
@Schema(name = "HealthCheckResultEntity", description = "Health check result entity")
public class HealthCheckResultEntity extends BaseRuntimeIdEntity {

    private static final long serialVersionUID = -7350585209577598040L;

    private ClusterType clusterType;

    private String protocol;

    private String interfaces;

    private HealthCheckTypeEnum healthCheckTypeEnum;

    private HealthCheckStatus result;

    private String resultDesc;

    private LocalDateTime beginTime;

    private LocalDateTime finishTime;



    @Schema(description = "Type of Health Check;0:Unknown, 1:Cluster, 2:Runtime, 3:Topic, 4:Storage", defaultValue = "0", allowableValues = {"0",
        "1", "2", "3", "4"})
    private Integer type;

    @Schema(description = "Instance id(database schema) of the health check object")
    private Long typeId;


}
