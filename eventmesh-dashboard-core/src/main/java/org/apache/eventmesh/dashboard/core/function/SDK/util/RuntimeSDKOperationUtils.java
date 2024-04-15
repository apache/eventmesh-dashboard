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

package org.apache.eventmesh.dashboard.core.function.SDK.util;

import org.apache.eventmesh.client.grpc.config.EventMeshGrpcClientConfig;
import org.apache.eventmesh.client.http.conf.EventMeshHttpClientConfig;
import org.apache.eventmesh.client.tcp.conf.EventMeshTCPClientConfig;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

public class RuntimeSDKOperationUtils {

    public static EventMeshTCPClientConfig buildEventMeshTCPClientConfig(String serverAddress, UserAgent userAgent) {
        String clientHost = serverAddress.split(":")[0];
        int clientPort = Integer.parseInt(serverAddress.split(":")[1]);
        return EventMeshTCPClientConfig.builder()
            .host(clientHost)
            .port(clientPort)
            .userAgent(userAgent)
            .build();
    }

    public static UserAgent buildUserAgent(UserAgent userAgent) {
        return UserAgent.builder()
            .host(userAgent.getHost())
            .subsystem(userAgent.getSubsystem())
            .port(userAgent.getPort())
            .group(userAgent.getGroup())
            .idc(userAgent.getIdc())
            .env(userAgent.getPassword())
            .pid(userAgent.getPid())
            .path(userAgent.getPath())
            .username(userAgent.getUsername())
            .password(userAgent.getPassword())
            .version(userAgent.getVersion())
            .purpose(userAgent.getPurpose())
            .build();
    }

    public static EventMeshHttpClientConfig buildEventMeshHttpConsumerConfig(CreateRuntimeConfig runtimeConfig) {
        return EventMeshHttpClientConfig.builder()
            .liteEventMeshAddr(runtimeConfig.getRuntimeServerAddress())
            .consumerGroup(runtimeConfig.getConsumerGroup())
            .sys(runtimeConfig.getSys())
            .env(runtimeConfig.getEnv())
            .idc(runtimeConfig.getIdc())
            .ip(runtimeConfig.getIp())
            .pid(runtimeConfig.getPid())
            .build();
    }

    public static EventMeshHttpClientConfig buildEventMeshHttpProducerConfig(CreateRuntimeConfig runtimeConfig) {
        return EventMeshHttpClientConfig.builder()
            .liteEventMeshAddr(runtimeConfig.getRuntimeServerAddress())
            .producerGroup(runtimeConfig.getProducerGroup())
            .env(runtimeConfig.getEnv())
            .idc(runtimeConfig.getIdc())
            .sys(runtimeConfig.getSys())
            .pid(runtimeConfig.getPid())
            .userName(runtimeConfig.getUsername())
            .password(runtimeConfig.getPassword())
            .build();
    }

    public static EventMeshGrpcClientConfig buildEventMeshGrpcConsumerConfig(CreateRuntimeConfig runtimeConfig) {
        final String grpcServerAddress = runtimeConfig.getRuntimeServerAddress();
        String clientHost = grpcServerAddress.split(":")[0];
        int clientPort = Integer.parseInt(grpcServerAddress.split(":")[1]);
        return EventMeshGrpcClientConfig.builder()
            .serverAddr(clientHost)
            .serverPort(clientPort)
            .consumerGroup(runtimeConfig.getConsumerGroup())
            .env(runtimeConfig.getEnv())
            .idc(runtimeConfig.getIdc())
            .sys(runtimeConfig.getSys())
            .build();
    }

    public static EventMeshGrpcClientConfig buildEventMeshGrpcProducerConfig(CreateRuntimeConfig runtimeConfig) {
        final String grpcServerAddress = runtimeConfig.getRuntimeServerAddress();
        String clientHost = grpcServerAddress.split(":")[0];
        int clientPort = Integer.parseInt(grpcServerAddress.split(":")[1]);
        return EventMeshGrpcClientConfig.builder()
            .serverAddr(clientHost)
            .serverPort(clientPort)
            .producerGroup(runtimeConfig.getProducerGroup())
            .env(runtimeConfig.getEnv())
            .idc(runtimeConfig.getIdc())
            .sys(runtimeConfig.getSys())
            .build();
    }
}
