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

package org.apache.eventmesh.dashboard.console.entity;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base Entity provide some basic fields that every Eventmesh Dashboard Entity would have
 *
 * 12 broker -> 12 queue ，
 *     11 queue ，  1broker 没有 队列。
 *   副本，随机出现在一个 broker
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"createTime", "updateTime"})
@Schema(name = "BaseEntity", description = "Base entity")
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = -2697805837923579585L;
    /**
     * Primary key
     */
    @Schema(name = "id", description = "primary key")
    protected Long id;

    /**
     * 集群id，不是 eventmesh集群id。
     */
    protected Long clusterId;

    protected ClusterType clusterType;

    protected LocalDateTime createTime;

    protected LocalDateTime updateTime;

    private Integer status;
}
