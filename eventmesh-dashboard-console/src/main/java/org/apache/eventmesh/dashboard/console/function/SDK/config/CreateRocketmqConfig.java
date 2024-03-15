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

package org.apache.eventmesh.dashboard.console.function.SDK.config;

import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import lombok.Data;

@Data
public class CreateRocketmqConfig implements CreateSDKConfig {

    // common
    private String nameServerUrl;
    private String brokerUrl;

    //consumer
    private String consumerGroup;
    private MessageModel messageModel = MessageModel.CLUSTERING;

    //producer
    private String producerGroup;

    //topic
    private String topic;
    private String subExpression = "*";

    private MessageListener messageListener;


    @Override
    public String getUniqueKey() {
        if (nameServerUrl != null) {
            return nameServerUrl;
        } else if (brokerUrl != null) {
            return brokerUrl;
        }
        return null;
    }
}
