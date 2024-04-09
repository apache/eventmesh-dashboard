package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.client.grpc.consumer.EventMeshGrpcConsumer;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;

import java.util.AbstractMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeGrpcConsumerSDKOperationTest {

    private final RuntimeGrpcConsumerSDKOperation grpcConsumerSDKOperation = new RuntimeGrpcConsumerSDKOperation();

    @Test
    void testCreateClient() {
        final CreateRuntimeConfig runtimeConfig = CreateRuntimeConfig.builder()
            .runtimeServerAddress("127.0.0.1:10205")
            .consumerGroup("EventMeshTest-consumerGroup")
            .env("test")
            .idc("idc")
            .sys("1234")
            .build();
        final AbstractMap.SimpleEntry<String, EventMeshGrpcConsumer> grpcConsumerSimpleEntry =
            grpcConsumerSDKOperation.createClient(runtimeConfig);
        Assertions.assertEquals("127.0.0.1:10205", grpcConsumerSimpleEntry.getKey());
    }
}
