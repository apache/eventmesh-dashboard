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

/** Broker 原生滚动采样窗口的取出消息数及消息 TPS；继承的 value 为消息数/秒，不代表消费确认。 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_ROCKETMQ, reportName = "rocketmq_broker_messages_out",
    defaultViewType = ReportViewType.GAUGE, tableName = "rocketmq_broker_messages_out",
    comment = "Broker 滚动采样窗口取出消息数及消息 TPS（消息数/秒），非消费确认数")
public class RocketmqBrokerMessagesOut extends RuntimeFloatValue {

    @ReportTag
    private String window;

    /** 滚动采样窗口内的取出消息数，不是累计计数或已确认消费数。 */
    private Long valueWindowCount;

}
