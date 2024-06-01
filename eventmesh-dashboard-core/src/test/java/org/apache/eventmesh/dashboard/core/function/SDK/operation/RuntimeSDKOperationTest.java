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
import org.apache.eventmesh.common.Constants;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime.RuntimeSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.RuntimeSDKWrapper;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
class RuntimeSDKOperationTest {

    private final RuntimeSDKOperation runtimeSDKOperation = new RuntimeSDKOperation();

    @Test
    void testCreateClient() {
        SimpleEntry<String, RuntimeSDKWrapper> sdkWrapperSimpleEntry = null;
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
            final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
                .runtimeServerAddress("127.0.0.1:10000")
                .protocol("TCP")
                .protocolName(Constants.EM_MESSAGE_PROTOCOL_NAME)
                .userAgent(userAgent)
                .build();
            sdkWrapperSimpleEntry = runtimeSDKOperation.createClient(runtimeConfig);
            Assertions.assertEquals("127.0.0.1:10000", sdkWrapperSimpleEntry.getKey());
            Assertions.assertNotNull(sdkWrapperSimpleEntry.getValue().getTcpEventMeshClient());
            sdkWrapperSimpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create runtime client failed", e);
            if (sdkWrapperSimpleEntry != null) {
                sdkWrapperSimpleEntry.getValue().close();
            }
        }
    }
}