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

import org.apache.eventmesh.client.grpc.producer.EventMeshGrpcProducer;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime.RuntimeGrpcProducerSDKOperation;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
public class RuntimeGrpcProducerSDKOperationTest {

    private final RuntimeGrpcProducerSDKOperation grpcProducerSDKOperation = new RuntimeGrpcProducerSDKOperation();

    @Test
    void testCreateClient() {
        SimpleEntry<String, EventMeshGrpcProducer> grpcProducerSimpleEntry = null;
        try {
            final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
                .runtimeServerAddress("127.0.0.1:10205")
                .producerGroup("EventMeshTest-producerGroup")
                .env("test")
                .idc("idc")
                .sys("1234")
                .build();
            grpcProducerSimpleEntry = grpcProducerSDKOperation.createClient(runtimeConfig);
            Assertions.assertEquals("127.0.0.1:10205", grpcProducerSimpleEntry.getKey());
            grpcProducerSimpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create runtime GRPC producer client failed", e);
            if (grpcProducerSimpleEntry != null) {
                grpcProducerSimpleEntry.getValue().close();
            }
        }
    }
}
