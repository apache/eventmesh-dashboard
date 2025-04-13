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
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.RuntimeSDKWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SDKMetadata(clusterType = {ClusterType.EVENTMESH_RUNTIME}, remotingType = RemotingType.EVENT_MESH_RUNTIME, sdkTypeEnum = SDKTypeEnum.ALL)
public class RuntimeSDKOperation extends AbstractSDKOperation<RuntimeSDKWrapper, CreateRuntimeConfig> {

    private final RuntimeTcpCloudEventSDKOperation tcpCloudEventSDKOperation = new RuntimeTcpCloudEventSDKOperation();
    private final RuntimeTcpEventMeshSDKOperation tcpEventMeshSDKOperation = new RuntimeTcpEventMeshSDKOperation();
    private final RuntimeTcpOpenMessageSDKOperation tcpOpenMessageSDKOperation = new RuntimeTcpOpenMessageSDKOperation();

    private final RuntimeHttpProducerSDKOperation httpProducerSDKOperation = new RuntimeHttpProducerSDKOperation();
    private final RuntimeHttpConsumerSDKOperation httpConsumerSDKOperation = new RuntimeHttpConsumerSDKOperation();

    private final RuntimeGrpcProducerSDKOperation grpcProducerSDKOperation = new RuntimeGrpcProducerSDKOperation();
    private final RuntimeGrpcConsumerSDKOperation grpcConsumerSDKOperation = new RuntimeGrpcConsumerSDKOperation();

    @Override
    public RuntimeSDKWrapper createClient(CreateRuntimeConfig runtimeConfig) throws Exception {
        final String protocol = runtimeConfig.getProtocol();
        final String protocolName = runtimeConfig.getProtocolName();
        final String clientType = runtimeConfig.getClientType();

        CloudEventTCPClient cloudSimple = null;
        EventMeshMessageTCPClient eventMeshMessageSimple = null;
        OpenMessageTCPClient openMessageSimple = null;

        EventMeshHttpProducer httpProducerSimple = null;
        EventMeshHttpConsumer httpConsumerSimple = null;

        EventMeshGrpcProducer grpcProducerSimple = null;
        EventMeshGrpcConsumer grpcConsumerSimple = null;

        switch (protocol) {
            case Constants.TCP:
                switch (protocolName) {
                    case Constants.CLOUD_EVENTS_PROTOCOL_NAME:
                        cloudSimple = tcpCloudEventSDKOperation.createClient(runtimeConfig);
                        break;
                    case Constants.EM_MESSAGE_PROTOCOL_NAME:
                        eventMeshMessageSimple = tcpEventMeshSDKOperation.createClient(runtimeConfig);
                        break;
                    case Constants.OPEN_MESSAGE_PROTOCOL_NAME:
                        openMessageSimple = tcpOpenMessageSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            case Constants.HTTP:
                switch (clientType) {
                    case "producer":
                        httpProducerSimple = httpProducerSDKOperation.createClient(runtimeConfig);
                        break;
                    case "consumer":
                        httpConsumerSimple = httpConsumerSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            case Constants.GRPC:
                switch (clientType) {
                    case "producer":
                        grpcProducerSimple = grpcProducerSDKOperation.createClient(runtimeConfig);
                        break;
                    case "consumer":
                        grpcConsumerSimple = grpcConsumerSDKOperation.createClient(runtimeConfig);
                        break;
                    default:
                        break;
                }
                break;
            default:
                log.warn("clients that do not support the current protocol");
                break;
        }
        return new RuntimeSDKWrapper(cloudSimple, eventMeshMessageSimple, openMessageSimple, httpProducerSimple, httpConsumerSimple,
            grpcProducerSimple, grpcConsumerSimple);

    }

    @Override
    public void close(RuntimeSDKWrapper client) throws Exception {
        client.close();
    }
}
