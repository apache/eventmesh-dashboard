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

package org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRocketmqProduceSDKConfig;

import org.apache.rocketmq.client.producer.DefaultMQProducer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SDKMetadata(clusterType = ClusterType.RUNTIME_ROCKETMQ_BROKER, remotingType = RemotingType.ROCKETMQ, sdkTypeEnum = SDKTypeEnum.PRODUCER)
public class RocketMQProduceSDKOperation extends AbstractSDKOperation<DefaultMQProducer, CreateRocketmqProduceSDKConfig> {

    @Override
    public DefaultMQProducer createClient(CreateRocketmqProduceSDKConfig clientConfig) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(clientConfig.getProducerGroup());
        producer.setNamesrvAddr(clientConfig.doUniqueKey());
        producer.setCompressMsgBodyOverHowmuch(16);
        producer.start();
        return producer;
    }

    @Override
    public void close(DefaultMQProducer client) {
        client.shutdown();
    }
}
