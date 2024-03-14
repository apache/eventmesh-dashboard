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

package org.apache.eventmesh.dashboard.console.function.client;

import org.apache.eventmesh.dashboard.console.function.client.config.CreateClientConfig;
import org.apache.eventmesh.dashboard.console.function.client.operation.NacosConfigSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.NacosNamingSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.NacosSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.RedisSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.RocketMQProduceSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.RocketMQPushConsumerSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.operation.RocketMQRemotingSDKOperation;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;



/**
 * SDK manager is a singleton to manage all SDK clients, it is a facade to create, delete and get a client.

 */
public class SDKManager {

    private static final SDKManager INSTANCE = new SDKManager();


    public static SDKManager getInstance() {
        return INSTANCE;
    }

    /**
     * inner key is the unique key of a client, such as (ip + port) they are defined in CreateClientConfig
     *
     * @see CreateClientConfig#getUniqueKey()
     */

    private final Map<ClientTypeEnum, Map<String, Object>> clientMap = new ConcurrentHashMap<>();

    private final Map<ClientTypeEnum, SDKOperation<?>> clientCreateOperationMap = new ConcurrentHashMap<>();

    // register all client create operation
    {
        for (ClientTypeEnum clientTypeEnum : ClientTypeEnum.values()) {
            clientMap.put(clientTypeEnum, new ConcurrentHashMap<>());
        }

        clientCreateOperationMap.put(ClientTypeEnum.STORAGE_REDIS, new RedisSDKOperation());

        clientCreateOperationMap.put(ClientTypeEnum.STORAGE_ROCKETMQ_REMOTING, new RocketMQRemotingSDKOperation());
        clientCreateOperationMap.put(ClientTypeEnum.STORAGE_ROCKETMQ_PRODUCER, new RocketMQProduceSDKOperation());
        clientCreateOperationMap.put(ClientTypeEnum.STORAGE_ROCKETMQ_CONSUMER, new RocketMQPushConsumerSDKOperation());

        clientCreateOperationMap.put(ClientTypeEnum.CENTER_NACOS, new NacosSDKOperation());
        clientCreateOperationMap.put(ClientTypeEnum.CENTER_NACOS_CONFIG, new NacosConfigSDKOperation());
        clientCreateOperationMap.put(ClientTypeEnum.CENTER_NACOS_NAMING, new NacosNamingSDKOperation());

    }

    private SDKManager() {
    }

    public <T> SimpleEntry<String, T> createClient(ClientTypeEnum clientTypeEnum, CreateClientConfig config) {
        return createClient(clientTypeEnum, config.getUniqueKey(), config);
    }

    public <T> SimpleEntry<String, T> createClient(ClientTypeEnum clientTypeEnum, String uniqueKey, CreateClientConfig config) {

        Map<String, Object> clients = this.clientMap.get(clientTypeEnum);

        Object client = clients.get(uniqueKey);
        SimpleEntry<String, ?> result = new SimpleEntry<>(uniqueKey, client);
        if (Objects.isNull(client)) {
            SDKOperation<?> clientCreateOperation = this.clientCreateOperationMap.get(clientTypeEnum);
            result = clientCreateOperation.createClient(config);
            clients.put(result.getKey(), result.getValue());
        }
        try {
            return (SimpleEntry<String, T>) result;
        } catch (Exception e) {
            throw new RuntimeException("create client error", e);
        }
    }

    public void deleteClient(ClientTypeEnum clientTypeEnum, String uniqueKey) {
        Map<String, Object> clients = this.clientMap.get(clientTypeEnum);
        SDKOperation<?> operation = this.clientCreateOperationMap.get(clientTypeEnum);
        try {
            operation.close(clients.get(uniqueKey));
        } catch (Exception e) {
            throw new RuntimeException("close client error", e);
        }
        clients.remove(uniqueKey);
    }

    public Object getClient(ClientTypeEnum clientTypeEnum, String uniqueKey) {
        return this.clientMap.get(clientTypeEnum).get(uniqueKey);
    }
}
