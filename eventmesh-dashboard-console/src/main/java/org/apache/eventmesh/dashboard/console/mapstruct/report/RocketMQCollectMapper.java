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
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerConnectionNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset;

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

}
