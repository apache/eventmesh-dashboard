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

package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.EtcdSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.NacosConfigSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.NacosNamingSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.NacosSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RedisSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RocketMQProduceSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RocketMQPushConsumerSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RocketMQRemotingSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeGrpcConsumerSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeGrpcProducerSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeHttpConsumerSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeHttpProducerSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeTcpCloudEventSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeTcpEventMeshSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.RuntimeTcpOpenMessageSDKOperation;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * SDK manager is a singleton to manage all SDK clients, it is a facade to create, delete and get a client.
 */
public class SDKManager {

    private static volatile SDKManager INSTANCE = null;

    public static synchronized SDKManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SDKManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SDKManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * inner key is the unique key of a client, such as (ip + port) they are defined in CreateClientConfig
     * <p>
     * key: SDKTypeEnum
     * value: A map collection is used with key being (ip+port) and value being client.
     * @see CreateSDKConfig#getUniqueKey()
     */
    private static final Map<SDKTypeEnum, Map<String, Object>> clientMap = new ConcurrentHashMap<>();

    /**
     * Initialise the SDKOperation object instance according to SDKTypeEnum.
     * <p>
     * key: SDKTypeEnum
     * value: SDKOperation
     * @see SDKTypeEnum
     * @see SDKOperation
     */
    private static final Map<SDKTypeEnum, SDKOperation<?>> clientCreateOperationMap = new ConcurrentHashMap<>();

    // register all client create operation
    static {
        for (SDKTypeEnum clientTypeEnum : SDKTypeEnum.values()) {
            clientMap.put(clientTypeEnum, new ConcurrentHashMap<>());
        }
        // redis
        clientCreateOperationMap.put(SDKTypeEnum.STORAGE_REDIS, new RedisSDKOperation());

        // rocketmq
        clientCreateOperationMap.put(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, new RocketMQRemotingSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.STORAGE_ROCKETMQ_PRODUCER, new RocketMQProduceSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.STORAGE_ROCKETMQ_CONSUMER, new RocketMQPushConsumerSDKOperation());

        // nacos
        clientCreateOperationMap.put(SDKTypeEnum.META_NACOS, new NacosSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.META_NACOS_CONFIG, new NacosConfigSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.META_NACOS_NAMING, new NacosNamingSDKOperation());

        // etcd
        clientCreateOperationMap.put(SDKTypeEnum.META_ETCD, new EtcdSDKOperation());

        // eventmesh_runtime
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_EVENTMESH_CLIENT, new RuntimeSDKOperation());

        // eventmesh_runtime_tcp
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_TCP_CLOUDEVENT_CLIENT, new RuntimeTcpCloudEventSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_TCP_EVENTMESH_CLIENT, new RuntimeTcpEventMeshSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_TCP_OPENMESSAGE_CLIENT, new RuntimeTcpOpenMessageSDKOperation());

        // eventmesh_runtime_http
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_HTTP_PRODUCER, new RuntimeHttpProducerSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_HTTP_CONSUMER, new RuntimeHttpConsumerSDKOperation());

        // eventmesh_runtime_grpc
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_GRPC_PRODUCER, new RuntimeGrpcProducerSDKOperation());
        clientCreateOperationMap.put(SDKTypeEnum.RUNTIME_GRPC_CONSUMER, new RuntimeGrpcConsumerSDKOperation());
    }

    private SDKManager() {
    }

    /**
     * Create SDK client through (SDKTypeEnum) clientTypeEnum, (CreateSDKConfig) config.
     */
    public <T> SimpleEntry<String, T> createClient(SDKTypeEnum clientTypeEnum, CreateSDKConfig config) {

        final String uniqueKey = config.getUniqueKey();

        Map<String, Object> clients = clientMap.get(clientTypeEnum);

        Object client = clients.get(uniqueKey);
        SimpleEntry<String, ?> result = new SimpleEntry<>(uniqueKey, client);
        if (Objects.isNull(client)) {
            SDKOperation<?> clientCreateOperation = clientCreateOperationMap.get(clientTypeEnum);
            result = clientCreateOperation.createClient(config);
            clients.put(result.getKey(), result.getValue());
        }
        try {
            return (SimpleEntry<String, T>) result;
        } catch (Exception e) {
            throw new RuntimeException("create client error", e);
        }
    }

    public void deleteClient(SDKTypeEnum clientTypeEnum, String uniqueKey) {
        Map<String, Object> clients = clientMap.get(clientTypeEnum);
        SDKOperation<?> operation = clientCreateOperationMap.get(clientTypeEnum);
        try {
            operation.close(clients.get(uniqueKey));
        } catch (Exception e) {
            throw new RuntimeException("close client error", e);
        }
        clients.remove(uniqueKey);
    }

    public Object getClient(SDKTypeEnum clientTypeEnum, String uniqueKey) {
        return clientMap.get(clientTypeEnum).get(uniqueKey);
    }

    // get all client
    public Map<String, Object> getClients(SDKTypeEnum clientTypeEnum) {
        return clientMap.get(clientTypeEnum);
    }
}
