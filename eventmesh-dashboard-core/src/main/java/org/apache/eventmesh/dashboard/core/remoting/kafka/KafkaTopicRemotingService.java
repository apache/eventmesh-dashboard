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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.kafka.TopicRemotingService;

import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

public class KafkaTopicRemotingService extends AbstractKafkaRemotingService implements TopicRemotingService {

    private static final int OPERATION_TIMEOUT_MS = 10000;

    @Override
    public CreateTopicResult createTopic(CreateTopic2Request request) throws Exception {
        TopicMetadata topic = this.requireTopic(request == null ? null : request.getMetaData());
        if (Objects.isNull(this.partitionCount(topic)) || this.partitionCount(topic) <= 0) {
            throw new IllegalArgumentException("A positive partitionCount is required for creation");
        }
        if (Objects.nonNull(topic.getReplicationFactor()) && (topic.getReplicationFactor() <= 0
            || topic.getReplicationFactor() > Short.MAX_VALUE)) {
            throw new IllegalArgumentException("replicationFactor must be between 1 and 32767");
        }
        Map<String, String> configs = this.validateConfigs(topic.getTopicConfig());
        NewTopic newTopic = new NewTopic(topic.getTopicName(), Optional.of(this.partitionCount(topic)),
            Optional.ofNullable(topic.getReplicationFactor()).map(Integer::shortValue));
        newTopic.configs(configs);
        this.await(this.getClient().createTopics(List.of(newTopic), new CreateTopicsOptions().timeoutMs(OPERATION_TIMEOUT_MS)).all());
        CreateTopicResult result = new CreateTopicResult();
        result.setCode(200);
        return result;
    }

    @Override
    public DeleteTopicResult deleteTopic(DeleteTopicRequest request) throws Exception {
        TopicMetadata topic = this.requireTopic(request == null ? null : request.getMetaData());
        this.await(this.getClient().deleteTopics(List.of(topic.getTopicName()), new DeleteTopicsOptions().timeoutMs(OPERATION_TIMEOUT_MS)).all());
        DeleteTopicResult result = new DeleteTopicResult();
        result.setCode(200);
        return result;
    }

    @Override
    public CreateTopicResult updateTopic(CreateTopic2Request request) throws Exception {
        TopicMetadata topic = this.requireTopic(request == null ? null : request.getMetaData());
        Integer targetCount = this.partitionCount(topic);
        if (Objects.nonNull(targetCount) && targetCount <= 0) {
            throw new IllegalArgumentException("partitionCount must be positive");
        }
        Map<String, String> configs = this.validateConfigs(topic.getTopicConfig());
        if (Objects.isNull(targetCount) && configs.isEmpty()) {
            throw new IllegalArgumentException("Specify partitionCount or at least one config to update");
        }
        Map<String, TopicDescription> descriptions = this.await(this.getClient()
            .describeTopics(List.of(topic.getTopicName()), new DescribeTopicsOptions().timeoutMs(OPERATION_TIMEOUT_MS)).allTopicNames());
        TopicDescription current = Objects.isNull(descriptions) ? null : descriptions.get(topic.getTopicName());
        if (Objects.isNull(current) || !Objects.equals(current.name(), topic.getTopicName())
            || Objects.isNull(current.partitions()) || current.partitions().isEmpty()) {
            throw new IllegalStateException("Kafka topic description is incomplete: " + topic.getTopicName());
        }
        if (topic.getReplicationFactor() != null && current.partitions().stream()
            .anyMatch(partition -> partition.replicas().size() != topic.getReplicationFactor())) {
            throw new IllegalArgumentException("Replication changes require partition reassignment and are not supported");
        }
        int currentCount = current.partitions().size();
        if (Objects.nonNull(targetCount) && targetCount < currentCount) {
            throw new IllegalArgumentException("Kafka partitions cannot shrink from " + currentCount + " to " + targetCount);
        }
        boolean expand = Objects.nonNull(targetCount) && targetCount > currentCount;
        Map<String, NewPartitions> partitions = expand ? Map.of(topic.getTopicName(), NewPartitions.increaseTo(targetCount)) : Map.of();
        List<AlterConfigOp> operations = new ArrayList<>();
        configs.forEach((key, value) -> operations.add(new AlterConfigOp(new ConfigEntry(key, value), AlterConfigOp.OpType.SET)));
        Map<ConfigResource, Collection<AlterConfigOp>> changes =
            Map.of(new ConfigResource(ConfigResource.Type.TOPIC, topic.getTopicName()), operations);
        // 两类修改分别预校验，避免已知非法配置造成不可逆的分区扩容。
        if (expand) {
            this.await(this.getClient().createPartitions(partitions,
                new CreatePartitionsOptions().validateOnly(true).timeoutMs(OPERATION_TIMEOUT_MS)).all());
        }
        if (!operations.isEmpty()) {
            this.await(this.getClient().incrementalAlterConfigs(changes,
                new AlterConfigsOptions().validateOnly(true).timeoutMs(OPERATION_TIMEOUT_MS)).all());
        }
        if (expand) {
            this.await(this.getClient().createPartitions(partitions, new CreatePartitionsOptions().timeoutMs(OPERATION_TIMEOUT_MS)).all());
        }
        if (!operations.isEmpty()) {
            try {
                this.await(this.getClient().incrementalAlterConfigs(changes, new AlterConfigsOptions().timeoutMs(OPERATION_TIMEOUT_MS)).all());
            } catch (Exception e) {
                if (expand) {
                    throw new IllegalStateException("Topic " + topic.getTopicName() + " expanded to " + targetCount
                        + " partitions, but configuration update did not complete successfully; re-query before retrying", e);
                }
                throw e;
            }
        }
        CreateTopicResult result = new CreateTopicResult();
        result.setCode(200);
        return result;
    }

    private TopicMetadata requireTopic(TopicMetadata topic) {
        if (Objects.isNull(topic)) {
            throw new IllegalArgumentException("Topic metadata is required");
        }
        String name = topic.getTopicName();
        if (Objects.isNull(name) || name.isEmpty() || name.length() > 249 || name.equals(".") || name.equals("..")
            || !name.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid Kafka topic name");
        }
        return topic;
    }

    private Integer partitionCount(TopicMetadata topic) {
        if (topic.getReadQueueNum() != null && topic.getWriteQueueNum() != null
            && !Objects.equals(topic.getReadQueueNum(), topic.getWriteQueueNum())) {
            throw new IllegalArgumentException("Kafka readQueueNum and writeQueueNum must match");
        }
        return topic.getWriteQueueNum() == null ? topic.getReadQueueNum() : topic.getWriteQueueNum();
    }

    private Map<String, String> validateConfigs(String json) {
        Map<?, ?> configs = null;
        if (json != null) {
            Object parsed = JSON.parse(json);
            if (!(parsed instanceof Map)) {
                throw new IllegalArgumentException("topicConfig must be a JSON object of string values");
            }
            configs = (Map<?, ?>) parsed;
        }
        Map<String, String> copy = new LinkedHashMap<>();
        if (Objects.nonNull(configs)) {
            configs.forEach((key, value) -> {
                if (!(key instanceof String) || ((String) key).isBlank() || !key.equals(((String) key).trim())
                    || !(value instanceof String)) {
                    throw new IllegalArgumentException("Config keys must be nonblank and values must be non-null");
                }
                copy.put((String) key, (String) value);
            });
        }
        return copy;
    }

    private <T> T await(KafkaFuture<T> future) throws Exception {
        try {
            return future.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopics2Request request) throws Exception {
        try {
            // 查询业务主题，再批量读取分区信息；连接目标始终来自托管客户端。
            Set<String> names = this.getClient().listTopics(new ListTopicsOptions().listInternal(false).timeoutMs(OPERATION_TIMEOUT_MS))
                .names().get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (Objects.isNull(names)) {
                throw new IllegalStateException("Kafka topic list is missing");
            }
            List<String> sortedNames = new ArrayList<>(names);
            Collections.sort(sortedNames);
            List<TopicMetadata> topics = new ArrayList<>();
            if (!sortedNames.isEmpty()) {
                Map<String, TopicDescription> descriptions = this.getClient()
                    .describeTopics(sortedNames, new DescribeTopicsOptions().timeoutMs(OPERATION_TIMEOUT_MS))
                    .allTopicNames().get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                for (String name : sortedNames) {
                    TopicDescription description = Objects.isNull(descriptions) ? null : descriptions.get(name);
                    if (Objects.isNull(description) || !Objects.equals(name, description.name())
                        || Objects.isNull(description.partitions()) || description.partitions().isEmpty()) {
                        throw new IllegalStateException("Kafka topic description is incomplete: " + name);
                    }
                    TopicMetadata topic = new TopicMetadata();
                    topic.setTopicName(name);
                    // Kafka 的读写使用同一组分区；其余未查询的配置保持为空。
                    topic.setReadQueueNum(description.partitions().size());
                    topic.setWriteQueueNum(description.partitions().size());
                    int replicas = description.partitions().get(0).replicas().size();
                    if (description.partitions().stream().allMatch(partition -> partition.replicas().size() == replicas)) {
                        topic.setReplicationFactor(replicas);
                    }
                    topics.add(topic);
                }
            }
            GetTopicsResult result = new GetTopicsResult();
            result.setCode(200);
            result.setData(topics);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
