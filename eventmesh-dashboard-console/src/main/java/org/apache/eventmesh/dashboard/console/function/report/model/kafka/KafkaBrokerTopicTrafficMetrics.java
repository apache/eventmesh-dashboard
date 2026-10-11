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

package org.apache.eventmesh.dashboard.console.function.report.model.kafka;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Broker JMX 每个 Broker 和 Topic 产生一条样本；Count 为累计值，速率由相邻样本计算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_broker_topic_traffic_metrics",
    defaultViewType = ReportViewType.COUNTER, tableName = "kafka_broker_topic_traffic_metrics",
    comment = "Kafka Broker Topic 累计流量指标")
public class KafkaBrokerTopicTrafficMetrics extends RuntimeId {

    @ReportTag
    private String topicName;

    /** 该 Broker 上 Topic 的累计写入消息数。 */
    private Long valueMessagesInTotal;

    /** 该 Broker 上 Topic 的累计入站字节数。 */
    private Long valueBytesInTotal;

    /** 该 Broker 上 Topic 的累计出站字节数。 */
    private Long valueBytesOutTotal;

}
