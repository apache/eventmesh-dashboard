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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.annotation.ClusterTypeMark;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTopicTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;

import java.util.Set;
import java.util.function.Consumer;

import javax.management.MBeanServerConnection;

/**
 * 通过节点 JMX 采集 Kafka 流量和运行状态，每个节点对应一个采集实例。
 * 所有样本保留 Broker 维度，Topic 流量额外保留 Topic 名称。
 */
@ClusterTypeMark(clusterType = {ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT})
public class KafkaJmxCollect extends JMXCollect {

    private static final String BROKER_TOPIC_METRICS = "kafka.server:type=BrokerTopicMetrics,name=";

    private static final String REPLICA_MANAGER = "kafka.server:type=ReplicaManager,name=";

    private static final String PURGATORY = "kafka.server:type=DelayedOperationPurgatory,name=PurgatorySize,delayedOperation=";

    private static final String REQUEST_IDLE = "kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent";

    private static final String REQUEST_LATENCY = "kafka.network:type=RequestMetrics,name=TotalTimeMs,request=";

    @Override
    protected void setData(OrganizationId data) {
        // 节点类型属于 Kafka 维度，通用 JMX 基类不负责填充。
        RuntimeMetadata runtimeMetadata = getRuntimeMetadata();
        if (data instanceof RuntimeId runtime && runtimeMetadata != null && runtimeMetadata.getClusterType() != null) {
            runtime.setRuntimeType(runtimeMetadata.getClusterType().name());
        }
        super.setData(data);
    }

    @Override
    protected void doJmxCollect() {
        // 连接、关闭与查询容错由 JMX 基类处理，Kafka 仅负责指标和维度组装。
        withConnection((connection, samples) -> {
            collectBrokerTraffic(connection, samples);
            collectTopicTraffic(connection, samples);
            collectBrokerHealth(connection, samples);
        });
    }

    /**
     * 采集 Broker 维度的累计流量；部分属性缺失时统一填充错误值 -1，至少一个字段有效才保存样本。
     */
    private void collectBrokerTraffic(MBeanServerConnection connection, Consumer<OrganizationId> samples) {
        // 名称中的 PerSec 不代表此处采集速率；读取 Count 属性，保留累计值口径。
        KafkaBrokerTrafficMetrics traffic = new KafkaBrokerTrafficMetrics();
        // Broker 总体消息数与收发字节数，直接读取总量 MBean，不累加 Topic 样本。
        traffic.setValueMessagesInTotal(readLong(connection, BROKER_TOPIC_METRICS + "MessagesInPerSec", "Count"));
        traffic.setValueBytesInTotal(readLong(connection, BROKER_TOPIC_METRICS + "BytesInPerSec", "Count"));
        traffic.setValueBytesOutTotal(readLong(connection, BROKER_TOPIC_METRICS + "BytesOutPerSec", "Count"));

        // 客户端生产与拉取请求总数，以及对应的失败请求数。
        traffic.setValueProduceRequestsTotal(readLong(connection, BROKER_TOPIC_METRICS + "TotalProduceRequestsPerSec", "Count"));
        traffic.setValueFailedProduceRequestsTotal(readLong(connection, BROKER_TOPIC_METRICS + "FailedProduceRequestsPerSec", "Count"));
        traffic.setValueFetchRequestsTotal(readLong(connection, BROKER_TOPIC_METRICS + "TotalFetchRequestsPerSec", "Count"));
        traffic.setValueFailedFetchRequestsTotal(readLong(connection, BROKER_TOPIC_METRICS + "FailedFetchRequestsPerSec", "Count"));

        // 拒绝的字节数与 Broker 之间的副本复制流量。
        traffic.setValueBytesRejectedTotal(readLong(connection, BROKER_TOPIC_METRICS + "BytesRejectedPerSec", "Count"));
        traffic.setValueReplicationBytesInTotal(readLong(connection, BROKER_TOPIC_METRICS + "ReplicationBytesInPerSec", "Count"));
        traffic.setValueReplicationBytesOutTotal(readLong(connection, BROKER_TOPIC_METRICS + "ReplicationBytesOutPerSec", "Count"));

        if (hasValue(traffic.getValueMessagesInTotal(), traffic.getValueBytesInTotal(), traffic.getValueBytesOutTotal(),
            traffic.getValueProduceRequestsTotal(), traffic.getValueFailedProduceRequestsTotal(), traffic.getValueFetchRequestsTotal(),
            traffic.getValueFailedFetchRequestsTotal(), traffic.getValueBytesRejectedTotal(), traffic.getValueReplicationBytesInTotal(),
            traffic.getValueReplicationBytesOutTotal())) {
            samples.accept(traffic);
        }
    }

    /**
     * 采集 Broker 的当前运行状态与请求统计，和累计流量分别保存，避免混淆指标口径。
     */
    private void collectBrokerHealth(MBeanServerConnection connection, Consumer<OrganizationId> samples) {
        KafkaBrokerHealthMetrics health = new KafkaBrokerHealthMetrics();
        // 分区与副本状态：同步不足、离线副本、ISR 下限和分区迁移情况。
        health.setValueUnderReplicatedPartitions(readLong(connection, REPLICA_MANAGER + "UnderReplicatedPartitions", "Value"));
        health.setValuePartitionCount(readLong(connection, REPLICA_MANAGER + "PartitionCount", "Value"));
        health.setValueLeaderCount(readLong(connection, REPLICA_MANAGER + "LeaderCount", "Value"));
        health.setValueOfflineReplicaCount(readLong(connection, REPLICA_MANAGER + "OfflineReplicaCount", "Value"));
        health.setValueUnderMinIsrPartitionCount(readLong(connection, REPLICA_MANAGER + "UnderMinIsrPartitionCount", "Value"));
        health.setValueAtMinIsrPartitionCount(readLong(connection, REPLICA_MANAGER + "AtMinIsrPartitionCount", "Value"));
        health.setValueReassigningPartitions(readLong(connection, REPLICA_MANAGER + "ReassigningPartitions", "Value"));

        // 汇总本 Broker 的分区日志大小；任一分区读取失败时保留未知值。
        health.setValueLogSizeBytes(sumLong(connection, "kafka.log:type=Log,name=Size,*", "Value"));

        // 延迟操作等待队列的当前大小，区分 Produce 与 Fetch。
        health.setValueProducePurgatorySize(readLong(connection, PURGATORY + "Produce", "Value"));
        health.setValueFetchPurgatorySize(readLong(connection, PURGATORY + "Fetch", "Value"));

        // 请求处理线程的空闲比例，以及生产、拉取请求的平均和 P99 延迟。
        health.setValueRequestHandlerIdleRatio(readFloat(connection, REQUEST_IDLE, "OneMinuteRate"));
        health.setValueProduceRequestMeanMs(readFloat(connection, REQUEST_LATENCY + "Produce", "Mean"));
        health.setValueProduceRequestP99Ms(readFloat(connection, REQUEST_LATENCY + "Produce", "99thPercentile"));
        health.setValueFetchRequestMeanMs(readFloat(connection, REQUEST_LATENCY + "Fetch", "Mean"));
        health.setValueFetchRequestP99Ms(readFloat(connection, REQUEST_LATENCY + "Fetch", "99thPercentile"));

        if (hasValue(health.getValueUnderReplicatedPartitions(), health.getValuePartitionCount(), health.getValueLeaderCount(),
            health.getValueOfflineReplicaCount(), health.getValueUnderMinIsrPartitionCount(), health.getValueAtMinIsrPartitionCount(),
            health.getValueReassigningPartitions(), health.getValueLogSizeBytes(), health.getValueProducePurgatorySize(),
            health.getValueFetchPurgatorySize(), health.getValueRequestHandlerIdleRatio(), health.getValueProduceRequestMeanMs(),
            health.getValueProduceRequestP99Ms(), health.getValueFetchRequestMeanMs(), health.getValueFetchRequestP99Ms())) {
            samples.accept(health);
        }
    }

    /** 将同一 Broker、同一 Topic 的消息数和收发字节数合并为一条样本。 */
    private void collectTopicTraffic(MBeanServerConnection connection, Consumer<OrganizationId> samples) {
        // 分组、缺失值和遍历均封装在通用 JMX 能力中，此处只组装单个 Topic 样本。
        collectGroupedLongs(connection,
            BROKER_TOPIC_METRICS + "*,topic=*", "topic", "name", "Count",
            Set.of("MessagesInPerSec", "BytesInPerSec", "BytesOutPerSec"), (topic, values) -> {
                KafkaBrokerTopicTrafficMetrics traffic = new KafkaBrokerTopicTrafficMetrics();
                traffic.setTopicName(topic);
                // 同一 Topic 的部分 MBean 缺失时保持错误值，避免将缺失转换为真实零。
                traffic.setValueMessagesInTotal(values.get("MessagesInPerSec"));
                traffic.setValueBytesInTotal(values.get("BytesInPerSec"));
                traffic.setValueBytesOutTotal(values.get("BytesOutPerSec"));
                if (hasValue(traffic.getValueMessagesInTotal(), traffic.getValueBytesInTotal(), traffic.getValueBytesOutTotal())) {
                    samples.accept(traffic);
                }
            });
    }
}
