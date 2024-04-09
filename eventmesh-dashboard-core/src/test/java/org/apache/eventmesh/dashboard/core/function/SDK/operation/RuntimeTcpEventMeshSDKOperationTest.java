package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.tcp.common.EventMeshCommon;
import org.apache.eventmesh.client.tcp.impl.eventmeshmessage.EventMeshMessageTCPClient;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeTcpEventMeshSDKOperationTest {

    private final RuntimeTcpEventMeshSDKOperation eventMeshSDKOperation = new RuntimeTcpEventMeshSDKOperation();

    @Test
    void testCreateClient() {
        final UserAgent userAgent = UserAgent.builder()
            .env("test")
            .host("localhost")
            .password("123456")
            .username("eventmesh")
            .group("EventmeshTestGroup")
            .path("/")
            .port(8365)
            .subsystem("501")
            .pid(32893)
            .version("2.1")
            .idc("A")
            .purpose(EventMeshCommon.USER_AGENT_PURPOSE_PUB)
            .build();
        log.info("userAgent {}", userAgent);
        final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
            .runtimeServerAddress("127.0.0.1:10000")
            .userAgent(userAgent)
            .build();
        log.info("{}", runtimeConfig);
        final SimpleEntry<String, EventMeshMessageTCPClient> simpleEntry =
            eventMeshSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10000", simpleEntry.getKey());
    }
}
