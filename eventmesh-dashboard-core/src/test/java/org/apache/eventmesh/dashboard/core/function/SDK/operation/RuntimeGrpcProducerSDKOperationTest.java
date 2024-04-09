package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.grpc.producer.EventMeshGrpcProducer;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

import java.util.AbstractMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeGrpcProducerSDKOperationTest {

    private final RuntimeGrpcProducerSDKOperation grpcProducerSDKOperation = new RuntimeGrpcProducerSDKOperation();

    @Test
    void testCreateClient() {
        final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
            .runtimeServerAddress("127.0.0.1:10205")
            .producerGroup("EventMeshTest-producerGroup")
            .env("test")
            .idc("idc")
            .sys("1234")
            .build();
        final AbstractMap.SimpleEntry<String, EventMeshGrpcProducer> grpcProducerSimpleEntry =
            grpcProducerSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10205", grpcProducerSimpleEntry.getKey());
    }
}
