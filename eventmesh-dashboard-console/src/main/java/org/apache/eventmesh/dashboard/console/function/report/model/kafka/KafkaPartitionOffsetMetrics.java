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
 * 每个 Topic 分区一条样本；起止位点来自同一轮 AdminClient 查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_partition_offset_metrics",
    defaultViewType = ReportViewType.GAUGE, tableName = "kafka_partition_offset_metrics", comment = "Kafka 分区位点指标")
public class KafkaPartitionOffsetMetrics extends ClusterId {

    @ReportTag

    private String topicName;

    @ReportTag

    private String partitionIndex;

    /** 分区当前最早可读取的位点。 */

    private Long valueEarliestOffset;

    /** 分区下一条待写入消息的位点。 */

    private Long valueLatestOffset;

}
