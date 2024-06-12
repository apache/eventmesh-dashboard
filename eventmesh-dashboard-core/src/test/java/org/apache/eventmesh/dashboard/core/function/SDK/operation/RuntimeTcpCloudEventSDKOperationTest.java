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

import org.apache.eventmesh.client.tcp.common.EventMeshCommon;
import org.apache.eventmesh.client.tcp.impl.cloudevent.CloudEventTCPClient;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime.RuntimeTcpCloudEventSDKOperation;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
public class RuntimeTcpCloudEventSDKOperationTest {

    private final RuntimeTcpCloudEventSDKOperation runtimeTCPPushSDKOperation = new RuntimeTcpCloudEventSDKOperation();

    @Test
    void testCreateClient() {
        SimpleEntry<String, CloudEventTCPClient> simpleEntry = null;
        try {
            final UserAgent userAgent = UserAgent.builder()
                .env("test")
                .host("localhost")
                .password("123456")
                .username("eventmesh")
                .group("EventmeshTestGroup")
                .path("/")
                .port(8366)
                .subsystem("502")
                .pid(32894)
                .version("2.1")
                .idc("A")
                .purpose(EventMeshCommon.USER_AGENT_PURPOSE_PUB)
                .build();
            log.info("{}", userAgent);
            final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
                .runtimeServerAddress("127.0.0.1:10000")
                .userAgent(userAgent)
                .build();
            log.info("{}", runtimeConfig);
            simpleEntry = runtimeTCPPushSDKOperation.createClient(runtimeConfig);
            Assertions.assertEquals("127.0.0.1:10000", simpleEntry.getKey());
            simpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create runtime tcp CloudEvent client failed", e);
            if (simpleEntry != null) {
                simpleEntry.getValue().close();
            }
        }
    }
}
