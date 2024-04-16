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

package org.apache.eventmesh.dashboard.core.function.SDK.wrapper;

import org.apache.eventmesh.client.grpc.consumer.EventMeshGrpcConsumer;
import org.apache.eventmesh.client.grpc.producer.EventMeshGrpcProducer;
import org.apache.eventmesh.client.http.consumer.EventMeshHttpConsumer;
import org.apache.eventmesh.client.http.producer.EventMeshHttpProducer;
import org.apache.eventmesh.client.tcp.impl.cloudevent.CloudEventTCPClient;
import org.apache.eventmesh.client.tcp.impl.eventmeshmessage.EventMeshMessageTCPClient;
import org.apache.eventmesh.client.tcp.impl.openmessage.OpenMessageTCPClient;
import org.apache.eventmesh.common.exception.EventMeshException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
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
        } catch (EventMeshException e) {
            log.error("runtime client close failed", e);
        }
    }
}
