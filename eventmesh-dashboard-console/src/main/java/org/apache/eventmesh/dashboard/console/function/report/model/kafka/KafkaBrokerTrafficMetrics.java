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
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Broker JMX 每个节点产生一条样本；Count 是进程生命周期累计值，不是每秒速率。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_broker_traffic_metrics",
    defaultViewType = ReportViewType.COUNTER, tableName = "kafka_broker_traffic_metrics", comment = "Kafka Broker 累计流量指标")
public class KafkaBrokerTrafficMetrics extends RuntimeId {

    /** BrokerTopicMetrics.MessagesInPerSec 的 Count，单位为消息。 */

    private Long valueMessagesInTotal;

    /** BrokerTopicMetrics.BytesInPerSec 的 Count，单位为字节。 */

    private Long valueBytesInTotal;

    /** Broker 累计发出的字节数。 */
    private Long valueBytesOutTotal;

    /** Broker 累计收到的 Produce 请求数。 */
    private Long valueProduceRequestsTotal;

    /** Broker 累计失败的 Produce 请求数。 */
    private Long valueFailedProduceRequestsTotal;

    /** Broker 累计收到的 Fetch 请求数。 */
    private Long valueFetchRequestsTotal;

    /** Broker 累计失败的 Fetch 请求数。 */
    private Long valueFailedFetchRequestsTotal;

    /** Broker 累计拒绝的字节数。 */
    private Long valueBytesRejectedTotal;

    /** Broker 累计接收的复制流量，单位为字节。 */
    private Long valueReplicationBytesInTotal;

    /** Broker 累计发出的复制流量，单位为字节。 */
    private Long valueReplicationBytesOutTotal;

}
