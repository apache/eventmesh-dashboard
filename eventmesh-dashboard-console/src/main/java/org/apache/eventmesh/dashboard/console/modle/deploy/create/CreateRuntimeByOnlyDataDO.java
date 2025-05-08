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


package org.apache.eventmesh.dashboard.console.modle.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class CreateRuntimeByOnlyDataDO extends ClusterIdDTO {

    @NotNull
    private String name;

    @NotNull
    private String host;

    /**
     * 添加的时候只需要 host 与 post eventmesh runtime 是 admin port。通过 runtime admin 获得所有的配置。 meta 需要 host 与 post store host 与 post jmxPort
     */
    @NotNull
    private Integer port;

    private Integer jmxPort;

    private Integer adminPort;

    @NotNull
    private ClusterTrusteeshipType trusteeshipType;

    @NotNull
    private FirstToWhom firstToWhom;

}
