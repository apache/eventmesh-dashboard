package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.http.consumer.EventMeshHttpConsumer;
import org.apache.eventmesh.common.utils.IPUtils;
import org.apache.eventmesh.common.utils.ThreadUtils;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

import java.util.AbstractMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeHttpConsumerSDKOperationTest {

    private final RuntimeHttpConsumerSDKOperation httpConsumerSDKOperation = new RuntimeHttpConsumerSDKOperation();

    @Test
    void testCreateClient() {
        final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
            .runtimeServerAddress("127.0.0.1:10105")
            .consumerGroup("EventMeshTest-consumerGroup")
            .env("test")
            .idc("idc")
            .ip(IPUtils.getLocalAddress())
            .sys("1234")
            .pid(String.valueOf(ThreadUtils.getPID()))
            .username("eventmesh")
            .password("123456")
            .build();
        final AbstractMap.SimpleEntry<String, EventMeshHttpConsumer> httpConsumerSimpleEntry =
            httpConsumerSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10105", httpConsumerSimpleEntry.getKey());
    }
}
