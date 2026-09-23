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

import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqBrokerMessagesIn;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqBrokerMessagesOut;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerConnectionNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerFailedTps;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerGroupNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerLagLatency;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerProcessTime;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerSuccessTps;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqGroupMessagesOut;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesInTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesOutTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDiskFreeBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDiskUsage;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDispatchBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageFlushBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageMessageReserveTime;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqThreadPoolWartermark;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqThroughputInTotal;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqTopicMessagesIn;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqTopicNumber;

import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** 将 Broker 采样转换为语义对应的报表模型，由模型注解指定目标表。 */
@Mapper
public interface RocketMQCollectMapper {
    RocketMQCollectMapper INSTANCE = Mappers.getMapper(RocketMQCollectMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "groupName", source = "group")
    @Mapping(target = "valueConnectionCount", source = "count")
    RocketmqConsumerConnectionNumber connections(String group, long count);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topic")
    @Mapping(target = "queueId", source = "queue")
    @Mapping(target = "value", source = "offset.maxOffset")
    @Mapping(target = "valueMinOffset", source = "offset.minOffset")
    @Mapping(target = "valueLastUpdateTime", source = "offset.lastUpdateTimestamp")
    Rocketmq2ProducerOffset topicOffset(String topic, String queue, TopicOffset offset);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topic")
    @Mapping(target = "queueId", constant = "")
    @Mapping(target = "valueMinOffsetSum", source = "min")
    @Mapping(target = "valueMaxOffsetSum", source = "max")
    @Mapping(target = "valueLastUpdateTime", source = "lastUpdate")
    Rocketmq2ProducerOffset aggregateOffset(String topic, long min, long max, long lastUpdate);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topic")
    @Mapping(target = "groupName", source = "group")
    @Mapping(target = "queueId", source = "queue")
    @Mapping(target = "valueConsumerOffset", source = "offset.consumerOffset")
    @Mapping(target = "valueBrokerOffset", source = "offset.brokerOffset")
    @Mapping(target = "valueOffsetLag", expression = "java(Math.max(0L, offset.getBrokerOffset() - offset.getConsumerOffset()))")
    RocketmqConsumerOffset consumerOffset(String topic, String group, String queue, OffsetWrapper offset);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "window", source = "window")
    @Mapping(target = "valueWindowCount", source = "count")
    @Mapping(target = "value", source = "rate")
    RocketmqBrokerMessagesIn brokerMessagesIn(String window, long count, float rate);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "window", source = "window")
    @Mapping(target = "valueWindowCount", source = "count")
    @Mapping(target = "value", source = "rate")
    RocketmqBrokerMessagesOut brokerMessagesOut(String window, long count, float rate);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "bytes")
    RocketmqStorageDispatchBehindBytes dispatchBytes(Long bytes);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "bytes")
    RocketmqStorageFlushBehindBytes flushBytes(Long bytes);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "age")
    RocketmqStorageMessageReserveTime reserveTime(Long age);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "poolName", source = "pool")
    @Mapping(target = "value", source = "size")
    RocketmqThreadPoolWartermark threadPool(String pool, long size);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "count")
    RocketmqTopicNumber topicNumber(Long count);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "count")
    RocketmqConsumerGroupNumber groupNumber(Long count);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "value")
    RocketmqMessagesInTotal messagesInTotal(Long value);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "value")
    RocketmqMessagesOutTotal messagesOutTotal(Long value);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "value")
    RocketmqThroughputInTotal throughputInTotal(Long value);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topic")
    @Mapping(target = "window", source = "window")
    @Mapping(target = "valueWindowCount", source = "count")
    @Mapping(target = "value", source = "rate")
    RocketmqTopicMessagesIn topicMessages(String topic, String window, long count, float rate);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topic")
    @Mapping(target = "groupName", source = "group")
    @Mapping(target = "window", source = "window")
    @Mapping(target = "valueWindowCount", source = "count")
    @Mapping(target = "value", source = "rate")
    RocketmqGroupMessagesOut groupMessages(String topic, String group, String window, long count, float rate);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topicName")
    @Mapping(target = "groupName", source = "groupName")
    @Mapping(target = "queueId", source = "queueId")
    @Mapping(target = "value", source = "value")
    RocketmqConsumerLagLatency consumerLag(String topicName, String groupName, String queueId, Long value);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topicName")
    @Mapping(target = "groupName", source = "groupName")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "value", source = "value")
    RocketmqConsumerSuccessTps consumerSuccess(String topicName, String groupName, String clientId, Float value);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topicName")
    @Mapping(target = "groupName", source = "groupName")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "value", source = "value")
    RocketmqConsumerFailedTps consumerFailed(String topicName, String groupName, String clientId, Float value);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "topicName", source = "topicName")
    @Mapping(target = "groupName", source = "groupName")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "value", source = "value")
    RocketmqConsumerProcessTime consumerProcessTime(String topicName, String groupName, String clientId, Float value);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "storeType", source = "storeType")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "value", source = "value")
    RocketmqStorageDiskUsage diskUsage(String storeType, String path, Float value);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "value", source = "value")
    RocketmqStorageDiskFreeBytes diskFreeBytes(Long value);

}
