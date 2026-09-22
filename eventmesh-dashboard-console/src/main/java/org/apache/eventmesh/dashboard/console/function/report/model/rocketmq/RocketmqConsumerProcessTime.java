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

package org.apache.eventmesh.dashboard.console.function.report.model.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId.RuntimeFloatValue;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户端消费平均耗时，毫秒；沿用客户端分钟窗口及小时窗口回退语义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_ROCKETMQ, reportName = "rocketmq_consumer_process_time",
    defaultViewType = ReportViewType.GAUGE, tableName = "rocketmq_consumer_process_time",
    comment = "客户端消费平均耗时，毫秒；沿用客户端分钟窗口及小时窗口回退语义")
public class RocketmqConsumerProcessTime extends RuntimeFloatValue {

    @ReportTag
    private String topicName;

    @ReportTag
    private String groupName;

    @ReportTag
    private String clientId;

}
