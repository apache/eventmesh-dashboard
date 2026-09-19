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

package org.apache.eventmesh.dashboard.console.mapstruct.report;

import org.apache.eventmesh.dashboard.common.model.remoting.metrics.MetricSample;
import org.apache.eventmesh.dashboard.console.function.report.collect.CollectContext;
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

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/** Generated updates populate only the observed metric and its resource dimensions. */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RocketMQMetricMapper {
    RocketMQMetricMapper INSTANCE = Mappers.getMapper(RocketMQMetricMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "value", source = "sample.value", qualifiedByName = "toLong")
    void updateQueueMaxOffset(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueMinOffset", source = "sample.value", qualifiedByName = "toLong")
    void updateQueueMinOffset(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueLastUpdateTime", source = "sample.value", qualifiedByName = "toLong")
    void updateQueueLastUpdateMs(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "familyId", source = "family")
    @Mapping(target = "value", source = "sample.value", qualifiedByName = "toLong")
    void updateTopicCount(MetricSample sample, String family, @MappingTarget RocketmqTopicNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "sample.value", qualifiedByName = "toLong")
    void updateGroupCount(MetricSample sample, String family, @MappingTarget RocketmqConsumerGroupNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "familyId", source = "family")
    @Mapping(target = "valueConnectionCount", source = "sample.value", qualifiedByName = "toLong")
    void updateConnectionCount(MetricSample sample, String family, @MappingTarget RocketmqTopicNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueBrokerMinOffsetSum", source = "sample.value", qualifiedByName = "toLong")
    void updateBrokerTopicMinOffsetSum(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueBrokerMaxOffsetSum", source = "sample.value", qualifiedByName = "toLong")
    void updateBrokerTopicMaxOffsetSum(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "sample.value", qualifiedByName = "toLong")
    void updateFlushBehindBytes(MetricSample sample, String family, @MappingTarget RocketmqStorageFlushBehindBytes target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "sample.value", qualifiedByName = "toLong")
    void updateDispatchBehindBytes(MetricSample sample, String family, @MappingTarget RocketmqStorageDispatchBehindBytes target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "valueReachable", source = "sample.value", qualifiedByName = "toLong")
    void updateBrokerReachable(MetricSample sample, String family, @MappingTarget RocketmqStorageFlushBehindBytes target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "valueStoredBytes", source = "sample.value", qualifiedByName = "toLong")
    @Mapping(target = "valueBootTimestamp", source = "sample.window", qualifiedByName = "bootTimestamp")
    void updateBrokerStoredBytesTotal(MetricSample sample, String family, @MappingTarget RocketmqStorageFlushBehindBytes target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueMinOffsetSum", source = "sample.value", qualifiedByName = "toLong")
    void updateTopicMinOffsetSum(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueMaxOffsetSum", source = "sample.value", qualifiedByName = "toLong")
    void updateTopicMaxOffsetSum(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "valueTopicLastUpdateTime", source = "sample.value", qualifiedByName = "toLong")
    void updateTopicLastUpdateMs(MetricSample sample, String family, @MappingTarget Rocketmq2ProducerOffset target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueConsumerOffset", source = "sample.value", qualifiedByName = "toLong")
    void updateConsumerOffset(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueBrokerOffset", source = "sample.value", qualifiedByName = "toLong")
    void updateBrokerOffset(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueOffsetLag", source = "sample.value", qualifiedByName = "toLong")
    void updateOffsetLag(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueOffsetNegative", source = "sample.value", qualifiedByName = "toLong")
    void updateOffsetNegative(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateTopicPutNumsTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesInTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueBytesRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateTopicPutSizeTps(MetricSample sample, String family, @MappingTarget RocketmqThroughputInTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateGroupGetNumsTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueBytesRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateGroupGetSizeTps(MetricSample sample, String family, @MappingTarget RocketmqThroughputOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueSendBackRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateSndbckPutNumsTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueBrokerRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateBrokerPutNumsTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesInTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueBrokerRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateBrokerGetNumsTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueRuntimeRate", source = "sample.value", qualifiedByName = "toFloat")
    void updatePutTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesInTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicKeyId", source = "sample.topic")
    @Mapping(target = "groupKeyId", source = "sample.group")
    @Mapping(target = "queueKeyId", source = "sample.queue")
    @Mapping(target = "windowId", source = "sample.window")
    @Mapping(target = "valueRuntimeRate", source = "sample.value", qualifiedByName = "toFloat")
    void updateGetTransferredTps(MetricSample sample, String family, @MappingTarget RocketmqMessagesOutTotal target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "familyId", source = "family")
    @Mapping(target = "valueDurationMs", source = "sample.value", qualifiedByName = "toFloat")
    void updateCollectionDurationMs(MetricSample sample, String family, @MappingTarget RocketmqTopicNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "familyId", source = "family")
    @Mapping(target = "valueFailures", source = "sample.value", qualifiedByName = "toLong")
    void updateCollectionFailures(MetricSample sample, String family, @MappingTarget RocketmqTopicNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "familyId", source = "family")
    @Mapping(target = "valueLastSuccessTime", source = "sample.value", qualifiedByName = "toLong")
    void updateCollectionLastSuccessMs(MetricSample sample, String family, @MappingTarget RocketmqTopicNumber target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "organizationId", source = "organization")
    @Mapping(target = "clustersId", source = "cluster")
    @Mapping(target = "clustersName", source = "clusterName")
    @Mapping(target = "runtimeId", source = "runtime")
    @Mapping(target = "runtimeName", source = "runtimeName")
    @Mapping(target = "time", source = "context.sampleTime")
    void fillMetadata(CollectContext context, Long organization, Long cluster, String clusterName,
        Long runtime, String runtimeName, @MappingTarget RuntimeId target);

    @Named("toLong")
    default Long toLong(Number value) {
        return value == null ? null : value.longValue();
    }

    @Named("toFloat")
    default Float toFloat(Number value) {
        return value == null ? null : value.floatValue();
    }

    @Named("bootTimestamp")
    default Long bootTimestamp(String value) {
        return value == null || value.isEmpty() ? null : Long.valueOf(value);
    }
}
