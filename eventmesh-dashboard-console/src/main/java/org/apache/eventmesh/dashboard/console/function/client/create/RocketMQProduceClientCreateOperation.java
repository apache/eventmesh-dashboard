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

package org.apache.eventmesh.dashboard.console.function.client.create;

import org.apache.eventmesh.dashboard.console.function.client.AbstractClientOperation;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateClientConfig;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateRocketmqConfig;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

import lombok.extern.slf4j.Slf4j;

import javafx.util.Pair;

@Slf4j
public class RocketMQProduceClientCreateOperation extends AbstractClientOperation<DefaultMQProducer> {

    @Override
    public Pair<String, DefaultMQProducer> createClient(CreateClientConfig clientConfig) {
        DefaultMQProducer producer = null;
        try {
            CreateRocketmqConfig config = (CreateRocketmqConfig) clientConfig;
            producer = new DefaultMQProducer(config.getProducerGroup());
            producer.setNamesrvAddr(config.getNameServerUrl());
            producer.setCompressMsgBodyOverHowmuch(16);
            producer.start();
        } catch (MQClientException e) {
            log.error("create rocketmq producer failed", e);
        }
        return new Pair<>(clientConfig.getUniqueKey(), producer);
    }

    @Override
    public void close(Object client) {
        ((DefaultMQProducer) client).shutdown();
    }
}
