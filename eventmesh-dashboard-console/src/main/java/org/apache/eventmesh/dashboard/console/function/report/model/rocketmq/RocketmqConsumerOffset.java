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
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_ROCKETMQ, reportName = "rocketmq_consumer_offset",
    defaultViewType = ReportViewType.GAUGE, tableName = "rocketmq_consumer_offset",
    comment = "消费组队列位点及位点差")
public class RocketmqConsumerOffset extends RuntimeId {

    @ReportTag
    private String topicName;

    @ReportTag
    private String groupName;

    private String queueId;

    private Long valueConsumerOffset;

    private Long valueBrokerOffset;

    /** Broker 位点减去已提交位点，下限为零；表示位点差。 */
    private Long valueOffsetLag;

}
