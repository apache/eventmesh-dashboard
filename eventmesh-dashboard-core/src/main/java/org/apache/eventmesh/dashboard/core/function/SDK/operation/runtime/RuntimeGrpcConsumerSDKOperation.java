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

import static org.apache.eventmesh.dashboard.core.function.SDK.util.RuntimeSDKOperationUtils.buildEventMeshGrpcConsumerConfig;

import org.apache.eventmesh.client.grpc.config.EventMeshGrpcClientConfig;
import org.apache.eventmesh.client.grpc.consumer.EventMeshGrpcConsumer;
import org.apache.eventmesh.common.exception.EventMeshException;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.util.AbstractMap.SimpleEntry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeGrpcConsumerSDKOperation extends AbstractSDKOperation<EventMeshGrpcConsumer> {

    @Override
    public SimpleEntry<String, EventMeshGrpcConsumer> createClient(CreateSDKConfig clientConfig) {
        final CreateRuntimeConfig runtimeConfig = (CreateRuntimeConfig) clientConfig;
        final EventMeshGrpcClientConfig grpcClientConfig = buildEventMeshGrpcConsumerConfig(runtimeConfig);
        EventMeshGrpcConsumer grpcConsumer = null;
        try {
            grpcConsumer = new EventMeshGrpcConsumer(grpcClientConfig);
            grpcConsumer.init();
        } catch (EventMeshException e) {
            log.error("create runtime grpc Consumer client failed", e);
        }
        return new SimpleEntry<>(clientConfig.getUniqueKey(), grpcConsumer);
    }

    @Override
    public void close(Object client) {
        castClient(client).close();
    }
}
