/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.report.model.metrics;

import org.apache.eventmesh.dashboard.console.modle.vo.RuntimeIdDTO;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class AbstractMetricsDO extends RuntimeIdDTO {

    private String clusterName;

    private String runtimeName;

    private String runtimeType;

    private Long groupId;

    private String groupName;

    private Long subscribeId;

    private Long topicId;

    private String topicName;

    private String topicType;

    private LocalDateTime recordTime;

    private Long value;

}
