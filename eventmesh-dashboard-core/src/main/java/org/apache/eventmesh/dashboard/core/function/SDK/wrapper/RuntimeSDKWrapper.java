
package org.apache.eventmesh.dashboard.core.function.SDK.wrapper;

import org.apache.eventmesh.client.grpc.consumer.EventMeshGrpcConsumer;
import org.apache.eventmesh.client.grpc.producer.EventMeshGrpcProducer;
import org.apache.eventmesh.client.http.consumer.EventMeshHttpConsumer;
import org.apache.eventmesh.client.http.producer.EventMeshHttpProducer;
import org.apache.eventmesh.client.tcp.impl.cloudevent.CloudEventTCPClient;
import org.apache.eventmesh.client.tcp.impl.eventmeshmessage.EventMeshMessageTCPClient;
import org.apache.eventmesh.client.tcp.impl.openmessage.OpenMessageTCPClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuntimeSDKWrapper {

    private CloudEventTCPClient tcpCloudEventClient;
    private EventMeshMessageTCPClient tcpEventMeshClient;
    private OpenMessageTCPClient openMessageTCPClient;

    private EventMeshHttpProducer httpProducerClient;
    private EventMeshHttpConsumer httpConsumerClient;

    private EventMeshGrpcProducer grpcProducerClient;
    private EventMeshGrpcConsumer grpcConsumerClient;

    public void close() {
        try {
            if (tcpCloudEventClient != null) {
                tcpCloudEventClient.close();
            } else if (tcpEventMeshClient != null) {
                tcpEventMeshClient.close();
            } else if (openMessageTCPClient != null) {
                openMessageTCPClient.close();
            } else if (httpProducerClient != null) {
                httpProducerClient.close();
            } else if (httpConsumerClient != null) {
                httpConsumerClient.close();
            } else if (grpcProducerClient != null) {
                grpcProducerClient.close();
            } else if (grpcConsumerClient != null) {
                grpcConsumerClient.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
