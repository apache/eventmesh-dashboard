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

package org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime;

import org.apache.eventmesh.client.grpc.consumer.EventMeshGrpcConsumer;
import org.apache.eventmesh.client.grpc.producer.EventMeshGrpcProducer;
import org.apache.eventmesh.client.http.consumer.EventMeshHttpConsumer;
import org.apache.eventmesh.client.http.producer.EventMeshHttpProducer;
import org.apache.eventmesh.client.tcp.impl.cloudevent.CloudEventTCPClient;
import org.apache.eventmesh.client.tcp.impl.eventmeshmessage.EventMeshMessageTCPClient;
import org.apache.eventmesh.client.tcp.impl.openmessage.OpenMessageTCPClient;
import org.apache.eventmesh.common.Constants;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.RuntimeSDKWrapper;

import java.util.AbstractMap.SimpleEntry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeSDKOperation extends AbstractSDKOperation<RuntimeSDKWrapper> {

    private final RuntimeTcpCloudEventSDKOperation tcpCloudEventSDKOperation = new RuntimeTcpCloudEventSDKOperation();
    private final RuntimeTcpEventMeshSDKOperation tcpEventMeshSDKOperation = new RuntimeTcpEventMeshSDKOperation();
    private final RuntimeTcpOpenMessageSDKOperation tcpOpenMessageSDKOperation = new RuntimeTcpOpenMessageSDKOperation();

    private final RuntimeHttpProducerSDKOperation httpProducerSDKOperation = new RuntimeHttpProducerSDKOperation();
    private final RuntimeHttpConsumerSDKOperation httpConsumerSDKOperation = new RuntimeHttpConsumerSDKOperation();

    private final RuntimeGrpcProducerSDKOperation grpcProducerSDKOperation = new RuntimeGrpcProducerSDKOperation();
    private final RuntimeGrpcConsumerSDKOperation grpcConsumerSDKOperation = new RuntimeGrpcConsumerSDKOperation();

    @Override
    public SimpleEntry<String, RuntimeSDKWrapper> createClient(CreateSDKConfig clientConfig) {
        CreateRuntimeConfig runtimeConfig = (CreateRuntimeConfig) clientConfig;
        final String protocol = ((CreateRuntimeConfig) clientConfig).getProtocol();
        final String protocolName = ((CreateRuntimeConfig) clientConfig).getProtocolName();
        final String clientType = ((CreateRuntimeConfig) clientConfig).getClientType();

        SimpleEntry<String, CloudEventTCPClient> cloudSimpleEntry = null;
        SimpleEntry<String, EventMeshMessageTCPClient> eventMeshMessageSimpleEntry = null;
        SimpleEntry<String, OpenMessageTCPClient> openMessageSimpleEntry = null;

        SimpleEntry<String, EventMeshHttpProducer> httpProducerSimpleEntry = null;
        SimpleEntry<String, EventMeshHttpConsumer> httpConsumerSimpleEntry = null;

        SimpleEntry<String, EventMeshGrpcProducer> grpcProducerSimpleEntry = null;
        SimpleEntry<String, EventMeshGrpcConsumer> grpcConsumerSimpleEntry = null;

        switch (protocol) {
            case Constants.TCP:
                switch (protocolName) {
                    case Constants.CLOUD_EVENTS_PROTOCOL_NAME:
                        cloudSimpleEntry = tcpCloudEventSDKOperation.createClient(runtimeConfig);
                        break;
                    case Constants.EM_MESSAGE_PROTOCOL_NAME:
                        eventMeshMessageSimpleEntry = tcpEventMeshSDKOperation.createClient(runtimeConfig);
                        break;
                    case Constants.OPEN_MESSAGE_PROTOCOL_NAME:
                        openMessageSimpleEntry = tcpOpenMessageSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            case Constants.HTTP:
                switch (clientType) {
                    case "producer":
                        httpProducerSimpleEntry = httpProducerSDKOperation.createClient(runtimeConfig);
                        break;
                    case "consumer":
                        httpConsumerSimpleEntry = httpConsumerSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            case Constants.GRPC:
                switch (clientType) {
                    case "producer":
                        grpcProducerSimpleEntry = grpcProducerSDKOperation.createClient(runtimeConfig);
                        break;
                    case "consumer":
                        grpcConsumerSimpleEntry = grpcConsumerSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            default:
                log.warn("clients that do not support the current protocol");
                break;
        }
        RuntimeSDKWrapper runtimeClient = new RuntimeSDKWrapper(
            cloudSimpleEntry != null ? cloudSimpleEntry.getValue() : null,
            eventMeshMessageSimpleEntry != null ? eventMeshMessageSimpleEntry.getValue() : null,
            openMessageSimpleEntry != null ? openMessageSimpleEntry.getValue() : null,
            httpProducerSimpleEntry != null ? httpProducerSimpleEntry.getValue() : null,
            httpConsumerSimpleEntry != null ? httpConsumerSimpleEntry.getValue() : null,
            grpcProducerSimpleEntry != null ? grpcProducerSimpleEntry.getValue() : null,
            grpcConsumerSimpleEntry != null ? grpcConsumerSimpleEntry.getValue() : null
        );
        return new SimpleEntry<>(clientConfig.getUniqueKey(), runtimeClient);
    }

    @Override
    public void close(Object client) {
        castClient(client).close();
    }
}
