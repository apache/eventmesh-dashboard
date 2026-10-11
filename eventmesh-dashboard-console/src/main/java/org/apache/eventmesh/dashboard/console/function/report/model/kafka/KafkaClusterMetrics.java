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
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AdminClient 每个集群产生一条样本；所有字段均为集群级瞬时数量。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_cluster_metrics",
    defaultViewType = ReportViewType.GAUGE, tableName = "kafka_cluster_metrics", comment = "Kafka 集群拓扑指标")
public class KafkaClusterMetrics extends ClusterId {

    /** 集群中当前可见的 Broker 数。 */

    private Long valueBrokerCount;

    /** 非内部 Topic 数。 */

    private Long valueTopicCount;

    /** 非内部 Topic 的分区总数。 */

    private Long valuePartitionCount;

    /** 非内部 Topic 中没有 Leader 的分区数。 */

    private Long valueOfflinePartitionCount;

    /** AdminClient 可见的消费组数。 */
    private Long valueConsumerGroupCount;

}
