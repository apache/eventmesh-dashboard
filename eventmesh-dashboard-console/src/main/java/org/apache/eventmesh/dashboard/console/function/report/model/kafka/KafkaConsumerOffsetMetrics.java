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
 * 每个消费组、Topic 和分区一条样本；未提交位点不写成零。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_consumer_offset_metrics",
    defaultViewType = ReportViewType.GAUGE, tableName = "kafka_consumer_offset_metrics", comment = "Kafka 消费组分区位点与积压指标")
public class KafkaConsumerOffsetMetrics extends ClusterId {

    @ReportTag

    private String groupName;

    @ReportTag

    private String topicName;

    @ReportTag

    private String partitionIndex;

    /** 消费组已提交的位点。 */

    private Long valueCommittedOffset;

    /** 分区日志末端位点。 */

    private Long valueEndOffset;

    /** 日志末端减已提交位点，下限为零。 */

    private Long valueLag;

}
