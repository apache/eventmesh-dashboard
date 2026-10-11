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

import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResponse.Status;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResult;
import org.apache.eventmesh.dashboard.service.remoting.kafka.OffsetRemotingService;

import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KafkaOffsetRemotingService extends AbstractKafkaRemotingService implements OffsetRemotingService {

    @Override
    public GetOffsetResult getOffsets(GetOffsetRequest request) throws Exception {
        final String group = this.requireName(request == null ? null : request.getGroupName(), "groupName");
        this.rejectAddress(request.getBootstrapServers());
        List<TopicPartition> partitions = request.getTopic() == null ? null : this.partitions(request.getTopic(), null);
        ListConsumerGroupOffsetsOptions options = new ListConsumerGroupOffsetsOptions().timeoutMs(ADMIN_TIMEOUT_MS);
        if (partitions != null) {
            options.topicPartitions(partitions);
        }
        Map<TopicPartition, OffsetAndMetadata> committed = this.awaitResult(this.getClient()
            .listConsumerGroupOffsets(group, options).partitionsToOffsetAndMetadata());
        if (committed == null) {
            throw new IllegalStateException("Kafka committed offsets are missing");
        }
        if (partitions == null) {
            partitions = this.sorted(committed.keySet());
        }
        Map<TopicPartition, ListOffsetsResultInfo> ends = this.offsets(partitions, OffsetSpec.latest());
        List<GetOffsetResponse> rows = new ArrayList<>();
        for (TopicPartition partition : partitions) {
            GetOffsetResponse row = new GetOffsetResponse();
            row.setTopic(partition.topic());
            row.setPartitionId(partition.partition());
            OffsetAndMetadata offset = committed.get(partition);
            row.setOffset(offset == null ? null : offset.offset());
            row.setBrokerOffset(ends.get(partition).offset());
            // Latest offset is a log-end position, not the timestamp of the last message.
            rows.add(row);
        }
        GetOffsetResult result = new GetOffsetResult();
        result.setCode(200);
        result.setData(rows);
        return result;
    }

    @Override
    public ResetOffsetResult resetOffsets(ResetOffsetRequest request) throws Exception {
        final String group = this.requireName(request == null ? null : request.getGroupName(), "groupName");
        this.rejectAddress(request.getBootstrapServers());
        this.requireName(request.getTopic(), "topic");
        ResetOffsetMode mode = request.getResetOffsetMode();
        if (mode == null) {
            throw new IllegalArgumentException("resetOffsetMode is required");
        }
        if (mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET && (request.getOffset() == null || request.getOffset() < 0)) {
            throw new IllegalArgumentException("A nonnegative offset is required");
        }
        if (mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP && (request.getTimestamp() == null || request.getTimestamp() < 0)) {
            throw new IllegalArgumentException("A nonnegative timestamp in milliseconds is required");
        }
        if (mode != ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET && request.getOffset() != null
            || mode != ResetOffsetMode.CONSUME_FROM_TIMESTAMP && request.getTimestamp() != null) {
            throw new IllegalArgumentException("offset/timestamp must match resetOffsetMode");
        }
        List<TopicPartition> partitions = this.partitions(request.getTopic(), request.getPartitionId());
        Map<TopicPartition, ListOffsetsResultInfo> beginnings = this.offsets(partitions, OffsetSpec.earliest());
        Map<TopicPartition, ListOffsetsResultInfo> ends = this.offsets(partitions, OffsetSpec.latest());
        Map<TopicPartition, ListOffsetsResultInfo> timestamps = mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP
            ? this.offsets(partitions, OffsetSpec.forTimestamp(request.getTimestamp())) : Map.of();
        Map<TopicPartition, OffsetAndMetadata> targets = new LinkedHashMap<>();
        for (TopicPartition partition : partitions) {
            long offset;
            switch (mode) {
                case CONSUME_FROM_FIRST_OFFSET:
                    offset = beginnings.get(partition).offset();
                    break;
                case CONSUME_FROM_LAST_OFFSET:
                    offset = ends.get(partition).offset();
                    break;
                case CONSUME_FROM_TIMESTAMP:
                    offset = timestamps.get(partition).offset();
                    if (offset < 0) {
                        throw new IllegalArgumentException("No offset matches timestamp for " + partition + "; no offsets were changed");
                    }
                    break;
                case CONSUME_FROM_DESIGNATED_OFFSET:
                    offset = request.getOffset();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported resetOffsetMode");
            }
            if (offset < beginnings.get(partition).offset() || offset > ends.get(partition).offset()) {
                throw new IllegalArgumentException("Offset outside retained log range for " + partition + "; no offsets were changed");
            }
            targets.put(partition, new OffsetAndMetadata(offset));
        }
        // Check immediately before mutation. Broker remains authoritative if membership changes afterwards.
        var descriptions = this.awaitResult(this.getClient()
            .describeConsumerGroups(List.of(group), new DescribeConsumerGroupsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        var description = descriptions == null ? null : descriptions.get(group);
        if (description == null || !group.equals(description.groupId()) || description.members() == null) {
            throw new IllegalStateException("Kafka group description is incomplete: " + group);
        }
        if (description.state() != ConsumerGroupState.EMPTY || !description.members().isEmpty()) {
            throw new IllegalStateException("Offset reset requires an existing EMPTY consumer group: " + group);
        }
        AlterConsumerGroupOffsetsResult altered = this.getClient().alterConsumerGroupOffsets(group, targets,
            new AlterConsumerGroupOffsetsOptions().timeoutMs(ADMIN_TIMEOUT_MS));
        return this.outcomes(altered, targets);
    }

    private ResetOffsetResult outcomes(AlterConsumerGroupOffsetsResult altered, Map<TopicPartition, OffsetAndMetadata> targets) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ADMIN_TIMEOUT_MS);
        List<ResetOffsetResponse> rows = new ArrayList<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> target : targets.entrySet()) {
            ResetOffsetResponse row = new ResetOffsetResponse();
            row.setTopic(target.getKey().topic());
            row.setPartitionId(target.getKey().partition());
            row.setOffset(target.getValue().offset());
            try {
                altered.partitionResult(target.getKey()).get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                row.setStatus(Status.SUCCESS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.failure(row, e, Status.UNKNOWN);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                this.failure(row, cause, cause instanceof org.apache.kafka.common.errors.TimeoutException ? Status.UNKNOWN : Status.FAILED);
            } catch (TimeoutException e) {
                this.failure(row, e, Status.UNKNOWN);
            }
            rows.add(row);
        }
        boolean success = rows.stream().allMatch(row -> row.getStatus() == Status.SUCCESS);
        ResetOffsetResult result = new ResetOffsetResult();
        result.setCode(success ? 200 : 207);
        result.setData(rows);
        if (!success) {
            result.setMessage("Some partitions failed or have unknown outcomes; re-query committed offsets before retrying");
        }
        return result;
    }

    private void failure(ResetOffsetResponse row, Throwable error, Status status) {
        row.setStatus(status);
        row.setErrorCode(error.getClass().getSimpleName());
        row.setErrorMessage(error.getMessage());
        row.setThrowable(error);
    }

    private List<TopicPartition> partitions(String topic, Integer partitionId) throws Exception {
        this.requireName(topic, "topic");
        if (partitionId != null && partitionId < 0) {
            throw new IllegalArgumentException("partitionId must be nonnegative");
        }
        Map<String, TopicDescription> descriptions = this.awaitResult(this.getClient()
            .describeTopics(List.of(topic), new DescribeTopicsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).allTopicNames());
        TopicDescription description = descriptions == null ? null : descriptions.get(topic);
        if (description == null || !topic.equals(description.name()) || description.partitions() == null || description.partitions().isEmpty()) {
            throw new IllegalStateException("Kafka topic description is incomplete: " + topic);
        }
        List<TopicPartition> partitions = description.partitions().stream()
            .filter(info -> partitionId == null || info.partition() == partitionId)
            .map(info -> new TopicPartition(topic, info.partition())).collect(Collectors.toList());
        if (partitions.isEmpty()) {
            throw new IllegalArgumentException("partitionId does not exist in topic " + topic);
        }
        return this.sorted(partitions);
    }

    private Map<TopicPartition, ListOffsetsResultInfo> offsets(List<TopicPartition> partitions, OffsetSpec spec) throws Exception {
        if (partitions.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> query = new LinkedHashMap<>();
        partitions.forEach(partition -> query.put(partition, spec));
        Map<TopicPartition, ListOffsetsResultInfo> result = this.awaitResult(this.getClient()
            .listOffsets(query, new ListOffsetsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        for (TopicPartition partition : partitions) {
            if (result == null || result.get(partition) == null) {
                throw new IllegalStateException("Kafka offset response is incomplete: " + partition);
            }
            if (!(spec instanceof OffsetSpec.TimestampSpec) && result.get(partition).offset() < 0) {
                throw new IllegalStateException("Kafka log offset is unavailable: " + partition);
            }
        }
        return result;
    }

    private List<TopicPartition> sorted(Collection<TopicPartition> partitions) {
        return partitions.stream().sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
            .collect(Collectors.toList());
    }

    private void rejectAddress(String address) {
        if (address != null && !address.isBlank()) {
            throw new IllegalArgumentException("bootstrapServers is supplied by the registered client, not the operation request");
        }
    }
}
