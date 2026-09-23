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
import org.apache.rocketmq.remoting.protocol.header.GetAllSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerRunningInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetTopicStatsInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.QueryConsumeTimeSpanRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.QueryTopicConsumeByWhoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ViewBrokerStatsDataRequestHeader;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Setter;

@ClusterTypeMark(clusterType = {ClusterType.STORAGE_ROCKETMQ_CLUSTER, ClusterType.STORAGE_ROCKETMQ_BROKER,
    ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE, ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT})
public class RocketMQCollect extends AbstractCollect {
    private static final RocketMQCollectMapper MAPPER = RocketMQCollectMapper.INSTANCE;
    private static final Pattern BYTE_COUNT = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(B|[KMGTPE]iB)");

    private static final Pattern DISK_CAPACITY = Pattern.compile("Total : (.+), Free : (.+)\\.");

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
        new CollectRound().executeAndAwait();
    }

    /**
     * 一轮采集流程：
     * 1. 先注册 Topic 和 Broker 两个方向的根请求，再分别启动，统一管理请求计数。
     * 2. TopicCollect 采集 Topic 总数、位点、积压时间、消息统计及客户端处理指标；死信 Topic 不查询消费组。
     * 3. BrokerCollect 采集配置消费组总数、消息统计和运行状态；响应直接调用父类 setData。
     * 4. 等待所有异步请求完成后返回，父类再统一交付本轮数据。
     * 5. 超时或中断时保留已经采集的数据，关闭本轮，拒绝尚未开始处理的迟到回调。
     */
    private class CollectRound {
        private final DefaultRemotingClient client = defaultRemotingClient;
        private final AtomicInteger pending = new AtomicInteger();
        private final CountDownLatch completed = new CountDownLatch(1);
        private final long deadline = System.currentTimeMillis() + 4000;
        private volatile boolean closed;

        private void executeAndAwait() {
            try {
                // 先注册所有根请求，避免同步回调在后续请求注册前让计数归零。
                TopicCollect topics = new TopicCollect();
                BrokerCollect broker = new BrokerCollect();
                topics.start();
                broker.start();
                // 所有请求完成时计数归零并唤醒；超时则保留已采集的数据。
                this.completed.await(Math.max(1, this.deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.close();
            }
        }

        private void close() {
            this.closed = true;
        }

        /** 采集 Topic 总数、生产和消费位点、积压量及时间、消息统计、消费组连接数和客户端处理指标。 */
        private class TopicCollect {
            private final Set<String> connectedGroups = ConcurrentHashMap.newKeySet();
            private final TopicsCallback topics =
                new TopicsCallback(RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null));

            private void start() {
                this.topics.execute();
            }

            private void collectTopic(String topic) {
                this.collectTopicStats(topic);
                this.collectMessages(topic, null);
                if (!topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                    this.collectGroups(topic);
                }
            }

            /** 原生统计键：写入按 Topic，取出按 Topic@消费组。 */
            private void collectMessages(String topic, String group) {
                ViewBrokerStatsDataRequestHeader header = new ViewBrokerStatsDataRequestHeader();
                header.setStatsName(group == null ? "TOPIC_PUT_NUMS" : "GROUP_GET_NUMS");
                header.setStatsKey(group == null ? topic : topic + "@" + group);
                new MessageStatsCallback(RemotingCommand.createRequestCommand(RequestCode.VIEW_BROKER_STATS_DATA, header),
                    topic, group).execute();
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
                    // 缺少列表不是零；仅对成功返回的完整配置表计数。
                    Map<?, ?> body = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                    if (body == null || !(body.get("topicConfigTable") instanceof Map<?, ?>)) {
                        return;
                    }
                    RocketMQCollect.this.setData(MAPPER.topicNumber((long) topics.getTopicConfigTable().size()));
                    for (String topic : topics.getTopicConfigTable().keySet()) {
                        collectTopic(topic);
                    }
                }
            }

            private class MessageStatsCallback extends WindowStatsCallback {
                private final String topic;
                private final String group;

                private MessageStatsCallback(RemotingCommand request, String topic, String group) {
                    super(request);
                    this.topic = topic;
                    this.group = group;
                }

                @Override
                protected void recordWindow(String window, long count, float rate) {
                    RocketMQCollect.this.setData(this.group == null
                        ? MAPPER.topicMessages(this.topic, window, count, rate)
                        : MAPPER.groupMessages(this.topic, this.group, window, count, rate));
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

                        RocketMQCollect.this.setData(MAPPER.topicOffset(topic, queueId, offset));
                    });
                    long min = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMinOffset()).sum();
                    long max = stats.getOffsetTable().values().stream().mapToLong(offset -> offset.getMaxOffset()).sum();
                    long lastUpdate = stats.getOffsetTable().values().stream()
                        .mapToLong(offset -> offset.getLastUpdateTimestamp()).max().orElse(0L);

                    RocketMQCollect.this.setData(MAPPER.aggregateOffset(topic, min, max, lastUpdate));
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
                        collectMessages(topic, group);
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

                    RocketMQCollect.this.setData(MAPPER.connections(group, connections.getConnectionSet().size()));
                    // 每个在线客户端分别采样，不把 Broker 取消息 TPS 当成处理成功 TPS。
                    Set<String> clients = ConcurrentHashMap.newKeySet();
                    connections.getConnectionSet().forEach(connection -> {
                        String clientId = connection.getClientId();
                        if (clientId != null && !clientId.isBlank() && clients.add(clientId)) {
                            GetConsumerRunningInfoRequestHeader header = new GetConsumerRunningInfoRequestHeader();
                            header.setConsumerGroup(this.group);
                            header.setClientId(clientId);
                            header.setJstackEnable(false);
                            new ClientStatsCallback(RemotingCommand.createRequestCommand(
                                RequestCode.GET_CONSUMER_RUNNING_INFO, header), this.group, clientId).execute();
                        }
                    });
                }
            }

            /** 按 Topic、消费组和客户端采集消费成功 TPS、失败 TPS 和平均处理耗时。 */
            private class ClientStatsCallback extends CollectCallback {
                private final String group;
                private final String clientId;

                private ClientStatsCallback(RemotingCommand request, String group, String clientId) {
                    super(request);
                    this.group = group;
                    this.clientId = clientId;
                }

                @Override
                protected void handleResponse(ResponseFuture responseFuture) {
                    Map<?, ?> body = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                    if (body == null || !(body.get("statusTable") instanceof Map<?, ?> statuses)) {
                        return;
                    }
                    statuses.forEach((key, value) -> {
                        if (!(key instanceof String topic) || !(value instanceof Map<?, ?> status)) {
                            return;
                        }
                        Float success = nonnegativeFloat(status.get("consumeOKTPS"));
                        Float failed = nonnegativeFloat(status.get("consumeFailedTPS"));
                        Float elapsed = nonnegativeFloat(status.get("consumeRT"));
                        if (success != null) {
                            RocketMQCollect.this.setData(MAPPER.consumerSuccess(topic, this.group, this.clientId, success));
                        }
                        if (failed != null) {
                            RocketMQCollect.this.setData(MAPPER.consumerFailed(topic, this.group, this.clientId, failed));
                        }
                        if (elapsed != null) {
                            RocketMQCollect.this.setData(MAPPER.consumerProcessTime(topic, this.group, this.clientId, elapsed));
                        }
                    });
                }
            }

            /** 采集各 Topic、消费组和队列中最早未消费消息的积压时间。 */
            private class LagCallback extends CollectCallback {
                private final String topic;
                private final String group;

                private LagCallback(RemotingCommand request, String topic, String group) {
                    super(request);
                    this.topic = topic;
                    this.group = group;
                }

                @Override
                protected void handleResponse(ResponseFuture responseFuture) {
                    Map<?, ?> body = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                    if (body == null || !(body.get("consumeTimeSpanSet") instanceof List<?> spans)) {
                        return;
                    }
                    for (Object item : spans) {
                        if (!(item instanceof Map<?, ?> span) || !(span.get("messageQueue") instanceof Map<?, ?> queue)
                            || !this.topic.equals(queue.get("topic"))) {
                            continue;
                        }
                        Long queueId = nonnegativeLong(queue.get("queueId"));
                        Long delay = nonnegativeLong(span.get("delayTime"));
                        Long earliest = nonnegativeLong(span.get("minTimeStamp"));
                        // 位点已过期或未初始化时，原生接口可能用 -1 时间戳计算出近似当前纪元的延迟。
                        if (queueId != null && delay != null && earliest != null && earliest > 0
                            && delay <= System.currentTimeMillis() - earliest) {
                            RocketMQCollect.this.setData(MAPPER.consumerLag(this.topic, this.group, queueId.toString(), delay));
                        }
                    }
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

                        RocketMQCollect.this.setData(MAPPER.consumerOffset(topic, group, queueId, offset));
                    });
                    if (!stats.getOffsetTable().isEmpty()) {
                        QueryConsumeTimeSpanRequestHeader header = new QueryConsumeTimeSpanRequestHeader();
                        header.setTopic(this.topic);
                        header.setGroup(this.group);
                        new LagCallback(RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUME_TIME_SPAN, header),
                            this.topic, this.group).execute();
                    }
                }
            }
        }

        /** 采集消费组总数、Broker 消息统计、存储积压、消息保留时间、磁盘使用率及剩余空间、线程池队列长度。 */
        private class BrokerCollect {
            private final BrokerConfigCallback config = new BrokerConfigCallback();
            private final BrokerRuntimeCallback runtime = new BrokerRuntimeCallback();
            private final GroupCountCallback groups = new GroupCountCallback();

            private void start() {
                this.config.execute();
                this.runtime.execute();
                this.groups.execute();
            }

            /** 采集 Broker 配置的消费组总数。 */
            private class GroupCountCallback extends CollectCallback {
                private GroupCountCallback() {
                    super(RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG,
                        new GetAllSubscriptionGroupRequestHeader()));
                }

                @Override
                protected void handleResponse(ResponseFuture responseFuture) {
                    Map<?, ?> body = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                    if (body != null && body.get("subscriptionGroupTable") instanceof Map<?, ?> groups) {
                        RocketMQCollect.this.setData(MAPPER.groupNumber((long) groups.size()));
                    }
                }
            }

            private void collectStats(String cluster, boolean incoming) {
                ViewBrokerStatsDataRequestHeader header = new ViewBrokerStatsDataRequestHeader();
                header.setStatsName(incoming ? "BROKER_PUT_NUMS" : "BROKER_GET_NUMS");
                header.setStatsKey(cluster);
                new BrokerStatsCallback(RemotingCommand.createRequestCommand(RequestCode.VIEW_BROKER_STATS_DATA, header), incoming).execute();
            }

            /** 步骤一：获取 Broker 所属集群名，启动消息写入和取出统计采集。 */
            private class BrokerConfigCallback extends CollectCallback {
                private BrokerConfigCallback() {
                    super(RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONFIG, null));
                }

                @Override
                protected void handleResponse(ResponseFuture responseFuture) throws Exception {
                    Properties properties = new Properties();
                    properties.load(new StringReader(new String(responseFuture.getResponseCommand().getBody(), StandardCharsets.UTF_8)));
                    String cluster = properties.getProperty("brokerClusterName");
                    if (cluster == null || cluster.isBlank()) {
                        return;
                    }
                    collectStats(cluster, true);
                    collectStats(cluster, false);
                }
            }

            /** 步骤二：采集分钟、小时、日窗口的消息写入和取出数量及 TPS。 */
            private class BrokerStatsCallback extends WindowStatsCallback {
                private final boolean incoming;

                private BrokerStatsCallback(RemotingCommand request, boolean incoming) {
                    super(request);
                    this.incoming = incoming;
                }

                @Override
                protected void recordWindow(String window, long count, float rate) {
                    RocketMQCollect.this.setData(this.incoming
                        ? MAPPER.brokerMessagesIn(window, count, rate) : MAPPER.brokerMessagesOut(window, count, rate));
                }
            }

            /** 步骤三：采集存储分发和刷盘积压、消息保留时间、磁盘使用率及剩余空间、线程池队列长度。 */
            private class BrokerRuntimeCallback extends CollectCallback {
                private BrokerRuntimeCallback() {
                    super(RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_RUNTIME_INFO, null));
                }

                @Override
                protected void handleResponse(ResponseFuture responseFuture) {
                    Map<?, ?> response = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                    if (response == null || !(response.get("table") instanceof Map<?, ?> table)) {
                        return;
                    }
                    Long messagesIn = nonnegativeLong(table.get("msgPutTotalTodayNow"));
                    if (messagesIn != null) {
                        RocketMQCollect.this.setData(MAPPER.messagesInTotal(messagesIn));
                    }
                    Long bytesIn = nonnegativeLong(table.get("putMessageSizeTotal"));
                    if (bytesIn != null) {
                        RocketMQCollect.this.setData(MAPPER.throughputInTotal(bytesIn));
                    }
                    Long messagesOut = nonnegativeLong(table.get("msgGetTotalTodayNow"));
                    if (messagesOut != null) {
                        RocketMQCollect.this.setData(MAPPER.messagesOutTotal(messagesOut));
                    }
                    Long dispatch = nonnegativeLong(table.get("dispatchBehindBytes"));
                    if (dispatch != null) {
                        RocketMQCollect.this.setData(MAPPER.dispatchBytes(dispatch));
                    }
                    this.collectFlush(table);
                    this.collectDisk(table);
                    Long earliest = nonnegativeLong(table.get("earliestMessageTimeStamp"));
                    long now = System.currentTimeMillis();
                    if (earliest != null && earliest > 0 && earliest <= now) {
                        RocketMQCollect.this.setData(MAPPER.reserveTime(now - earliest));
                    }
                    for (String pool : new String[] {"send", "pull", "litePull", "query", "ack"}) {
                        this.collectThreadPool(table, pool, pool + "ThreadPoolQueueSize");
                    }
                    this.collectThreadPool(table, "endTransaction", "EndTransactionQueueSize");
                }

                private void collectDisk(Map<?, ?> table) {
                    table.forEach((key, value) -> {
                        if (!(key instanceof String name)) {
                            return;
                        }
                        String type;
                        String path = "";
                        if (name.equals("commitLogDiskRatio")) {
                            type = "commitlog_min";
                        } else if (name.startsWith("commitLogDiskRatio_")) {
                            type = "commitlog";
                            path = name.substring("commitLogDiskRatio_".length());
                        } else if (name.equals("consumeQueueDiskRatio")) {
                            type = "consumequeue";
                        } else {
                            return;
                        }
                        Float ratio = nonnegativeFloat(value);
                        if (ratio != null && ratio <= 1) {
                            RocketMQCollect.this.setData(MAPPER.diskUsage(type, path, ratio));
                        }
                    });
                    Object capacity = table.get("commitLogDirCapacity");
                    if (capacity instanceof String text) {
                        Matcher matcher = DISK_CAPACITY.matcher(text.trim());
                        if (matcher.matches()) {
                            Long total = byteCount(matcher.group(1));
                            Long free = byteCount(matcher.group(2));
                            if (total != null && total > 0 && free != null && free <= total) {
                                RocketMQCollect.this.setData(MAPPER.diskFreeBytes(free));
                            }
                        }
                    }
                }

                private void collectFlush(Map<?, ?> table) {
                    Long flush = byteCount(table.get("remainHowManyDataToFlush"));
                    // 启用暂存池时，原生刷盘积压还包括待提交字节；字段缺席表示未启用该分支。
                    Long commit = table.containsKey("remainHowManyDataToCommit")
                        ? byteCount(table.get("remainHowManyDataToCommit")) : Long.valueOf(0L);
                    if (flush != null && commit != null && flush <= Long.MAX_VALUE - commit) {
                        RocketMQCollect.this.setData(MAPPER.flushBytes(flush + commit));
                    }
                }

                private void collectThreadPool(Map<?, ?> table, String pool, String key) {
                    Long size = nonnegativeLong(table.get(key));
                    if (size != null) {
                        RocketMQCollect.this.setData(MAPPER.threadPool(pool, size));
                    }
                }
            }

            private Long byteCount(Object value) {
                if (!(value instanceof String text)) {
                    return null;
                }
                Matcher matcher = BYTE_COUNT.matcher(text.trim());
                if (!matcher.matches()) {
                    return null;
                }
                String unit = matcher.group(2);
                int exponent = "B".equals(unit) ? 0 : "KMGTPE".indexOf(unit.charAt(0)) + 1;
                try {
                    // Broker 已将二进制字节数格式化并舍入，此处只能还原近似值，不能声称字节级精确。
                    return new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(1024).pow(exponent))
                        .setScale(0, RoundingMode.HALF_UP).longValueExact();
                } catch (ArithmeticException e) {
                    return null;
                }
            }
        }

        private Long nonnegativeLong(Object value) {
            if (value == null) {
                return null;
            }
            try {
                long number = Long.parseLong(value.toString());
                return number >= 0 ? number : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Float nonnegativeFloat(Object value) {
            if (value == null) {
                return null;
            }
            try {
                double number = Double.parseDouble(value.toString());
                float result = (float) number;
                return number >= 0 && Float.isFinite(result) ? result : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** 解析 Broker、Topic 和消费组的分钟、小时、日消息统计窗口。 */
        private abstract class WindowStatsCallback extends CollectCallback {
            private WindowStatsCallback(RemotingCommand request) {
                super(request);
            }

            @Override
            protected final void handleResponse(ResponseFuture responseFuture) {
                Map<?, ?> stats = RemotingSerializable.decode(responseFuture.getResponseCommand().getBody(), Map.class);
                this.collectWindow(stats, "statsMinute", "minute");
                this.collectWindow(stats, "statsHour", "hour");
                this.collectWindow(stats, "statsDay", "day");
            }

            private void collectWindow(Map<?, ?> stats, String key, String window) {
                if (stats == null || !(stats.get(key) instanceof Map<?, ?> sample)
                    || !(sample.get("sum") instanceof Number count) || !(sample.get("tps") instanceof Number rate)) {
                    return;
                }
                try {
                    long value = Long.parseLong(count.toString());
                    float tps = rate.floatValue();
                    if (value >= 0 && Float.isFinite(tps) && rate.doubleValue() >= 0) {
                        this.recordWindow(window, value, tps);
                    }
                } catch (NumberFormatException e) {
                    // 小数、溢出或格式无效的消息数不参与采集。
                }
            }

            protected abstract void recordWindow(String window, long count, float rate);
        }

        /** 统一处理请求发送、响应校验、异常和完成计数，具体响应由各采集回调解析。 */
        private abstract class CollectCallback implements InvokeCallback {
            private final RemotingCommand request;
            private final AtomicBoolean finished = new AtomicBoolean();

            private CollectCallback(RemotingCommand request) {
                this.request = request;
                // 请求发送前 +1；子请求先计数，父请求处理结束后才 -1。
                CollectRound.this.pending.incrementAndGet();
            }

            // 发送异常也进入失败回调，每个请求只扣减一次。
            protected final void execute() {
                executeSafely(this,
                    () -> client.invokeAsync(this.request, this.timeoutMillis(), this));
            }

            private long timeoutMillis() throws TimeoutException {
                long remaining = CollectRound.this.deadline - System.currentTimeMillis();
                if (CollectRound.this.closed || remaining <= 0) {
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
                    if (CollectRound.this.closed) {
                        return;
                    }
                    RemotingCommand response = responseFuture.getResponseCommand();
                    if (response == null) {
                        throw new IllegalStateException("Missing RocketMQ response");
                    }
                    if (response.getCode() != ResponseCode.SUCCESS || response.getBody() == null || response.getBody().length == 0) {
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
                if (CollectRound.this.pending.decrementAndGet() == 0) {
                    CollectRound.this.completed.countDown();
                }
            }

            protected abstract void handleResponse(ResponseFuture responseFuture) throws Exception;
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
