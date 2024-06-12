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

package org.apache.eventmesh.dashboard.console.entity.connector;

import org.apache.eventmesh.dashboard.common.enums.KubernetesPodStatus;
import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true, exclude = "status")
public class ConnectorEntity extends BaseEntity {

    private static final long serialVersionUID = -8226303660232951326L;

    private Long clusterId;

    private String name;

    private String className;

    private String type;

    /**
     * 0: not active, 1: active
     *
     * @see RecordStatus
     */
    @Schema(name = "status", defaultValue = "0", allowableValues = {"0", "1"}, description = "0:inactive, 1:active")
    private Integer status;

    private String host;

    private Integer port;

    /**
     * @see KubernetesPodStatus
     */
    @Schema(name = "podState", defaultValue = "0", allowableValues = {"0", "1", "2", "3", "4", "5",
        "6"}, description = "0:Pending, 1:Running, 2:Succeeded, 3:Failed, 4:Unknown, 5:Terminating, 6:Terminated")
    private Integer podState;

    /**
     * csv format config id list.<br> Example value: 1,2,7<br> This field is updated when the configuration is modified via the web API, but is not
     * used during the configuration retrieval process.
     */
    private String configIds;

    public void setStatusEnum(RecordStatus statusEnum) {
        this.status = statusEnum.getNumber();
    }

    public void setKubernetesPodStatusEnum(KubernetesPodStatus kubernetesPodStatusEnum) {
        this.podState = kubernetesPodStatusEnum.getNumber();
    }
}
