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

import org.apache.eventmesh.client.http.producer.EventMeshHttpProducer;
import org.apache.eventmesh.common.utils.IPUtils;
import org.apache.eventmesh.common.utils.ThreadUtils;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime.RuntimeHttpProducerSDKOperation;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
public class RuntimeHttpProducerSDKOperationTest {

    private final RuntimeHttpProducerSDKOperation httpProducerSDKOperation = new RuntimeHttpProducerSDKOperation();

    @Test
    void testCreateClient() {
        SimpleEntry<String, EventMeshHttpProducer> httpProducerSimpleEntry = null;
        try {
            final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
                .runtimeServerAddress("127.0.0.1:10105")
                .producerGroup("EventMeshTest-producerGroup")
                .env("test")
                .idc("idc")
                .ip(IPUtils.getLocalAddress())
                .sys("1234")
                .pid(String.valueOf(ThreadUtils.getPID()))
                .username("eventmesh")
                .password("123456")
                .build();
            httpProducerSimpleEntry = httpProducerSDKOperation.createClient(runtimeConfig);
            Assertions.assertEquals("127.0.0.1:10105", httpProducerSimpleEntry.getKey());
            httpProducerSimpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create runtime EventMesh HTTP producer client failed", e);
            if (httpProducerSimpleEntry != null) {
                httpProducerSimpleEntry.getValue().close();
            }
        }
    }
}
