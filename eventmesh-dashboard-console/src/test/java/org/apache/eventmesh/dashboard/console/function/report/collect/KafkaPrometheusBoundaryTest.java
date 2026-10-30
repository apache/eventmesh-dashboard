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
package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.CollectExporter;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaPrometheusBoundaryTest {

    private CollectManage manager;
    private ReportEngine engine;
    private ClusterMetadata legacyCluster;
    private RuntimeMetadata exporterNode;
    private CollectMetadata legacyConfig;
    private HttpClient client;
    private HttpResponse<String> response;
    private final BlockingQueue<Map<Class<?>, List<Object>>> batches = new LinkedBlockingQueue<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        manager = new CollectManage();
        ReportHandlerManage reports = mock(ReportHandlerManage.class);
        engine = mock(ReportEngine.class);
        ReportMetaData sample = new ReportMetaData();
        sample.setReportName("sample");
        sample.setClazz(ClusterId.class);
        sample.setFieldList(List.of());
        when(reports.getReportMetaDataMap()).thenReturn(Map.of("sample", sample));
        when(reports.getAggregationMetaDataMap()).thenReturn(Map.of());
        when(reports.getReportEngine()).thenReturn(engine);
        doAnswer(invocation -> {
            Map<Class<?>, List<Object>> data = invocation.getArgument(0);
            batches.add(new HashMap<>(data));
            return null;
        }).when(engine).batchInsertByClass(any());
        manager.setReportHandlerManage(reports);
        manager.init();

        legacyCluster = cluster(21L, ClusterType.EVENTMESH_JVM_CLUSTER);
        exporterNode = new RuntimeMetadata();
        exporterNode.setId(22L);
        exporterNode.setClusterId(21L);
        exporterNode.setStatus(1L);
        exporterNode.setClusterType(ClusterType.PROMETHEUS_EXPORTER);
        exporterNode.setHost("http://127.0.0.1");
        exporterNode.setPort(9999);
        legacyConfig = config(23L, 21L, ClusterType.EVENTMESH_JVM_CLUSTER, CollectType.PROMETHEUS);
        client = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        when(response.body()).thenReturn("sample{label=\"fixture\"} 7\n");
    }

    @AfterEach
    public void shutdown() throws Exception {
        java.util.concurrent.ExecutorService executor =
            (java.util.concurrent.ExecutorService) FieldUtils.readField(manager, "threadPoolExecutor", true);
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void mixedClustersDeliverRegisteredExporterSample() throws Exception {
        ClusterMetadata kafka = cluster(31L, ClusterType.STORAGE_KAFKA_CLUSTER);
        CollectMetadata kafkaConfig = config(33L, 31L, ClusterType.STORAGE_KAFKA_CLUSTER, CollectType.EVENTMESH);
        manager.handlerData(List.of(legacyCluster, kafka), List.of(exporterNode), List.of(legacyConfig, kafkaConfig));
        installClient();
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(response);

        manager.collect();
        Map<Class<?>, List<Object>> first = nextBatch();
        Map<Class<?>, List<Object>> second = nextBatch();
        Map<Class<?>, List<Object>> sampleBatch = first.containsKey(ClusterId.class) ? first : second;
        Map<Class<?>, List<Object>> otherBatch = sampleBatch == first ? second : first;
        assertTrue(otherBatch.isEmpty());
        assertNotNull(sampleBatch.get(ClusterId.class));
        assertEquals(1, sampleBatch.get(ClusterId.class).size());
        ClusterId row = (ClusterId) sampleBatch.get(ClusterId.class).get(0);
        assertEquals(21L, row.getClustersId());
        assertEquals("cluster-21", row.getClustersName());
        assertEquals(4L, row.getOrganizationId());
        verify(client).send(org.mockito.ArgumentMatchers.argThat(request ->
            request.uri().toString().equals("http://127.0.0.1:9999")),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    public void exporterRequestFailureCompletesAndNextCycleCanDeliver() throws Exception {
        manager.handlerData(List.of(legacyCluster), List.of(exporterNode), List.of(legacyConfig));
        installClient();
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenThrow(new IOException("fixture failure")).thenReturn(response);
        manager.collect();
        assertTrue(nextBatch().isEmpty());
        manager.collect();
        List<Object> samples = nextBatch().get(ClusterId.class);
        assertNotNull(samples);
        assertEquals(1, samples.size());
    }

    @SuppressWarnings("unchecked")
    private CollectExporter installClient() throws Exception {
        Map<String, Collect> active = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        CollectExporter exporter = (CollectExporter) active.get("CollectMetadata-23");
        assertNotNull(exporter);
        FieldUtils.writeField(exporter, "client", client, true);
        return exporter;
    }

    private Map<Class<?>, List<Object>> nextBatch() throws InterruptedException {
        Map<Class<?>, List<Object>> result = batches.poll(5, TimeUnit.SECONDS);
        assertNotNull(result, "scheduled collection did not complete");
        return result;
    }

    private static ClusterMetadata cluster(long id, ClusterType type) {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(id);
        cluster.setName("cluster-" + id);
        cluster.setOrganizationId(4L);
        cluster.setClusterType(type);
        cluster.setStatus(1L);
        return cluster;
    }

    private static CollectMetadata config(long id, long clusterId, ClusterType type, CollectType collectType) {
        CollectMetadata configuration = new CollectMetadata();
        configuration.setId(id);
        configuration.setClusterId(clusterId);
        configuration.setClusterType(type);
        configuration.setCollectType(collectType);
        configuration.setStatus(1L);
        configuration.setEnable(true);
        return configuration;
    }
}
