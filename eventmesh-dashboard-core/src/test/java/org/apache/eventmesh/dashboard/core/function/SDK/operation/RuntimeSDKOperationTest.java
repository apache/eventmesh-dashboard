package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.tcp.common.EventMeshCommon;
import org.apache.eventmesh.common.Constants;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.RuntimeSDKWrapper;

import java.util.AbstractMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RuntimeSDKOperationTest {

    private final RuntimeSDKOperation runtimeSDKOperation = new RuntimeSDKOperation();

    @Test
    void testCreateClient() {
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
        final AbstractMap.SimpleEntry<String, RuntimeSDKWrapper> sdkWrapperSimpleEntry =
            runtimeSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10000", sdkWrapperSimpleEntry.getKey());
        Assertions.assertNotNull(sdkWrapperSimpleEntry.getValue().getTcpEventMeshClient());
    }
}