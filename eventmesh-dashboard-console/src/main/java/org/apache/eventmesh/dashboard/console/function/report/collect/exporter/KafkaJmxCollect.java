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
import org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTopicTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import lombok.extern.slf4j.Slf4j;

/**
 * 通过节点 JMX 采集 Kafka 流量和运行状态，每个节点对应一个采集实例。
 * 所有样本保留 Broker 维度，Topic 流量额外保留 Topic 名称。
 */
@Slf4j
@ClusterTypeMark(clusterType = {ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT})
public class KafkaJmxCollect extends AbstractCollect {

    private RuntimeMetadata runtimeMetadata;

    @Override
    public void setRuntimeMetadata(RuntimeMetadata runtimeMetadata) {
        super.setRuntimeMetadata(runtimeMetadata);
        this.runtimeMetadata = runtimeMetadata;
    }

    private RuntimeMetadata getRuntimeMetadata() {
        return runtimeMetadata;
    }

    @Override
    public synchronized void collect(int index, DataSyncHandlerWrapper wrapper) {
        // JMX 请求可能跨越调度周期，同一实例串行执行，避免覆盖父类中的本轮采集缓冲。
        super.collect(index, wrapper);
    }

    private static final String BROKER_TOPIC_METRICS = "kafka.server:type=BrokerTopicMetrics,name=";

    private static final String REPLICA_MANAGER = "kafka.server:type=ReplicaManager,name=";

    private static final String PURGATORY = "kafka.server:type=DelayedOperationPurgatory,name=PurgatorySize,delayedOperation=";

    private static final String REQUEST_IDLE = "kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent";

    private static final String REQUEST_LATENCY = "kafka.network:type=RequestMetrics,name=TotalTimeMs,request=";

    private static final Map<String, Object> JMX_ENVIRONMENT = Map.of(
            "com.sun.jndi.rmi.connect.timeout", "5000",
            "com.sun.jndi.rmi.read.timeout", "5000",
            "jmx.remote.x.request.waiting.timeout", 5000L);

    @Override
    protected void doCollect() {
        RuntimeMetadata broker = getRuntimeMetadata();
        if (broker == null || broker.getHost() == null || broker.getHost().isBlank()
            || broker.getJmxPort() == null || broker.getJmxPort() <= 0) {
            return;
        }
        // 使用独立的 JMX 端口连接当前节点，每轮结束后关闭连接。
        String address = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", broker.getHost(), broker.getJmxPort());
        try (JMXConnector connector = connect(new JMXServiceURL(address))) {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            // 采集 Broker 累计流量：消息数、收发字节数、请求数和副本复制流量。
            collectBrokerTraffic(connection);
            // 采集当前 Broker 下各 Topic 的累计流量：消息数和收发字节数。
            collectTopicTraffic(connection);
            // 采集 Broker 运行状态与请求统计：分区、副本、日志大小、等待队列、空闲比例和请求延迟。
            collectBrokerHealth(connection);
        } catch (IOException e) {
            log.warn("Kafka JMX collection failed for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
        } catch (SecurityException e) {
            log.warn("Kafka JMX access denied for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
        }
    }

    /**
     * 采集 Broker 维度的累计流量；部分属性缺失时保留 null，至少一个字段有效才保存样本。
     */
    private void collectBrokerTraffic(MBeanServerConnection connection) {
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
            setData(traffic);
        }
    }

    /**
     * 采集 Broker 的当前运行状态与请求统计，和累计流量分别保存，避免混淆指标口径。
     */
    private void collectBrokerHealth(MBeanServerConnection connection) {
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
        health.setValueLogSizeBytes(readLogSize(connection));

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
            setData(health);
        }
    }

    protected JMXConnector connect(JMXServiceURL url) throws IOException {
        return JMXConnectorFactory.connect(url, JMX_ENVIRONMENT);
    }

    /**
     * 读取整数指标；属性不可用时保留缺失值，允许本轮继续采集其他指标。
     */
    private Long readLong(MBeanServerConnection connection, String name, String attribute) {
        try {
            Object value = connection.getAttribute(new ObjectName(name), attribute);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (JMException | JMRuntimeException | IOException e) {
            log.warn("Kafka JMX attribute {}:{} is unavailable for broker {}", name, attribute, getRuntimeMetadata().getName(), e);
        }
        return null;
    }

    /**
     * 读取浮点指标，过滤 NaN 和无穷值，缺失或无效的结果返回 null。
     */
    private Float readFloat(MBeanServerConnection connection, String name, String attribute) {
        try {
            Object value = connection.getAttribute(new ObjectName(name), attribute);
            if (value instanceof Number number && Float.isFinite(number.floatValue())) {
                return number.floatValue();
            }
        } catch (JMException | JMRuntimeException | IOException e) {
            log.warn("Kafka JMX attribute {}:{} is unavailable for broker {}", name, attribute, getRuntimeMetadata().getName(), e);
        }
        return null;
    }

    /**
     * 累加当前 Broker 的各分区日志大小；任一读取失败或求和溢出时返回 null，避免上报不完整的总量。
     */
    private Long readLogSize(MBeanServerConnection connection) {
        try {
            Set<ObjectName> names = connection.queryNames(new ObjectName("kafka.log:type=Log,name=Size,*"), null);
            if (names.isEmpty()) {
                return null;
            }
            long total = 0;
            for (ObjectName name : names) {
                Object value = connection.getAttribute(name, "Value");
                if (!(value instanceof Number number)) {
                    return null;
                }
                total = Math.addExact(total, number.longValue());
            }
            return total;
        } catch (JMException | JMRuntimeException | IOException | ArithmeticException e) {
            log.warn("Kafka log size is unavailable for broker {}", getRuntimeMetadata().getName(), e);
            return null;
        }
    }

    /**
     * 将同一 Broker、同一 Topic 的消息数和收发字节数合并为一条样本。
     */
    private void collectTopicTraffic(MBeanServerConnection connection) {
        Map<String, KafkaBrokerTopicTrafficMetrics> topics = new HashMap<>();
        collectTopicCounter(connection, "MessagesInPerSec", topics, KafkaBrokerTopicTrafficMetrics::setValueMessagesInTotal);
        collectTopicCounter(connection, "BytesInPerSec", topics, KafkaBrokerTopicTrafficMetrics::setValueBytesInTotal);
        collectTopicCounter(connection, "BytesOutPerSec", topics, KafkaBrokerTopicTrafficMetrics::setValueBytesOutTotal);
        topics.values().forEach(this::setData);
    }

    private void collectTopicCounter(MBeanServerConnection connection, String name,
        Map<String, KafkaBrokerTopicTrafficMetrics> topics, BiConsumer<KafkaBrokerTopicTrafficMetrics, Long> setter) {
        try {
            // 只匹配带 topic 标签的 MBean，与上面的 Broker 总量分开采集，避免混淆维度。
            Set<ObjectName> names = connection.queryNames(new ObjectName(BROKER_TOPIC_METRICS + name + ",topic=*"), null);
            for (ObjectName objectName : names) {
                Long value = readLong(connection, objectName.toString(), "Count");
                if (value == null) {
                    continue;
                }
                String topicName = objectName.getKeyProperty("topic");
                KafkaBrokerTopicTrafficMetrics metric = topics.computeIfAbsent(topicName, topic -> {
                    KafkaBrokerTopicTrafficMetrics result = new KafkaBrokerTopicTrafficMetrics();
                    result.setTopicName(topic);
                    return result;
                });
                setter.accept(metric, value);
            }
        } catch (JMException | JMRuntimeException | IOException e) {
            log.warn("Kafka topic JMX metric {} is unavailable for broker {}", name, getRuntimeMetadata().getName(), e);
        }
    }

    /** 至少取得一个有效指标时才生成样本，避免把全部缺失的采集结果当作正常数据。 */
    private boolean hasValue(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }
}
