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

package org.apache.eventmesh.dashboard.console.function.report.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GeneralReportDO {

    private Long organizationId;

    private Long clustersId;

    private String reportName;

    private String reportType;

    private Long runtimeId;

    private String runtimeType;

    private Long topicId;

    private Long groupId;

    /**
     * 查询条件 与 分组条件
     */
    private LocalDateTime startTime;

    /**
     * 查询条件 与 分组条件
     */
    private LocalDateTime endTime;

    /**
     * 分组的时间间隔
     */
    private String interval;

    private String selectFun;

    /**
     *
     */
    private String orderFun;



}
