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
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerGroupMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaPartitionOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaTopicMetrics;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKakfaConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 按 Kafka 集群采集 Broker 数量、Topic、分区位点和消费组指标，每个集群注册一个采集实例。
 * AdminClient 查询集群元数据和消费进度，节点流量及运行状态由 KafkaJmxCollect 采集。
 */
@Slf4j
@ClusterTypeMark(clusterType = {ClusterType.STORAGE_KAFKA_CLUSTER})
public class KafkaAdminCollect extends AbstractCollect {

    private static final int REQUEST_TIMEOUT_SECONDS = 5;

    private static final long UNAVAILABLE_VALUE = -1L;

    private final AtomicBoolean collecting = new AtomicBoolean();

    /** 用于构造 bootstrap 地址的节点列表，由采集管理器提供。 */
    @Setter
    private List<RuntimeMetadata> brokers = List.of();

    private ClusterMetadata clusterMetadata;

    private CollectMetadata collectMetadata;

    @Override
    public void setCollectMetadata(CollectMetadata collectMetadata) {
        super.setCollectMetadata(collectMetadata);
        this.collectMetadata = collectMetadata;
    }

    @Override
    public void restore(RestoreData restoreData) {
        // Admin 采集轮次相互独立，不能复用仍可能被其他线程持有的结果容器。
    }

    @Override
    public void setClusterMetadata(ClusterMetadata clusterMetadata) {
        super.setClusterMetadata(clusterMetadata);
        this.clusterMetadata = clusterMetadata;
    }

    private ClusterMetadata getClusterMetadata() {
        return clusterMetadata;
    }

    @Override
    public void collect(int index, DataSyncHandlerWrapper wrapper) {
        if (!collecting.compareAndSet(false, true)) {
            RestoreData empty = new RestoreData();
            empty.setAbstractCollect(this);
            empty.setCollectMetadata(collectMetadata);
            empty.setIndex(index);
            wrapper.sync(empty);
            return;
        }
        try {
            super.collect(index, wrapper);
        } finally {
            collecting.set(false);
        }
    }

    @Override
    protected void doCollect() {
        CreateKakfaConfig config = new CreateKakfaConfig();
        for (RuntimeMetadata broker : brokers) {
            if (broker.getHost() != null && !broker.getHost().isBlank() && broker.getPort() != null && broker.getPort() > 0) {
                config.addNetAddress(NetAddress.create(broker.getHost(), broker.getPort()));
            }
        }
        if (config.isNullAddress()) {
            log.warn("Kafka cluster {} has no bootstrap broker", getClusterMetadata().getId());
            return;
        }
        // 每轮创建并关闭客户端；先获取分区末端位点，再与消费组提交位点计算积压。
        try (AdminClient client = createClient(config)) {
            KafkaClusterMetrics cluster = new KafkaClusterMetrics();
            // 消费组请求可能失败，未知数量沿用 Kafka 采集的 -1 约定，避免误报为零或留下空数值。
            cluster.setValueConsumerGroupCount(UNAVAILABLE_VALUE);
            cluster.setValueBrokerCount((long) client.describeCluster().nodes().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS).size());
            Set<TopicPartition> partitions = collectTopics(client, cluster);
            Map<TopicPartition, Long> latestOffsets = collectPartitionOffsets(client, partitions);
            collectConsumerGroups(client, cluster, latestOffsets);
            setData(cluster);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kafka AdminClient collection interrupted for cluster {}", getClusterMetadata().getId(), e);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka AdminClient collection failed for cluster {}", getClusterMetadata().getId(), e);
        } catch (Exception e) {
            log.warn("Kafka AdminClient could not connect to cluster {}", getClusterMetadata().getId(), e);
        }
    }

    protected AdminClient createClient(CreateKakfaConfig config) throws Exception {
        String[] addresses = config.getNetAddresses();
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Kafka bootstrap servers are required");
        }
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", addresses));
        // 仅设置当前采集客户端的请求超时和默认 API 超时，不修改其他 SDK 操作的配置。
        // 各处 Future.get() 分别限制等待时间，整轮采集仍可能包含多个请求的等待。
        long requestTimeoutMillis = TimeUnit.SECONDS.toMillis(REQUEST_TIMEOUT_SECONDS);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) requestTimeoutMillis);
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) requestTimeoutMillis);
        // 单个不可达节点不能占满整轮期限，应留出时间连接其他 bootstrap 节点。
        properties.put(AdminClientConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, 2000L);
        properties.put(AdminClientConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG, 2000L);
        return AdminClient.create(properties);
    }

    /**
     * 统计非内部 Topic 的分区和副本状态，返回后续位点采集所需的分区集合。
     */
    private Set<TopicPartition> collectTopics(AdminClient client, KafkaClusterMetrics cluster)
        throws InterruptedException, ExecutionException, TimeoutException {
        Set<String> names = client.listTopics(new ListTopicsOptions().listInternal(false))
            .names().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        cluster.setValueTopicCount((long) names.size());
        Set<TopicPartition> partitions = new HashSet<>();
        if (names.isEmpty()) {
            cluster.setValuePartitionCount(0L);
            cluster.setValueOfflinePartitionCount(0L);
            return partitions;
        }
        Map<String, TopicDescription> descriptions =
            client.describeTopics(names).allTopicNames().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long offlineTotal = 0;
        for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
            KafkaTopicMetrics topic = new KafkaTopicMetrics();
            topic.setTopicName(entry.getKey());
            long replicaCount = 0;
            long underReplicated = 0;
            long offline = 0;
            // 副本数累加各分区的副本数量；ISR 不足和缺少 Leader 分别按分区计数。
            for (TopicPartitionInfo info : entry.getValue().partitions()) {
                partitions.add(new TopicPartition(entry.getKey(), info.partition()));
                replicaCount += info.replicas().size();
                if (info.isr().size() < info.replicas().size()) {
                    underReplicated++;
                }
                if (info.leader() == null || info.leader().id() < 0) {
                    offline++;
                }
            }
            topic.setValuePartitionCount((long) entry.getValue().partitions().size());
            topic.setValueReplicaCount(replicaCount);
            topic.setValueUnderReplicatedPartitionCount(underReplicated);
            topic.setValueOfflinePartitionCount(offline);
            setData(topic);
            offlineTotal += offline;
        }
        cluster.setValuePartitionCount((long) partitions.size());
        cluster.setValueOfflinePartitionCount(offlineTotal);
        return partitions;
    }

    /**
     * 采集分区最早保留位点和末端位点，并返回有效末端位点供消费积压计算使用。
     * 位点表示日志位置，不等同于消息数量或吞吐速率。
     */
    private Map<TopicPartition, Long> collectPartitionOffsets(AdminClient client, Set<TopicPartition> partitions) {
        Map<TopicPartition, Long> latest = new HashMap<>();
        if (partitions.isEmpty()) {
            return latest;
        }
        Map<TopicPartition, OffsetSpec> latestRequest = new HashMap<>();
        Map<TopicPartition, OffsetSpec> earliestRequest = new HashMap<>();
        partitions.forEach(partition -> {
            latestRequest.put(partition, OffsetSpec.latest());
            earliestRequest.put(partition, OffsetSpec.earliest());
        });
        try {
            Map<TopicPartition, ListOffsetsResultInfo> latestResult = readOffsets(client, latestRequest);
            // 最早位点失败不应丢弃已经取得的末端位点及消费积压。
            Map<TopicPartition, ListOffsetsResultInfo> earliestResult = readOffsets(client, earliestRequest);
            latestResult.forEach((partition, info) -> {
                // 无效位点不作为零值上报，避免影响后续消费积压计算。
                if (info.offset() < 0) {
                    return;
                }
                latest.put(partition, info.offset());
                KafkaPartitionOffsetMetrics metric = new KafkaPartitionOffsetMetrics();
                metric.setTopicName(partition.topic());
                metric.setPartitionIndex(String.valueOf(partition.partition()));
                metric.setValueLatestOffset(info.offset());
                ListOffsetsResultInfo earliestInfo = earliestResult.get(partition);
                // 最早位点不可用时保留错误标识，末端位点和后续积压计算仍使用已成功取得的结果。
                metric.setValueEarliestOffset(earliestInfo != null && earliestInfo.offset() >= 0
                    ? earliestInfo.offset() : UNAVAILABLE_VALUE);
                setData(metric);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kafka partition offset collection interrupted for cluster {}", getClusterMetadata().getId(), e);
        }
        return latest;
    }

    private Map<TopicPartition, ListOffsetsResultInfo> readOffsets(AdminClient client, Map<TopicPartition, OffsetSpec> request)
        throws InterruptedException {
        ListOffsetsResult response = client.listOffsets(request);
        Map<TopicPartition, ListOffsetsResultInfo> offsets = new HashMap<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(REQUEST_TIMEOUT_SECONDS);
        for (TopicPartition partition : request.keySet()) {
            try {
                long remaining = Math.max(0L, deadline - System.nanoTime());
                offsets.put(partition, response.partitionResult(partition).get(remaining, TimeUnit.NANOSECONDS));
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Kafka offset unavailable for cluster {} partition {}", getClusterMetadata().getId(), partition, e);
            }
        }
        return offsets;
    }

    /**
     * 统计消费组数量和成员数，并查询各组提交位点；组描述查询失败时仍尝试采集提交位点。
     */
    private void collectConsumerGroups(AdminClient client, KafkaClusterMetrics cluster, Map<TopicPartition, Long> latest) {
        try {
            Collection<ConsumerGroupListing> groups = client.listConsumerGroups().all().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            cluster.setValueConsumerGroupCount((long) groups.size());
            if (groups.isEmpty()) {
                return;
            }
            List<String> names = groups.stream().map(ConsumerGroupListing::groupId).toList();
            Map<String, ListConsumerGroupOffsetsResult> offsetRequests = new HashMap<>();
            // 先提交所有消费组位点请求，再逐组等待结果，减少逐组发起请求的串行开销。
            names.forEach(name -> offsetRequests.put(name, client.listConsumerGroupOffsets(name)));
            Map<String, ConsumerGroupDescription> descriptions = Map.of();
            try {
                descriptions = client.describeConsumerGroups(names).all().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Kafka consumer group description failed for cluster {}", getClusterMetadata().getId(), e);
            }
            for (String name : names) {
                ConsumerGroupDescription description = descriptions.get(name);
                if (description != null) {
                    KafkaConsumerGroupMetrics group = new KafkaConsumerGroupMetrics();
                    group.setGroupName(name);
                    group.setValueMemberCount((long) description.members().size());
                    setData(group);
                }
                collectConsumerOffsets(offsetRequests.get(name), name, latest);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kafka consumer group collection interrupted for cluster {}", getClusterMetadata().getId(), e);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka consumer group collection failed for cluster {}", getClusterMetadata().getId(), e);
        }
    }

    /**
     * 按“消费组、Topic、分区”记录提交位点及积压；请求异常或超时时跳过当前组，中断交由上层处理。
     */
    private void collectConsumerOffsets(ListConsumerGroupOffsetsResult request, String group, Map<TopicPartition, Long> latest)
        throws InterruptedException {
        try {
            Map<TopicPartition, OffsetAndMetadata> committed =
                request.partitionsToOffsetAndMetadata().get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            committed.forEach((partition, offset) -> {
                Long end = latest.get(partition);
                // 提交位点或末端位点缺失时无法计算积压，跳过该分区，避免误报为零。
                if (offset == null || offset.offset() < 0 || end == null) {
                    return;
                }
                KafkaConsumerOffsetMetrics metric = new KafkaConsumerOffsetMetrics();
                metric.setGroupName(group);
                metric.setTopicName(partition.topic());
                metric.setPartitionIndex(String.valueOf(partition.partition()));
                metric.setValueCommittedOffset(offset.offset());
                metric.setValueEndOffset(end);
                // 两类位点来自不同请求，提交位点可能暂时领先于采样到的末端位点，积压下限取零。
                metric.setValueLag(Math.max(0L, end - offset.offset()));
                setData(metric);
            });
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka consumer offset collection failed for cluster {} group {}", getClusterMetadata().getId(), group, e);
        }
    }
}
