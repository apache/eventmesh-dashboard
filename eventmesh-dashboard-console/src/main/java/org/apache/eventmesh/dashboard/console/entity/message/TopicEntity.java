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

package org.apache.eventmesh.dashboard.console.entity.message;

import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true, exclude = "status")
public class TopicEntity extends BaseRuntimeIdEntity {


    private String topicType;

    private String topicName;


    /**
     *
     */
    private Long numQueue;

    /**
     * 副本个数
     */
    private Short replicationFactor;

    /**
     * topic 拦截器类型
     */
    private String topicFilterType;

    /**
     * 不确定参数
     */
    private String attributes;

    private String order;

    @Schema(description = "time to live in milliseconds, -2 unknown, -1 no limit;", example = "1000")
    private Long retentionMs;


    private String description;


    private Integer createProgress;

}
