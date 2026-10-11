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
 * Broker JMX 每个节点产生一条样本；健康值与累计流量分开存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_KAFKA, reportName = "kafka_broker_health_metrics",
    defaultViewType = ReportViewType.GAUGE, tableName = "kafka_broker_health_metrics", comment = "Kafka Broker 健康指标")
public class KafkaBrokerHealthMetrics extends RuntimeId {

    /** ReplicaManager.UnderReplicatedPartitions 的 Value，单位为分区。 */

    private Long valueUnderReplicatedPartitions;

    /** 当前节点持有的分区副本数。 */
    private Long valuePartitionCount;

    /** 当前节点担任 Leader 的分区数。 */
    private Long valueLeaderCount;

    /** 当前节点的离线副本数。 */
    private Long valueOfflineReplicaCount;

    /** 当前节点低于最小 ISR 要求的分区数。 */
    private Long valueUnderMinIsrPartitionCount;

    /** 当前节点恰好达到最小 ISR 要求的分区数。 */
    private Long valueAtMinIsrPartitionCount;

    /** 当前节点正在重新分配的分区数。 */
    private Long valueReassigningPartitions;

    /** 当前 Broker 本地所有日志分区的大小之和，单位为字节。 */
    private Long valueLogSizeBytes;

    /** 等待完成的 Produce 请求数量。 */
    private Long valueProducePurgatorySize;

    /** 等待完成的 Fetch 请求数量。 */
    private Long valueFetchPurgatorySize;

    /** 请求处理线程过去一分钟的平均空闲比例；保留 JMX 原值，采样误差可能使其略高于 1。 */
    private Float valueRequestHandlerIdleRatio;

    /** Broker Produce 请求平均总耗时，单位为毫秒。 */
    private Float valueProduceRequestMeanMs;

    /** Broker Produce 请求总耗时的 99 分位数，单位为毫秒。 */
    private Float valueProduceRequestP99Ms;

    /** Broker Fetch 请求平均总耗时，单位为毫秒。 */
    private Float valueFetchRequestMeanMs;

    /** Broker Fetch 请求总耗时的 99 分位数，单位为毫秒。 */
    private Float valueFetchRequestP99Ms;

}
