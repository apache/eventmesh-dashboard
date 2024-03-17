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

package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRocketmqConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.AbstractMap.SimpleEntry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RocketMQPushConsumerSDKOperation extends AbstractSDKOperation<DefaultMQPushConsumer> {

    @Override
    public SimpleEntry<String, DefaultMQPushConsumer> createClient(CreateSDKConfig clientConfig) {
        DefaultMQPushConsumer consumer = null;
        try {
            CreateRocketmqConfig config = (CreateRocketmqConfig) clientConfig;
            consumer = new DefaultMQPushConsumer(config.getConsumerGroup());
            consumer.setMessageModel(config.getMessageModel());
            consumer.setNamesrvAddr(config.getNameServerUrl());
            consumer.subscribe(config.getTopic(), config.getSubExpression());
            consumer.registerMessageListener(config.getMessageListener());
            consumer.start();
        } catch (MQClientException e) {
            log.error("create rocketmq push consumer failed", e);
        }
        return new SimpleEntry(((CreateRocketmqConfig) clientConfig).getNameServerUrl(), consumer);
    }

    @Override
    public void close(Object client) {
        ((DefaultMQPushConsumer) client).shutdown();
    }
}
