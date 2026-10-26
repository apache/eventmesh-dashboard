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

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KafkaDataSyncHandlerTest {

    @Test
    public void outOfOrderResultsAreDeliveredAsAnIterableSampleList() {
        ReportEngine engine = mock(ReportEngine.class);
        AtomicReference<Map<Class<?>, List<Object>>> written = new AtomicReference<>();
        doAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        DataSyncHandler handler = new DataSyncHandler();
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(2);
        wrapper.sync(result(1, 2L));
        wrapper.sync(result(0, 1L));
        List<Object> samples = written.get().get(KafkaClusterMetrics.class);
        assertEquals(2, samples.size());
        assertEquals(2, samples.stream().count());
        assertEquals(Long.valueOf(2), ((KafkaClusterMetrics) samples.iterator().next()).getValueBrokerCount());
    }

    @Test
    public void emptyRoundsCompleteThroughTheExistingResultContract() {
        ReportEngine engine = mock(ReportEngine.class);
        AtomicReference<Map<Class<?>, List<Object>>> written = new AtomicReference<>();
        doAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        DataSyncHandler handler = new DataSyncHandler();
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(2);
        RestoreData empty = result(1, 2L);
        empty.getDataMap().clear();
        wrapper.sync(empty);
        verify(engine, never()).batchInsertByClass(anyMap());
        wrapper.sync(result(0, 2L));
        verify(engine, times(1)).batchInsertByClass(anyMap());
        List<Object> samples = written.get().get(KafkaClusterMetrics.class);
        assertEquals(1, samples.size());
        assertEquals(Long.valueOf(2), ((KafkaClusterMetrics) samples.get(0)).getValueBrokerCount());
    }

    @Test
    public void concurrentCollectorsDeliverEverySampleExactlyOnce() {
        ReportEngine engine = mock(ReportEngine.class);
        AtomicReference<Map<Class<?>, List<Object>>> written = new AtomicReference<>();
        doAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        DataSyncHandler handler = new DataSyncHandler();
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(64);
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            int index = i;
            tasks.add(CompletableFuture.runAsync(() -> wrapper.sync(result(index, index))));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        verify(engine, times(1)).batchInsertByClass(anyMap());
        List<Object> samples = written.get().get(KafkaClusterMetrics.class);
        assertEquals(64, samples.size());
        assertEquals(64, samples.stream().map(value -> ((KafkaClusterMetrics) value).getValueBrokerCount()).distinct().count());
    }

    @Test
    public void returnedBufferIsNotReusedAgainWhileItsNextBatchIsPending() {
        AbstractCollect collector = new AbstractCollect() {
            private long samples;

            @Override
            protected void doCollect() {
                KafkaClusterMetrics sample = new KafkaClusterMetrics();
                sample.setValueBrokerCount(++samples);
                setData(sample);
            }
        };
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(1L);
        cluster.setOrganizationId(1L);
        collector.setClusterMetadata(cluster);
        collector.setCollectMetadata(new CollectMetadata());
        List<RestoreData> results = new ArrayList<>();
        DataSyncHandler.DataSyncHandlerWrapper wrapper = mock(DataSyncHandler.DataSyncHandlerWrapper.class);
        doAnswer(invocation -> {
            RestoreData result = invocation.getArgument(0);
            results.add(result);
            if (results.size() == 1) {
                // 第一轮立即归还，第二轮延迟交付，第三轮不能再次取出第二轮仍占用的缓冲。
                result.restore();
            }
            return null;
        }).when(wrapper).sync(org.mockito.ArgumentMatchers.any(RestoreData.class));

        collector.collect(0, wrapper);
        collector.collect(1, wrapper);
        collector.collect(2, wrapper);

        assertNotSame(results.get(1), results.get(2));
        assertEquals(1, results.get(1).getDataMap().get(KafkaClusterMetrics.class).size());
        assertEquals(Long.valueOf(2), ((KafkaClusterMetrics) results.get(1).getDataMap().get(KafkaClusterMetrics.class).get(0))
            .getValueBrokerCount());
    }

    private RestoreData result(int index, long count) {
        RestoreData result = new RestoreData();
        result.setIndex(index);
        result.setAbstractCollect(mock(AbstractCollect.class));
        KafkaClusterMetrics sample = new KafkaClusterMetrics();
        sample.setValueBrokerCount(count);
        result.setData(sample);
        return result;
    }
}
