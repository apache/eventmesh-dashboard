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


package org.apache.eventmesh.dashboard.console.model.dto.topic;


import org.apache.eventmesh.dashboard.console.model.dto.operation.OperationBaseDTO;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TODO this class is copied from storage plugin, needs update
 * 
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateTopicDTO extends OperationBaseDTO {

    private Integer cleanupStrategy;

    private String topicType;

    @NotBlank(message = "topic 名不能为空")
    private String name;


    /**
     *
     */
    @NotNull(message = "队列数量不能为空")
    @Positive(message = "队列数量必须大于0")
    private Integer readQueueNum;

    private Integer writeQueueNum;


    private Long saveTime;

    /**
     * 副本个数
     */
    private Integer replicationFactor;

    /**
     * topic 拦截器类型
     */
    private String topicFilterType;

    /**
     * 不确定参数
     */
    private String attributes;

    private Integer order;


    @NotBlank(message = "topic 说明不能为空")
    private String description;
}
