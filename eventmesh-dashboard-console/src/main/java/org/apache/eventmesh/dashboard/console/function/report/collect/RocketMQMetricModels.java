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

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.model.remoting.metrics.MetricSample;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerGroupNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesInTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesOutTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDispatchBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageFlushBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqThroughputInTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqThroughputOutTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqTopicNumber;
import org.apache.eventmesh.dashboard.console.mapstruct.report.RocketMQMetricMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Combine observations with the same resource dimensions into annotated report rows. */
public final class RocketMQMetricModels {
    private RocketMQMetricModels() {
    }

    /** Typed generated update for one metric observation. */
    @FunctionalInterface
    public interface Update<T extends RuntimeId> {
        void apply(MetricSample sample, String family, T target);
    }

    public enum Dimension {
        TOPIC, GROUP, QUEUE, WINDOW, FAMILY
    }

    public record Binding(Supplier<? extends RuntimeId> factory, String fieldName,
                          Update<RuntimeId> update, List<Dimension> dimensions) {
    }

    private static <T extends RuntimeId> Binding bind(Supplier<T> factory, String fieldName,
        Update<T> update, Dimension... dimensions) {
        Class<?> model = factory.get().getClass();
        return new Binding(factory, fieldName, (sample, family, target) -> {
            // Every row is created by this binding's factory and keyed by the same model class.
            @SuppressWarnings("unchecked")
            T typed = (T) model.cast(target);
            update.apply(sample, family, typed);
        }, List.of(dimensions));
    }

    private static final Map<String, Binding> BINDINGS = Map.ofEntries(
        Map.entry("queue_max_offset", bind(Rocketmq2ProducerOffset::new, "value",
            RocketMQMetricMapper.INSTANCE::updateQueueMaxOffset, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("queue_min_offset", bind(Rocketmq2ProducerOffset::new, "valueMinOffset",
            RocketMQMetricMapper.INSTANCE::updateQueueMinOffset, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("queue_last_update_ms", bind(Rocketmq2ProducerOffset::new, "valueLastUpdateTime",
            RocketMQMetricMapper.INSTANCE::updateQueueLastUpdateMs, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("topic_count", bind(RocketmqTopicNumber::new, "value",
            RocketMQMetricMapper.INSTANCE::updateTopicCount, Dimension.FAMILY)),
        Map.entry("group_count", bind(RocketmqConsumerGroupNumber::new, "value",
            RocketMQMetricMapper.INSTANCE::updateGroupCount)),
        Map.entry("connection_count", bind(RocketmqTopicNumber::new, "valueConnectionCount",
            RocketMQMetricMapper.INSTANCE::updateConnectionCount, Dimension.FAMILY)),
        Map.entry("broker_topic_min_offset_sum", bind(Rocketmq2ProducerOffset::new, "valueBrokerMinOffsetSum",
            RocketMQMetricMapper.INSTANCE::updateBrokerTopicMinOffsetSum, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("broker_topic_max_offset_sum", bind(Rocketmq2ProducerOffset::new, "valueBrokerMaxOffsetSum",
            RocketMQMetricMapper.INSTANCE::updateBrokerTopicMaxOffsetSum, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("flush_behind_bytes", bind(RocketmqStorageFlushBehindBytes::new, "value",
            RocketMQMetricMapper.INSTANCE::updateFlushBehindBytes)),
        Map.entry("dispatch_behind_bytes", bind(RocketmqStorageDispatchBehindBytes::new, "value",
            RocketMQMetricMapper.INSTANCE::updateDispatchBehindBytes)),
        Map.entry("broker_reachable", bind(RocketmqStorageFlushBehindBytes::new, "valueReachable",
            RocketMQMetricMapper.INSTANCE::updateBrokerReachable)),
        Map.entry("broker_stored_bytes_total", bind(RocketmqStorageFlushBehindBytes::new, "valueStoredBytes",
            RocketMQMetricMapper.INSTANCE::updateBrokerStoredBytesTotal)),
        Map.entry("topic_min_offset_sum", bind(Rocketmq2ProducerOffset::new, "valueMinOffsetSum",
            RocketMQMetricMapper.INSTANCE::updateTopicMinOffsetSum, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("topic_max_offset_sum", bind(Rocketmq2ProducerOffset::new, "valueMaxOffsetSum",
            RocketMQMetricMapper.INSTANCE::updateTopicMaxOffsetSum, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("topic_last_update_ms", bind(Rocketmq2ProducerOffset::new, "valueTopicLastUpdateTime",
            RocketMQMetricMapper.INSTANCE::updateTopicLastUpdateMs, Dimension.TOPIC, Dimension.QUEUE)),
        Map.entry("consumer_offset", bind(RocketmqMessagesOutTotal::new, "valueConsumerOffset",
            RocketMQMetricMapper.INSTANCE::updateConsumerOffset, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("broker_offset", bind(RocketmqMessagesOutTotal::new, "valueBrokerOffset",
            RocketMQMetricMapper.INSTANCE::updateBrokerOffset, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("offset_lag", bind(RocketmqMessagesOutTotal::new, "valueOffsetLag",
            RocketMQMetricMapper.INSTANCE::updateOffsetLag, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("offset_negative", bind(RocketmqMessagesOutTotal::new, "valueOffsetNegative",
            RocketMQMetricMapper.INSTANCE::updateOffsetNegative, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("topic_put_nums_tps", bind(RocketmqMessagesInTotal::new, "valueRate",
            RocketMQMetricMapper.INSTANCE::updateTopicPutNumsTps, Dimension.TOPIC, Dimension.WINDOW)),
        Map.entry("topic_put_size_tps", bind(RocketmqThroughputInTotal::new, "valueBytesRate",
            RocketMQMetricMapper.INSTANCE::updateTopicPutSizeTps, Dimension.TOPIC, Dimension.WINDOW)),
        Map.entry("group_get_nums_tps", bind(RocketmqMessagesOutTotal::new, "valueRate",
            RocketMQMetricMapper.INSTANCE::updateGroupGetNumsTps, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("group_get_size_tps", bind(RocketmqThroughputOutTotal::new, "valueBytesRate",
            RocketMQMetricMapper.INSTANCE::updateGroupGetSizeTps, Dimension.TOPIC, Dimension.GROUP, Dimension.WINDOW)),
        Map.entry("sndbck_put_nums_tps", bind(RocketmqMessagesOutTotal::new, "valueSendBackRate",
            RocketMQMetricMapper.INSTANCE::updateSndbckPutNumsTps, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("broker_put_nums_tps", bind(RocketmqMessagesInTotal::new, "valueBrokerRate",
            RocketMQMetricMapper.INSTANCE::updateBrokerPutNumsTps, Dimension.TOPIC, Dimension.WINDOW)),
        Map.entry("broker_get_nums_tps", bind(RocketmqMessagesOutTotal::new, "valueBrokerRate",
            RocketMQMetricMapper.INSTANCE::updateBrokerGetNumsTps, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("putTps", bind(RocketmqMessagesInTotal::new, "valueRuntimeRate",
            RocketMQMetricMapper.INSTANCE::updatePutTps, Dimension.TOPIC, Dimension.WINDOW)),
        Map.entry("getTransferredTps", bind(RocketmqMessagesOutTotal::new, "valueRuntimeRate",
            RocketMQMetricMapper.INSTANCE::updateGetTransferredTps, Dimension.TOPIC, Dimension.GROUP, Dimension.QUEUE, Dimension.WINDOW)),
        Map.entry("collection_duration_ms", bind(RocketmqTopicNumber::new, "valueDurationMs",
            RocketMQMetricMapper.INSTANCE::updateCollectionDurationMs, Dimension.FAMILY)),
        Map.entry("collection_failures", bind(RocketmqTopicNumber::new, "valueFailures",
            RocketMQMetricMapper.INSTANCE::updateCollectionFailures, Dimension.FAMILY)),
        Map.entry("collection_last_success_ms", bind(RocketmqTopicNumber::new, "valueLastSuccessTime",
            RocketMQMetricMapper.INSTANCE::updateCollectionLastSuccessMs, Dimension.FAMILY)));

    public static Map<String, Binding> bindings() {
        return BINDINGS;
    }

    public static List<Supplier<? extends RuntimeId>> models() {
        Map<Class<?>, Supplier<? extends RuntimeId>> factories = new LinkedHashMap<>();
        BINDINGS.values().forEach(binding -> factories.putIfAbsent(binding.factory().get().getClass(), binding.factory()));
        return new ArrayList<>(factories.values());
    }

    public static List<RuntimeId> toReports(List<MetricSample> samples, String family) {
        Map<List<Object>, RuntimeId> rows = new LinkedHashMap<>();
        for (MetricSample sample : samples) {
            Binding binding = BINDINGS.get(sample.getMetric());
            if (binding == null) {
                throw new IllegalArgumentException("Unsupported RocketMQ metric: " + sample.getMetric());
            }
            List<Object> identity = new ArrayList<>();
            identity.add(binding.factory().get().getClass());
            for (Dimension dimension : binding.dimensions()) {
                String dimensionValue = switch (dimension) {
                    case TOPIC -> sample.getTopic();
                    case GROUP -> sample.getGroup();
                    case QUEUE -> sample.getQueue();
                    case WINDOW -> sample.getWindow();
                    case FAMILY -> family;
                };
                identity.add(dimensionValue);
            }
            RuntimeId row = rows.computeIfAbsent(identity, key -> binding.factory().get());
            binding.update().apply(sample, family, row);
        }
        return new ArrayList<>(rows.values());
    }

    public static String table(Class<?> model) {
        return model.getAnnotation(ReportMeta.class).tableName();
    }
}
