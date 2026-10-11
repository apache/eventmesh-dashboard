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
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 每个非内部 Topic 一条样本；名称用于区分不同 Topic。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_topic_metrics",
    defaultViewType = ReportViewType.GAUGE, tableName = "kafka_topic_metrics", comment = "Kafka Topic 副本与分区指标")
public class KafkaTopicMetrics extends ClusterId {

    @ReportTag
    private String topicName;

    /** Topic 的分区数量。 */
    private Long valuePartitionCount;

    /** Topic 各分区副本数之和。 */
    private Long valueReplicaCount;

    /** Topic 中 ISR 小于副本数的分区数量。 */
    private Long valueUnderReplicatedPartitionCount;

    /** Topic 中没有 Leader 的分区数量。 */
    private Long valueOfflinePartitionCount;

}
