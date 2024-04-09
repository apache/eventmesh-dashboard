package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.http.producer.EventMeshHttpProducer;
import org.apache.eventmesh.common.utils.IPUtils;
import org.apache.eventmesh.common.utils.ThreadUtils;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

import java.util.AbstractMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeHttpProducerSDKOperationTest {

    private final RuntimeHttpProducerSDKOperation httpProducerSDKOperation = new RuntimeHttpProducerSDKOperation();

    @Test
    void testCreateClient() {
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
        final AbstractMap.SimpleEntry<String, EventMeshHttpProducer> httpProducerSimpleEntry =
            httpProducerSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10105", httpProducerSimpleEntry.getKey());
    }
}
