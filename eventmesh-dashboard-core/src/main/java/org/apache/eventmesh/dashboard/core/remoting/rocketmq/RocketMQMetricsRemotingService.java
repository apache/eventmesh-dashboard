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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import static java.util.Locale.ROOT;

import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.client.GetClientsResult;
import org.apache.eventmesh.dashboard.common.model.remoting.metrics.MetricFamily;
import org.apache.eventmesh.dashboard.common.model.remoting.metrics.MetricSample;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.service.remoting.MetricsRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.BrokerStatsData;
import org.apache.rocketmq.remoting.protocol.body.BrokerStatsItem;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.body.ProducerTableInfo;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetTopicStatsInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ViewBrokerStatsDataRequestHeader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/** Read-only Broker-local collection using the framework's registered ADMIN transport. */
public class RocketMQMetricsRemotingService extends AbstractRocketMQRemotingService implements MetricsRemotingService {

    @Override
    public Map<String, CompletableFuture<List<MetricSample>>> collectAsync(MetricFamily family, long deadline) {
        switch (family) {
            case TOPIC_OFFSET:
                return this.collectTopicOffset(deadline);
            case CONSUMER_OFFSET:
                return this.collectConsumerOffset(deadline);
            case TOPIC_STATS:
                return this.collectBrokerStatsTopic(deadline);
            case BROKER_STATS:
                return this.collectBrokerStats(deadline);
            case BROKER_RUNTIME_STATS:
                return this.collectBrokerRuntimeStats(deadline);
            default:
                throw new IllegalArgumentException("Unknown metric family: " + family);
        }
    }

    private Map<String, CompletableFuture<List<MetricSample>>> collectTopicOffset(long deadline) {
        Map<String, CompletableFuture<List<MetricSample>>> families = new LinkedHashMap<>();
        CompletableFuture<List<String>> topics = query(RequestCode.GET_ALL_TOPIC_CONFIG, null, deadline)
            .thenApplyAsync(reply -> new ArrayList<>(decode(reply, TopicConfigSerializeWrapper.class).getTopicConfigTable().keySet()),
                RocketMQAsync.PROCESSING);
        families.put("topics", topics.thenApply(list -> List.of(sample("topic_count", "", "", "", "", list.size()))));
        families.put("topic_offsets", topics.thenComposeAsync(list -> sequence(list, topic -> {
            GetTopicStatsInfoRequestHeader header = new GetTopicStatsInfoRequestHeader();
            header.setTopic(topic);
            return query(RequestCode.GET_TOPIC_STATS_INFO, header, deadline).thenApplyAsync(reply -> {
                List<MetricSample> samples = new ArrayList<>();
                decode(reply, TopicStatsTable.class).getOffsetTable().forEach((queue, offset) -> {
                    String index = String.valueOf(queue.getQueueId());
                    samples.add(sample("queue_min_offset", topic, "", index, "", offset.getMinOffset()));
                    samples.add(sample("queue_max_offset", topic, "", index, "", offset.getMaxOffset()));
                    samples.add(sample("queue_last_update_ms", topic, "", index, "", offset.getLastUpdateTimestamp()));
                });
                long minimum = samples.stream().filter(sample -> "queue_min_offset".equals(sample.getMetric()))
                    .mapToLong(sample -> sample.getValue().longValue()).sum();
                long maximum = samples.stream().filter(sample -> "queue_max_offset".equals(sample.getMetric()))
                    .mapToLong(sample -> sample.getValue().longValue()).sum();
                long lastUpdate = samples.stream().filter(sample -> "queue_last_update_ms".equals(sample.getMetric()))
                    .mapToLong(sample -> sample.getValue().longValue()).max().orElse(0);
                samples.add(sample("topic_min_offset_sum", topic, "", "", "", minimum));
                samples.add(sample("topic_max_offset_sum", topic, "", "", "", maximum));
                samples.add(sample("topic_last_update_ms", topic, "", "", "", lastUpdate));
                return samples;
            }, RocketMQAsync.PROCESSING);
        }), RocketMQAsync.PROCESSING));
        families.computeIfPresent("topic_offsets", (key, future) -> future.thenApply(samples -> {
            long minimum = samples.stream().filter(sample -> "topic_min_offset_sum".equals(sample.getMetric()))
                .mapToLong(sample -> sample.getValue().longValue()).sum();
            long maximum = samples.stream().filter(sample -> "topic_max_offset_sum".equals(sample.getMetric()))
                .mapToLong(sample -> sample.getValue().longValue()).sum();
            samples.add(sample("broker_topic_min_offset_sum", "", "", "", "", minimum));
            samples.add(sample("broker_topic_max_offset_sum", "", "", "", "", maximum));
            return samples;
        }));
        return families;
    }

    private Map<String, CompletableFuture<List<MetricSample>>> collectConsumerOffset(long deadline) {
        Map<String, CompletableFuture<List<MetricSample>>> families = new LinkedHashMap<>();
        CompletableFuture<List<String>> groups = query(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null, deadline)
            .thenApplyAsync(reply -> new ArrayList<>(decode(reply, SubscriptionGroupWrapper.class).getSubscriptionGroupTable().keySet()),
                RocketMQAsync.PROCESSING);
        families.put("groups", groups.thenApply(list -> List.of(sample("group_count", "", "", "", "", list.size()))));
        families.put("consumer_offsets", groups.thenComposeAsync(list -> sequence(list, group -> {
            GetConsumeStatsRequestHeader header = new GetConsumeStatsRequestHeader();
            header.setConsumerGroup(group);
            return query(RequestCode.GET_CONSUME_STATS, header, deadline).thenComposeAsync(reply -> {
                if (reply.getCode() == ResponseCode.CONSUMER_NOT_ONLINE) {
                    return CompletableFuture.completedFuture(List.of());
                }
                ConsumeStats body = decode(reply, ConsumeStats.class);
                List<MetricSample> samples = new ArrayList<>();
                body.getOffsetTable().forEach((queue, offset) -> {
                    String topic = queue.getTopic();
                    String index = String.valueOf(queue.getQueueId());
                    samples.add(sample("consumer_offset", topic, group, index, "", offset.getConsumerOffset()));
                    samples.add(sample("broker_offset", topic, group, index, "", offset.getBrokerOffset()));
                    long difference = offset.getBrokerOffset() - offset.getConsumerOffset();
                    samples.add(sample("offset_lag", topic, group, index, "", Math.max(difference, 0)));
                    samples.add(sample("offset_negative", topic, group, index, "", difference < 0 ? 1 : 0));
                });
                return CompletableFuture.completedFuture(samples);
            }, RocketMQAsync.PROCESSING);
        }), RocketMQAsync.PROCESSING));
        families.put("connections", this.getClientListAsync(deadline).thenApply(result -> {
            if (result.getCode() != 200) {
                throw new IllegalStateException("Client query failed: " + result.getCode());
            }
            return List.of(sample("connection_count", "", "", "", "", result.getData().size()));
        }));
        return families;
    }

    private Map<String, CompletableFuture<List<MetricSample>>> collectBrokerStatsTopic(long deadline) {
        Map<String, CompletableFuture<List<MetricSample>>> families = new LinkedHashMap<>();
        CompletableFuture<List<String>> topics = query(RequestCode.GET_ALL_TOPIC_CONFIG, null, deadline)
            .thenApplyAsync(reply -> new ArrayList<>(decode(reply, TopicConfigSerializeWrapper.class).getTopicConfigTable().keySet()),
                RocketMQAsync.PROCESSING);
        families.put("topic_rates", topics.thenComposeAsync(list -> sequence(list, topic ->
            sequence(List.of("TOPIC_PUT_NUMS", "TOPIC_PUT_SIZE"), name -> stats(name, topic, topic, "", deadline))),
            RocketMQAsync.PROCESSING));
        CompletableFuture<List<String>> groups = query(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null, deadline)
            .thenApplyAsync(reply -> new ArrayList<>(decode(reply, SubscriptionGroupWrapper.class).getSubscriptionGroupTable().keySet()),
                RocketMQAsync.PROCESSING);
        families.put("group_rates", groups.thenComposeAsync(list -> sequence(list, group -> {
            GetConsumeStatsRequestHeader header = new GetConsumeStatsRequestHeader();
            header.setConsumerGroup(group);
            return query(RequestCode.GET_CONSUME_STATS, header, deadline).thenComposeAsync(reply -> {
                ConsumeStats body = decode(reply, ConsumeStats.class);
                Set<String> consumedTopics = new TreeSet<>();
                body.getOffsetTable().keySet().forEach(queue -> consumedTopics.add(queue.getTopic()));
                return sequence(new ArrayList<>(consumedTopics), topic -> sequence(
                    List.of("GROUP_GET_NUMS", "GROUP_GET_SIZE", "SNDBCK_PUT_NUMS"),
                    name -> stats(name, topic + "@" + group, topic, group, deadline)));
            }, RocketMQAsync.PROCESSING);
        }), RocketMQAsync.PROCESSING));
        return families;
    }

    private Map<String, CompletableFuture<List<MetricSample>>> collectBrokerStats(long deadline) {
        return Map.of("broker_rates", query(RequestCode.GET_BROKER_CONFIG, null, deadline).thenComposeAsync(reply -> {
            requireSuccess(reply);
            if (reply.getBody() == null) {
                throw new IllegalStateException("Missing Broker configuration");
            }
            Properties config = new Properties();
            try {
                config.load(new StringReader(new String(reply.getBody(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new IllegalStateException("Malformed Broker configuration", e);
            }
            String cluster = config.getProperty("brokerClusterName");
            if (StringUtils.isBlank(cluster)) {
                throw new IllegalStateException("Missing Broker cluster name");
            }
            return sequence(List.of("BROKER_PUT_NUMS", "BROKER_GET_NUMS"), name -> stats(name, cluster, "", "", deadline));
        }, RocketMQAsync.PROCESSING));
    }

    private Map<String, CompletableFuture<List<MetricSample>>> collectBrokerRuntimeStats(long deadline) {
        Map<String, CompletableFuture<List<MetricSample>>> families = new LinkedHashMap<>();
        families.put("runtime", query(RequestCode.GET_BROKER_RUNTIME_INFO, null, deadline).thenApplyAsync(reply -> {
            Map<String, String> values = decode(reply, KVTable.class).getTable();
            List<MetricSample> samples = new ArrayList<>();
            samples.add(sample("broker_reachable", "", "", "", "", 1));
            runtime(samples, values, "dispatchBehindBytes", "dispatch_behind_bytes");
            if (values.containsKey("remainHowManyDataToFlush")) {
                samples.add(sample("flush_behind_bytes", "", "", "", "", bytes(values.get("remainHowManyDataToFlush"))));
            }
            if (values.containsKey("putMessageSizeTotal") && values.containsKey("bootTimestamp")) {
                samples.add(sample("broker_stored_bytes_total", "", "", "", values.get("bootTimestamp"),
                    Long.parseLong(values.get("putMessageSizeTotal"))));
            }
            // Snapshot triplets are Broker's 10/60/600 second TPS, never cumulative totals.
            for (String key : List.of("putTps", "getTransferredTps")) {
                if (values.containsKey(key)) {
                    String[] rates = values.get(key).trim().split("\\s+");
                    String[] windows = {"10s", "60s", "600s"};
                    if (rates.length != windows.length) {
                        throw new IllegalStateException("Invalid Broker TPS: " + key);
                    }
                    for (int i = 0; i < rates.length; i++) {
                        samples.add(sample(key, "", "", "", windows[i], Double.parseDouble(rates[i])));
                    }
                }
            }
            return samples;
        }, RocketMQAsync.PROCESSING));
        return families;
    }

    private CompletableFuture<RemotingCommand> query(int code, CommandCustomHeader header, long deadline) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
            return CompletableFuture.failedFuture(new TimeoutException("Collection deadline exceeded"));
        }
        return RocketMQAsync.invoke(this.getClient(), RemotingCommand.createRequestCommand(code, header), Math.min(3000, remaining));
    }

    private CompletableFuture<List<MetricSample>> stats(String name, String key, String topic, String group, long deadline) {
        ViewBrokerStatsDataRequestHeader header = new ViewBrokerStatsDataRequestHeader();
        header.setStatsName(name);
        header.setStatsKey(key);
        return query(RequestCode.VIEW_BROKER_STATS_DATA, header, deadline).thenApplyAsync(reply -> {
            // Broker explicitly reports an absent stats item; do not manufacture a zero rate.
            if (reply.getCode() == ResponseCode.SYSTEM_ERROR
                && ("The stats <" + name + "> <" + key + "> not exist").equals(reply.getRemark())) {
                return List.of();
            }
            BrokerStatsData data = decode(reply, BrokerStatsData.class);
            List<MetricSample> samples = new ArrayList<>();
            rate(samples, name, topic, group, "minute", data.getStatsMinute());
            rate(samples, name, topic, group, "hour", data.getStatsHour());
            rate(samples, name, topic, group, "day", data.getStatsDay());
            return samples;
        }, RocketMQAsync.PROCESSING);
    }

    private void rate(List<MetricSample> samples, String name, String topic, String group, String window, BrokerStatsItem item) {
        if (item != null) {
            samples.add(sample(name.toLowerCase(ROOT) + "_tps", topic, group, "", window, item.getTps()));
        }
    }

    private <T> CompletableFuture<List<MetricSample>> sequence(List<T> items, Function<T, CompletableFuture<List<MetricSample>>> operation) {
        CompletableFuture<List<MetricSample>> chain = CompletableFuture.completedFuture(new ArrayList<>());
        for (T item : items) {
            chain = chain.thenComposeAsync(samples -> operation.apply(item).thenApply(next -> {
                samples.addAll(next);
                return samples;
            }), RocketMQAsync.PROCESSING);
        }
        return chain;
    }

    private <T> T decode(RemotingCommand reply, Class<T> type) {
        if (reply.getCode() != ResponseCode.SUCCESS) {
            throw new IllegalStateException("RocketMQ response " + reply.getCode() + ": " + reply.getRemark());
        }
        if (reply.getBody() == null || reply.getBody().length == 0) {
            throw new IllegalStateException("Missing " + type.getSimpleName() + " body");
        }
        T body = RemotingSerializable.decode(reply.getBody(), type);
        if (body == null) {
            throw new IllegalStateException("Invalid " + type.getSimpleName() + " body");
        }
        return body;
    }

    private void runtime(List<MetricSample> samples, Map<String, String> values, String key, String metric) {
        if (values.containsKey(key)) {
            samples.add(sample(metric, "", "", "", "", Double.parseDouble(values.get(key))));
        }
    }

    static double bytes(String text) {
        String[] parts = text.trim().split("\\s+");
        double value = Double.parseDouble(parts[0]);
        if (parts.length == 1 || "B".equals(parts[1])) {
            return value;
        }
        String unit = parts[1];
        int exponent = List.of("KiB", "MiB", "GiB", "TiB", "PiB", "EiB").indexOf(unit) + 1;
        if (exponent == 0) {
            throw new IllegalArgumentException("Unknown byte unit: " + text);
        }
        return value * Math.pow(1024, exponent);
    }

    private MetricSample sample(String name, String topic, String group, String queue, String window, Number value) {
        if (!Double.isFinite(value.doubleValue())) {
            throw new IllegalStateException("Invalid metric: " + name);
        }
        return new MetricSample(name, topic, group, queue, window, value);
    }

    @Override
    public CompletableFuture<GetClientsResult> getClientListAsync() {
        return this.getClientListAsync(System.currentTimeMillis() + 4000);
    }

    public CompletableFuture<GetClientsResult> getClientListAsync(long deadline) {
        CompletableFuture<Map<String, ClientMetadata>> producers =
            RocketMQAsync.invoke(this.getClient(), RemotingCommand.createRequestCommand(RequestCode.GET_ALL_PRODUCER_INFO, null), 3000)
                .thenApplyAsync(response -> {
                    this.requireSuccess(response);
                    ProducerTableInfo table = this.decodeClientBody(response, ProducerTableInfo.class, "data");
                    Map<String, ClientMetadata> clients = new TreeMap<>();
                    table.getData().values().forEach(group -> group.forEach(producer ->
                        this.addClient(clients, producer.getClientId(), producer.getRemoteIP(), producer.getLanguage())));
                    return clients;
                }, RocketMQAsync.PROCESSING);
        CompletableFuture<Map<String, ClientMetadata>> consumers =
            RocketMQAsync.invoke(this.getClient(), RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null), 3000)
                .thenComposeAsync(response -> {
                    this.requireSuccess(response);
                    SubscriptionGroupWrapper groups = this.decodeClientBody(response, SubscriptionGroupWrapper.class,
                        "subscriptionGroupTable");
                    CompletableFuture<Map<String, ClientMetadata>> chain =
                        CompletableFuture.completedFuture(new TreeMap<>());
                    for (String group : new TreeSet<>(groups.getSubscriptionGroupTable().keySet())) {
                        chain = chain.thenComposeAsync(clients -> {
                            long remaining = deadline - System.currentTimeMillis();
                            if (remaining <= 0) {
                                return CompletableFuture.failedFuture(new TimeoutException());
                            }
                            GetConsumerConnectionListRequestHeader header = new GetConsumerConnectionListRequestHeader();
                            header.setConsumerGroup(group);
                            return RocketMQAsync.invoke(this.getClient(),
                                RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST, header),
                                Math.min(3000, remaining)).thenApplyAsync(reply -> {
                                    if (reply.getCode() == ResponseCode.CONSUMER_NOT_ONLINE) {
                                        return clients;
                                    }
                                    this.requireSuccess(reply);
                                    ConsumerConnection body = this.decodeClientBody(reply, ConsumerConnection.class, "connectionSet");
                                    body.getConnectionSet().forEach(connection -> this.addClient(clients,
                                        connection.getClientId(), connection.getClientAddr(), connection.getLanguage()));
                                    return clients;
                                }, RocketMQAsync.PROCESSING);
                        }, RocketMQAsync.PROCESSING);
                    }
                    return chain;
                }, RocketMQAsync.PROCESSING);
        return producers.thenCombineAsync(consumers, (first, second) -> {
            second.values().forEach(client -> this.addClient(first, client.getName(), client.getHost() + ":" + client.getPort(),
                LanguageCode.valueOf(client.getLanguage())));
            GetClientsResult result = new GetClientsResult();
            result.setCode(200);
            result.setData(new ArrayList<>(first.values()));
            return result;
        }, RocketMQAsync.PROCESSING);
    }

    private void requireSuccess(RemotingCommand response) {
        if (response.getCode() != ResponseCode.SUCCESS) {
            throw new IllegalStateException("RocketMQ response " + response.getCode() + ": " + response.getRemark());
        }
    }

    private void addClient(Map<String, ClientMetadata> clients, String name, String address, LanguageCode language) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(address) || language == null) {
            throw new IllegalStateException("RocketMQ returned incomplete client identity");
        }
        String endpoint = address.startsWith("/") ? address.substring(1) : address;
        int separator = endpoint.lastIndexOf(':');
        if (separator <= 0 || separator == endpoint.length() - 1) {
            throw new IllegalStateException("RocketMQ returned an invalid client address");
        }
        String host = endpoint.substring(0, separator);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int port;
        try {
            port = Integer.parseInt(endpoint.substring(separator + 1));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("RocketMQ returned an invalid client port", e);
        }
        if (StringUtils.isBlank(host) || port < 1 || port > 65535) {
            throw new IllegalStateException("RocketMQ returned an invalid client endpoint");
        }
        ClientMetadata client = new ClientMetadata();
        client.setName(name);
        client.setHost(host);
        client.setPort(port);
        client.setLanguage(language.name());
        client.setProtocol("RocketMQ");
        ClientMetadata previous = clients.putIfAbsent(client.nodeUnique(), client);
        if (previous != null && (!name.equals(previous.getName()) || !client.getLanguage().equals(previous.getLanguage()))) {
            throw new IllegalStateException("RocketMQ returned conflicting client identities for one connection");
        }
    }

    private <T> T decodeClientBody(RemotingCommand response, Class<T> type, String field) {
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no client query body");
        }
        JSONObject json = JSON.parseObject(response.getBody(), JSONObject.class);
        if (json == null || !json.containsKey(field)) {
            throw new IllegalStateException("RocketMQ returned an invalid client query body");
        }
        T body = RemotingSerializable.decode(response.getBody(), type);
        if (body == null) {
            throw new IllegalStateException("RocketMQ returned an invalid client query body");
        }
        return body;
    }

    @Override
    public CompletableFuture<GetOffsetResult> getOffsetAsync(GetOffsetRequest request) {
        if (request == null || StringUtils.isBlank(request.getGroupName())
            || request.getTopic() != null && StringUtils.isBlank(request.getTopic())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Consumer group is required"));
        }
        GetConsumeStatsRequestHeader header = new GetConsumeStatsRequestHeader();
        header.setConsumerGroup(request.getGroupName());
        header.setTopic(request.getTopic());
        return RocketMQAsync.invoke(this.getClient(), RemotingCommand.createRequestCommand(RequestCode.GET_CONSUME_STATS, header), 3000)
            .thenApplyAsync(this::decodeOffsets, RocketMQAsync.PROCESSING);
    }

    private GetOffsetResult decodeOffsets(RemotingCommand response) {
        GetOffsetResult result = this.buildResult(response, new GetOffsetResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        ConsumeStats stats = this.decode(response, ConsumeStats.class);
        if (stats.getOffsetTable() == null) {
            throw new IllegalStateException("RocketMQ returned no consumption offset table");
        }
        List<GetOffsetResponse> offsets = new ArrayList<>();
        stats.getOffsetTable().forEach((queue, value) -> {
            if (queue == null || value == null) {
                throw new IllegalStateException("RocketMQ returned an invalid consumption offset entry");
            }
            GetOffsetResponse offset = new GetOffsetResponse();
            offset.setTopic(queue.getTopic());
            offset.setBrokerName(queue.getBrokerName());
            offset.setPartitionId(queue.getQueueId());
            offset.setOffset(value.getConsumerOffset());
            offset.setBrokerOffset(value.getBrokerOffset());
            offset.setLastTimestamp(value.getLastTimestamp());
            offsets.add(offset);
        });
        offsets.sort(Comparator.comparing(GetOffsetResponse::getTopic).thenComparing(GetOffsetResponse::getBrokerName)
            .thenComparing(GetOffsetResponse::getPartitionId));
        result.setData(offsets);
        return result;
    }

}
