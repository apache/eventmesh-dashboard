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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Setter;

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
            return;
        }
        this.defaultRemotingClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, this.runtimeUnique);
        new TopicCollect().collect();
    }

    /**
     * Topic 采集流程：
     * 1. 查询 Broker 上的 Topic 列表，逐个启动位点采集和消费组发现。
     * 2. 死信 Topic 只采集位点；普通 Topic 继续查询消费组连接和消费进度。
     * 3. 连接查询按消费组去重，响应转换为模型后直接调用 setData，不额外缓存结果。
     * 4. 等待所有异步请求完成后返回，父类再统一交付本轮数据。
     * 5. 超时或中断时保留已经采集的数据，关闭本轮写入，拒绝迟到回调。
     */
    private class TopicCollect {
        private final DefaultRemotingClient client = defaultRemotingClient;
        private final AtomicInteger pending = new AtomicInteger();
        private final CountDownLatch completed = new CountDownLatch(1);
        private final Set<String> connectedGroups = ConcurrentHashMap.newKeySet();
        private final long deadline = System.currentTimeMillis() + 4000;
        private final LocalDateTime sampleTime = LocalDateTime.now();
        private volatile boolean closed;

        private void collect() {
            try {
                this.collectTopics();
                // 所有请求完成时计数归零并唤醒；超时则保留已采集的数据。
                this.completed.await(Math.max(1, this.deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.close();
            }
        }

        // 父类的结果容器不是线程安全的；写入和关闭共用锁，保证返回后不会再写入父类。
        private synchronized void setData(OrganizationId data) {
            if (this.closed || System.currentTimeMillis() >= this.deadline) {
                return;
            }
            data.setTime(this.sampleTime);
            RocketMQCollect.this.setData(data);
        }

        private synchronized void close() {
            this.closed = true;
        }

        private void collectTopics() {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
            new TopicsCallback(request).execute();
        }

        private void collectTopic(String topic) {
            this.collectTopicStats(topic);
            if (!topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                this.collectGroups(topic);
            }
        }

        private void collectTopicStats(String topic) {
            GetTopicStatsInfoRequestHeader topicHeader = new GetTopicStatsInfoRequestHeader();
            topicHeader.setTopic(topic);
            RemotingCommand topicRequest = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_STATS_INFO, topicHeader);
            new TopicStatsCallback(topicRequest, topic).execute();
        }

        private void collectGroups(String topic) {
            QueryTopicConsumeByWhoRequestHeader groupHeader = new QueryTopicConsumeByWhoRequestHeader();
            groupHeader.setTopic(topic);
            RemotingCommand groupRequest = RemotingCommand.createRequestCommand(RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, groupHeader);
            new GroupsCallback(groupRequest, topic).execute();
        }

        private void collectConnections(String group) {
            GetConsumerConnectionListRequestHeader connectionHeader = new GetConsumerConnectionListRequestHeader();
            connectionHeader.setConsumerGroup(group);
            RemotingCommand connectionRequest = RemotingCommand.createRequestCommand(
                RequestCode.GET_CONSUMER_CONNECTION_LIST, connectionHeader);
            new ConnectionsCallback(connectionRequest, group).execute();
        }

        private void collectConsumeStats(String topic, String group) {
            GetConsumeStatsRequestHeader consumeHeader = new GetConsumeStatsRequestHeader();
            consumeHeader.setTopic(topic);
            consumeHeader.setConsumerGroup(group);
            RemotingCommand consumeRequest = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUME_STATS, consumeHeader);
            new ConsumeStatsCallback(consumeRequest, topic, group).execute();
        }

        /** 步骤一：解析 Broker 的 Topic 列表，逐个启动采集。 */
        private class TopicsCallback extends CollectCallback {
            private TopicsCallback(RemotingCommand request) {
                super(request);
            }

            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                TopicConfigSerializeWrapper topics =
                    RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), TopicConfigSerializeWrapper.class);
                for (String topic : topics.getTopicConfigTable().keySet()) {
                    collectTopic(topic);
                }
            }
        }

        /** 步骤二：采集队列位点，并计算 Topic 汇总位点。 */
        private class TopicStatsCallback extends CollectCallback {
            private final String topic;

            private TopicStatsCallback(RemotingCommand request, String topic) {
                super(request);
                this.topic = topic;
            }

            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                TopicStatsTable stats =
                    RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), TopicStatsTable.class);
                stats.getOffsetTable().forEach((queue, offset) -> {
                    String queueId = String.valueOf(queue.getQueueId());

                    TopicCollect.this.setData(MAPPER.topicOffset(topic, queueId, offset));
                });
                long min = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMinOffset()).sum();
                long max = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMaxOffset()).sum();
                long lastUpdate = stats.getOffsetTable().values().stream()
                    .mapToLong(offset -> offset.getLastUpdateTimestamp()).max().orElse(0L);

                TopicCollect.this.setData(MAPPER.aggregateOffset(topic, min, max, lastUpdate));
            }
        }

        /** 步骤三：发现 Topic 对应的消费组，启动连接与消费进度采集。 */
        private class GroupsCallback extends CollectCallback {
            private final String topic;

            private GroupsCallback(RemotingCommand request, String topic) {
                super(request);
                this.topic = topic;
            }

            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                GroupList groups = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), GroupList.class);
                for (String group : groups.getGroupList()) {
                    if (TopicCollect.this.connectedGroups.add(group)) {
                        collectConnections(group);
                    }
                    collectConsumeStats(topic, group);
                }
            }
        }

        /** 步骤四：采集消费组连接数；同一消费组每轮只查询一次。 */
        private class ConnectionsCallback extends CollectCallback {
            private final String group;

            private ConnectionsCallback(RemotingCommand request, String group) {
                super(request);
                this.group = group;
            }

            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                ConsumerConnection connections = RemotingSerializable.decode(
                    responseFuture.getResponseCommand().getBody(), ConsumerConnection.class);

                TopicCollect.this.setData(MAPPER.connections(group, connections.getConnectionSet().size()));
            }
        }

        /** 步骤五：采集 Topic 与消费组对应的队列位点和积压量。 */
        private class ConsumeStatsCallback extends CollectCallback {
            private final String topic;
            private final String group;

            private ConsumeStatsCallback(RemotingCommand request, String topic, String group) {
                super(request);
                this.topic = topic;
                this.group = group;
            }

            @Override
            protected void handleResponse(ResponseFuture responseFuture) {
                ConsumeStats stats = RemotingSerializable.decode(
                    responseFuture.getResponseCommand().getBody(), ConsumeStats.class);
                stats.getOffsetTable().forEach((queue, offset) -> {
                    if (!topic.equals(queue.getTopic())) {
                        return;
                    }
                    String queueId = String.valueOf(queue.getQueueId());

                    TopicCollect.this.setData(MAPPER.consumerOffset(topic, group, queueId, offset));
                });
            }
        }

        /** 统一处理请求发送、响应校验、异常和完成计数，具体响应由各采集回调解析。 */
        private abstract class CollectCallback implements InvokeCallback {
            private final RemotingCommand request;
            private final AtomicBoolean finished = new AtomicBoolean();

            private CollectCallback(RemotingCommand request) {
                this.request = request;
                // 请求发送前 +1；子请求先计数，父请求处理结束后才 -1。
                TopicCollect.this.pending.incrementAndGet();
            }

            // 发送异常也进入失败回调，每个请求只扣减一次。
            protected final void execute() {
                executeSafely(this,
                    () -> client.invokeAsync(this.request, this.timeoutMillis(), this));
            }

            private long timeoutMillis() throws TimeoutException {
                long remaining = TopicCollect.this.deadline - System.currentTimeMillis();
                if (TopicCollect.this.closed || remaining <= 0) {
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
                    if (TopicCollect.this.closed) {
                        return;
                    }
                    RemotingCommand response = responseFuture.getResponseCommand();
                    if (response == null) {
                        throw new IllegalStateException("Missing RocketMQ response");
                    }
                    if (response.getCode() != ResponseCode.SUCCESS) {
                        return;
                    }
                    this.handleResponse(responseFuture);
                } catch (Exception e) {
                    this.restoreInterrupt(e);
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
                    this.restoreInterrupt(cause);
                } finally {
                    this.completeRequest();
                }
            }

            private void restoreInterrupt(Throwable cause) {
                if (cause instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }

            private void completeRequest() {
                // 成功、失败都 -1；finished 防止重复回调重复扣减。
                if (TopicCollect.this.pending.decrementAndGet() == 0) {
                    TopicCollect.this.completed.countDown();
                }
            }

            protected abstract void handleResponse(ResponseFuture responseFuture);
        }


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

}
