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

import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerConnectionNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataSyncHandlerTest {

    @Test
    public void initializationUsesConfiguredEngine() throws Exception {
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        ReportHandlerManage reports = new ReportHandlerManage();
        reports.setReportEngine(engine);
        CollectManage manager = new CollectManage();
        manager.setReportHandlerManage(reports);
        manager.init();
        var field = CollectManage.class.getDeclaredField("dataSyncHandler");
        field.setAccessible(true);
        DataSyncHandler handler = (DataSyncHandler) field.get(manager);
        // 空结果也经过真实批次交付，验证初始化接线，而不是测试中手动注入引擎。
        handler.getDataSyncHandlerWrapper(1).sync(data(0));
        Mockito.verify(engine).batchInsertByClass(Map.of());
    }

    @Test
    public void reverseCompletionDeliversFlatIterableRowsOnce() {
        DataSyncHandler handler = new DataSyncHandler();
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        PaddingService padding = Mockito.mock(PaddingService.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(padding);
        var wrapper = handler.getDataSyncHandlerWrapper(3);
        RestoreData first = data(0, 10, 11);
        RestoreData empty = data(1);
        RestoreData last = data(2, 20);
        List<Long> observed = new ArrayList<>();
        Mockito.doAnswer(call -> {
            Map<Class<?>, List<Object>> batch = call.getArgument(0);
            List<Object> rows = batch.get(RocketmqConsumerConnectionNumber.class);
            Assertions.assertEquals(3, rows.size());
            Assertions.assertNotNull(rows.get(2));
            for (Object row : rows) {
                observed.add(((RocketmqConsumerConnectionNumber) row).getValueConnectionCount());
            }
            Assertions.assertFalse(first.getDataMap().isEmpty(), "Do not recycle before engine consumes the batch");
            return null;
        }).when(engine).batchInsertByClass(Mockito.anyMap());
        wrapper.sync(last);
        wrapper.sync(empty);
        Mockito.verifyNoInteractions(engine);
        wrapper.sync(first);
        Assertions.assertEquals(List.of(20L, 10L, 11L), observed);
        Mockito.verify(engine).batchInsertByClass(Mockito.anyMap());
        Mockito.verify(padding, Mockito.times(3)).padding(Mockito.any());
        Assertions.assertTrue(first.getDataMap().isEmpty());
        Assertions.assertTrue(last.getDataMap().isEmpty());
        Assertions.assertTrue(wrapper.restoreDataList.isEmpty());
    }

    @Test
    public void concurrentCompletionAllocatesUniqueIndicesAndLosesNoRows() throws Exception {
        DataSyncHandler handler = new DataSyncHandler();
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(Mockito.mock(PaddingService.class));
        var wrapper = handler.getDataSyncHandlerWrapper(200);
        Set<Integer> indices = ConcurrentHashMap.newKeySet();
        Set<Long> observed = ConcurrentHashMap.newKeySet();
        Mockito.doAnswer(call -> {
            Map<Class<?>, List<Object>> batch = call.getArgument(0);
            List<Object> rows = batch.get(RocketmqConsumerConnectionNumber.class);
            Assertions.assertEquals(200, rows.size());
            for (Object row : rows) {
                observed.add(((RocketmqConsumerConnectionNumber) row).getValueConnectionCount());
            }
            return null;
        }).when(engine).batchInsertByClass(Mockito.anyMap());
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> jobs = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                final int value = i;
                jobs.add(executor.submit(() -> {
                    int index = wrapper.getIndex();
                    Assertions.assertTrue(indices.add(index));
                    wrapper.sync(data(index, value));
                }));
            }
            for (Future<?> job : jobs) {
                job.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        Assertions.assertEquals(200, indices.size());
        Assertions.assertEquals(200, observed.size());
        Mockito.verify(engine).batchInsertByClass(Mockito.anyMap());
    }

    @Test
    public void failedWriteStillReleasesContainers() {
        DataSyncHandler handler = new DataSyncHandler();
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(Mockito.mock(PaddingService.class));
        Mockito.doThrow(new IllegalStateException("write failed")).when(engine).batchInsertByClass(Mockito.anyMap());
        var wrapper = handler.getDataSyncHandlerWrapper(1);
        RestoreData data = data(0, 1);
        Assertions.assertThrows(IllegalStateException.class, () -> wrapper.sync(data));
        Assertions.assertTrue(data.getDataMap().isEmpty());
        Assertions.assertTrue(wrapper.restoreDataList.isEmpty());
    }

    private RestoreData data(int index, long... values) {
        RestoreData data = new RestoreData();
        data.setIndex(index);
        data.setAbstractCollect(Mockito.mock(AbstractCollect.class));
        for (long value : values) {
            RocketmqConsumerConnectionNumber row = new RocketmqConsumerConnectionNumber();
            row.setValueConnectionCount(value);
            data.setData(row);
        }
        return data;
    }
}
