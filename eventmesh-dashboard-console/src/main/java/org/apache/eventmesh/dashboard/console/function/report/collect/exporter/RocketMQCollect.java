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
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.mapstruct.report.RocketMQCollectMapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.GroupList;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetTopicStatsInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.QueryTopicConsumeByWhoRequestHeader;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ClusterTypeMark(clusterType = {ClusterType.STORAGE_ROCKETMQ_CLUSTER, ClusterType.STORAGE_ROCKETMQ_BROKER,
    ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE, ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT})
public class RocketMQCollect extends AbstractCollect {
    private static final RocketMQCollectMapper MAPPER = RocketMQCollectMapper.INSTANCE;

    private String runtimeUnique;

    @Setter
    private DefaultRemotingClient defaultRemotingClient;

    @Override
    public void setRuntimeMetadata(RuntimeMetadata runtimeMetadata) {
        super.setRuntimeMetadata(runtimeMetadata);
        this.runtimeUnique = runtimeMetadata == null ? null : runtimeMetadata.getUnique();
    }

    @Override
    protected void doCollect() {
        if (this.runtimeUnique == null) {
            log.warn("RocketMQ collection requires Broker runtime metadata");
            return;
        }
        this.defaultRemotingClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, this.runtimeUnique);
        CollectRound round = new CollectRound();
        this.collectTopics(round);
        int phase = round.pending.arriveAndDeregister();
        try {
            round.pending.awaitAdvanceInterruptibly(phase, Math.max(1, round.deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("RocketMQ collection interrupted runtime={}", this.runtimeUnique);
            return;
        } catch (TimeoutException e) {
            log.warn("RocketMQ collection timed out; incomplete round discarded runtime={}", this.runtimeUnique);
            return;
        } finally {
            round.pending.forceTermination();
        }
        // Only this collecting thread writes to the parent's current RestoreData.
        round.rows.forEach(row -> {
            row.setTime(round.sampleTime);
            this.setData(row);
        });
    }

    private void collectTopics(CollectRound round) {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
        CollectCallback invokeCallback = new CollectCallback(round, request) {
            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                TopicConfigSerializeWrapper topics =
                    RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), TopicConfigSerializeWrapper.class);
                for (String topic : topics.getTopicConfigTable().keySet()) {
                    collectTopic(round, topic);
                }
            }
        };
        executeSafely(invokeCallback,
            () -> this.defaultRemotingClient.invokeAsync(request, invokeCallback.timeoutMillis(), invokeCallback));
    }

    private void collectTopic(CollectRound round, String topic) {
        this.collectTopicStats(round, topic);
        if (!topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
            this.collectGroups(round, topic);
        }
    }

    private void collectTopicStats(CollectRound round, String topic) {
        GetTopicStatsInfoRequestHeader topicHeader = new GetTopicStatsInfoRequestHeader();
        topicHeader.setTopic(topic);
        RemotingCommand topicRequest = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_STATS_INFO, topicHeader);
        CollectCallback topicCallback = new CollectCallback(round, topicRequest) {
            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                TopicStatsTable stats =
                    RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), TopicStatsTable.class);
                stats.getOffsetTable().forEach((queue, offset) -> {
                    String queueId = String.valueOf(queue.getQueueId());
                    round.rows.add(MAPPER.topicOffset(topic, queueId, offset));
                    RocketMQCollect.this.metric("queue_min_offset", "队列最小位点", topic, "", queueId, offset.getMinOffset());
                    RocketMQCollect.this.metric("queue_max_offset", "队列最大位点", topic, "", queueId, offset.getMaxOffset());
                    RocketMQCollect.this.metric("queue_last_update_ms", "队列最后更新时间（毫秒）", topic, "", queueId,
                        offset.getLastUpdateTimestamp());
                });
                long min = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMinOffset()).sum();
                long max = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMaxOffset()).sum();
                long lastUpdate = stats.getOffsetTable().values().stream()
                    .mapToLong(offset -> offset.getLastUpdateTimestamp()).max().orElse(0L);
                round.rows.add(MAPPER.aggregateOffset(topic, min, max, lastUpdate));
                RocketMQCollect.this.metric("topic_min_offset_sum", "Topic 最小位点之和", topic, "", "", min);
                RocketMQCollect.this.metric("topic_max_offset_sum", "Topic 最大位点之和", topic, "", "", max);
            }
        };
        executeSafely(topicCallback,
            () -> defaultRemotingClient.invokeAsync(topicRequest, topicCallback.timeoutMillis(), topicCallback));
    }

    private void collectGroups(CollectRound round, String topic) {
        QueryTopicConsumeByWhoRequestHeader groupHeader = new QueryTopicConsumeByWhoRequestHeader();
        groupHeader.setTopic(topic);
        RemotingCommand groupRequest = RemotingCommand.createRequestCommand(RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, groupHeader);
        CollectCallback groupCallback = new CollectCallback(round, groupRequest) {
            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                GroupList groups = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), GroupList.class);
                for (String group : groups.getGroupList()) {
                    if (round.connectedGroups.add(group)) {
                        collectConnections(round, group);
                    }
                    collectConsumeStats(round, topic, group);
                }
            }
        };
        executeSafely(groupCallback,
            () -> defaultRemotingClient.invokeAsync(groupRequest, groupCallback.timeoutMillis(), groupCallback));
    }

    private void collectConnections(CollectRound round, String group) {
        GetConsumerConnectionListRequestHeader connectionHeader = new GetConsumerConnectionListRequestHeader();
        connectionHeader.setConsumerGroup(group);
        RemotingCommand connectionRequest = RemotingCommand.createRequestCommand(
            RequestCode.GET_CONSUMER_CONNECTION_LIST, connectionHeader);
        CollectCallback connectionCallback = new CollectCallback(round, connectionRequest) {
            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                ConsumerConnection connections = RemotingSerializable.decode(
                    responseFuture.getResponseCommand().getBody(), ConsumerConnection.class);
                round.rows.add(MAPPER.connections(group, connections.getConnectionSet().size()));
                RocketMQCollect.this.metric("consumer_connection_count", "消费组连接数", "", group, "",
                    connections.getConnectionSet().size());
                connections.getConnectionSet().forEach(connection -> log.info(
                    "RocketMQ connection runtime={} group={} clientId={} address={} language={} version={}",
                    runtimeUnique, group, connection.getClientId(), connection.getClientAddr(),
                    connection.getLanguage(), connection.getVersion()));
            }
        };
        executeSafely(connectionCallback,
            () -> defaultRemotingClient.invokeAsync(
                connectionRequest, connectionCallback.timeoutMillis(), connectionCallback));
    }

    private void collectConsumeStats(CollectRound round, String topic, String group) {
        GetConsumeStatsRequestHeader consumeHeader = new GetConsumeStatsRequestHeader();
        consumeHeader.setTopic(topic);
        consumeHeader.setConsumerGroup(group);
        RemotingCommand consumeRequest = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUME_STATS, consumeHeader);
        CollectCallback consumeCallback = new CollectCallback(round, consumeRequest) {
            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                ConsumeStats stats = RemotingSerializable.decode(
                    responseFuture.getResponseCommand().getBody(), ConsumeStats.class);
                stats.getOffsetTable().forEach((queue, offset) -> {
                    if (!topic.equals(queue.getTopic())) {
                        return;
                    }
                    String queueId = String.valueOf(queue.getQueueId());
                    round.rows.add(MAPPER.consumerOffset(topic, group, queueId, offset));
                    RocketMQCollect.this.metric("consumer_offset", "消费组已提交位点", topic, group, queueId,
                        offset.getConsumerOffset());
                    RocketMQCollect.this.metric("broker_offset", "Broker 位点", topic, group, queueId,
                        offset.getBrokerOffset());
                    RocketMQCollect.this.metric("offset_lag", "消费位点差", topic, group, queueId,
                        Math.max(0L, offset.getBrokerOffset() - offset.getConsumerOffset()));
                });
            }
        };
        executeSafely(consumeCallback,
            () -> defaultRemotingClient.invokeAsync(consumeRequest, consumeCallback.timeoutMillis(), consumeCallback));
    }

    private static void executeSafely(InvokeCallback callback, RequestAction action) {
        try {
            action.run();
        } catch (Exception e) {
            callback.operationFail(e);
        }
    }

    @FunctionalInterface
    private interface RequestAction {
        void run() throws Exception;
    }

    /** Common completion accounting and error handling for every collection request. */
    private abstract class CollectCallback implements InvokeCallback {
        private final CollectRound round;
        private final RemotingCommand request;
        private final boolean registered;
        private final AtomicBoolean finished = new AtomicBoolean();

        private CollectCallback(CollectRound round, RemotingCommand request) {
            this.round = round;
            this.request = request;
            this.registered = round.pending.register() >= 0;
        }

        private long timeoutMillis() throws TimeoutException {
            long remaining = this.round.deadline - System.currentTimeMillis();
            if (!this.registered || this.round.pending.isTerminated() || remaining <= 0) {
                throw new TimeoutException("RocketMQ collection deadline reached");
            }
            return Math.min(3000, remaining);
        }

        @Override
        public final void operationComplete(ResponseFuture responseFuture) {
            if (!this.finished.compareAndSet(false, true)) {
                return;
            }
            try {
                if (this.round.pending.isTerminated()) {
                    return;
                }
                RemotingCommand response = responseFuture.getResponseCommand();
                if (response == null) {
                    throw new IllegalStateException("Missing RocketMQ response");
                }
                if (response.getCode() != ResponseCode.SUCCESS) {
                    log.warn("RocketMQ collection response runtime={} requestCode={} header={} responseCode={} remark={}",
                        runtimeUnique, this.request.getCode(), this.request.readCustomHeader(), response.getCode(), response.getRemark());
                    return;
                }
                this.handleResponse(responseFuture);
            } catch (Exception e) {
                this.reportFailure(e);
            } finally {
                this.completeRequest();
            }
        }

        @Override
        public final void operationFail(Throwable cause) {
            if (!this.finished.compareAndSet(false, true)) {
                return;
            }
            try {
                this.reportFailure(cause);
            } finally {
                this.completeRequest();
            }
        }

        private void reportFailure(Throwable cause) {
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("RocketMQ collection failed runtime={} requestCode={} header={}",
                runtimeUnique, this.request.getCode(), this.request.readCustomHeader(), cause);
        }

        private void completeRequest() {
            if (this.registered) {
                this.round.pending.arriveAndDeregister();
            }
        }

        protected abstract void handleResponse(ResponseFuture responseFuture);
    }

    private static class CollectRound {
        private final Phaser pending = new Phaser(1);
        private final Set<String> connectedGroups = ConcurrentHashMap.newKeySet();
        private final long deadline = System.currentTimeMillis() + 4000;
        private final LocalDateTime sampleTime = LocalDateTime.now();
        private final Queue<OrganizationId> rows = new ConcurrentLinkedQueue<>();
    }

    private void metric(String name, String meaning, String topic, String group, String queue, Object value) {
        log.info("采集时间={} 实例={} 指标={} 中文含义={} Topic={} Group={} Queue={} 数值={}",
            LocalDateTime.now(), this.runtimeUnique, name, meaning, topic, group, queue, value);
    }
}
