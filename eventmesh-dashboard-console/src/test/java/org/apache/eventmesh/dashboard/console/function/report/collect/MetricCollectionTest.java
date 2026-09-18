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
import org.apache.eventmesh.dashboard.console.function.report.collect.RocketMQDataSyncHandler.RocketMQDataSyncHandlerWrapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MetricCollectionTest {
    @Test
    void outOfOrderFailureAndDuplicateCompletionWriteSuccessfulRowsOnce() throws Exception {
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        try (RocketMQDataSyncHandler handler = new RocketMQDataSyncHandler()) {
            handler.setReportEngine(engine);
            RocketMQDataSyncHandlerWrapper batch = handler.getRocketMQDataSyncHandlerWrapper(3);
            RestoreData second = new RestoreData();
            second.setData("kept");
            batch.complete(2, second, null);
            batch.complete(2, second, null);
            batch.complete(0, null, new IllegalStateException("expected failure"));
            Assertions.assertFalse(batch.completion().isDone());
            batch.complete(1, new RestoreData(), null);
            batch.completion().get(2, TimeUnit.SECONDS);
            ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(engine).batchInsertByClass(captured.capture());
            Assertions.assertEquals(List.of("kept"), captured.getValue().get(String.class));
            Assertions.assertEquals(List.of("kept"), second.getDataMap().get(String.class));
        }
    }

    @Test
    void writeRetriesOnceThenReportsFailure() throws Exception {
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        Mockito.doThrow(new IllegalStateException("IoTDB unavailable")).when(engine).batchInsertByClass(Mockito.anyMap());
        try (RocketMQDataSyncHandler handler = new RocketMQDataSyncHandler()) {
            handler.setReportEngine(engine);
            RocketMQDataSyncHandlerWrapper batch = handler.getRocketMQDataSyncHandlerWrapper(1);
            RestoreData data = new RestoreData();
            data.setData("sample");
            batch.complete(0, data, null);
            Assertions.assertThrows(ExecutionException.class, () -> batch.completion().get(2, TimeUnit.SECONDS));
            Mockito.verify(engine, Mockito.times(2)).batchInsertByClass(Mockito.anyMap());
        }
    }

    @Test
    void shutdownWriterDoesNotLeavePendingFuture() {
        RocketMQDataSyncHandler handler = new RocketMQDataSyncHandler();
        handler.close();
        RocketMQDataSyncHandlerWrapper batch = handler.getRocketMQDataSyncHandlerWrapper(1);
        batch.complete(0, new RestoreData(), null);
        Assertions.assertTrue(batch.completion().isCompletedExceptionally());
    }

    @Test
    void emptyAndTimedOutBatchesTerminate() throws Exception {
        try (RocketMQDataSyncHandler handler = new RocketMQDataSyncHandler()) {
            Assertions.assertTrue(handler.getRocketMQDataSyncHandlerWrapper(0).completion().isDone());
            RocketMQDataSyncHandlerWrapper batch = handler.getRocketMQDataSyncHandlerWrapper(2);
            batch.shutdown();
            batch.completion().get(2, TimeUnit.SECONDS);
            batch.complete(1, new RestoreData(), null);
            Assertions.assertTrue(batch.completion().isDone());
        }
    }
    @Test
    void springSchedulesAllFiveDirections() throws Exception {
        RocketMQCollectManage manager = Mockito.mock(RocketMQCollectManage.class);
        java.util.concurrent.CountDownLatch called = new java.util.concurrent.CountDownLatch(5);
        Mockito.when(manager.collectAsync(Mockito.any())).thenAnswer(invocation -> {
            called.countDown();
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context =
            new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            Map<String, Object> cron = new java.util.HashMap<>();
            for (String name : List.of("collectTopicOffset", "collectConsumerOffset", "collectBrokerStatsTopic",
                "collectBrokerStats", "collectBrokerRuntimeStats")) {
                cron.put("task." + name + ".cron", "* * * * * *");
            }
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test-cron", cron));
            context.register(Scheduling.class);
            context.getBeanFactory().registerSingleton("rocketMQMetricsManage", manager);
            context.register(RocketMQMetricsCollectTask.class);
            context.refresh();
            Assertions.assertTrue(called.await(3, TimeUnit.SECONDS));
            for (org.apache.eventmesh.dashboard.common.enums.MetricFamily family :
                org.apache.eventmesh.dashboard.common.enums.MetricFamily.values()) {
                Mockito.verify(manager, Mockito.atLeastOnce()).collectAsync(family);
            }
        }
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.scheduling.annotation.EnableScheduling
    static class Scheduling {
    }
}
