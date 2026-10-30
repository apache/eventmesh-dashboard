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
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId.ClusterLongValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class KafkaBatchOwnershipTest {

    @Test
    public void pendingBatchDoesNotShareReturnedBufferWithNextBatch() {
        List<List<Long>> writes = new ArrayList<>();
        DataSyncHandler handler = handler(writes);
        SequenceCollector collector = collector(new SequenceCollector());
        var warm = handler.getDataSyncHandlerWrapper(1);
        collector.collect(warm.getIndex(), warm);
        var pending = handler.getDataSyncHandlerWrapper(2);
        collector.collect(pending.getIndex(), pending);
        var next = handler.getDataSyncHandlerWrapper(1);
        collector.collect(next.getIndex(), next);
        RestoreData other = new RestoreData();
        other.setAbstractCollect(collector(new SequenceCollector()));
        other.setIndex(pending.getIndex());
        pending.sync(other);
        Assertions.assertEquals(List.of(List.of(1L), List.of(3L), List.of(2L)), writes);
        Assertions.assertEquals(3, collector.returned.size());
        Assertions.assertNotSame(collector.returned.get(0), collector.returned.get(1));
        Assertions.assertSame(collector.returned.get(0), collector.returned.get(2));
    }

    @Test
    public void sequentialBatchesCanReuseBuffers() {
        List<List<Long>> writes = new ArrayList<>();
        DataSyncHandler handler = handler(writes);
        SequenceCollector collector = collector(new SequenceCollector());
        for (int i = 0; i < 3; i++) {
            var wrapper = handler.getDataSyncHandlerWrapper(1);
            collector.collect(wrapper.getIndex(), wrapper);
        }
        Assertions.assertEquals(List.of(List.of(1L), List.of(2L), List.of(3L)), writes);
        Assertions.assertEquals(3, collector.returned.size());
        Assertions.assertSame(collector.returned.get(0), collector.returned.get(1));
        Assertions.assertSame(collector.returned.get(1), collector.returned.get(2));
    }

    @Test
    public void failedCollectionReturnsBufferAndCompletesItsBatchOnce() {
        List<List<Long>> writes = new ArrayList<>();
        DataSyncHandler handler = handler(writes);
        SequenceCollector collector = collector(new SequenceCollector());
        var warm = handler.getDataSyncHandlerWrapper(1);
        collector.collect(warm.getIndex(), warm);
        collector.fail = true;
        var failed = handler.getDataSyncHandlerWrapper(1);
        int index = failed.getIndex();
        Assertions.assertThrows(IllegalStateException.class, () -> collector.collect(index, failed));
        failed.complete(index);
        failed.complete(index);
        collector.fail = false;
        var next = handler.getDataSyncHandlerWrapper(1);
        collector.collect(next.getIndex(), next);
        Assertions.assertEquals(List.of(List.of(1L), List.of(), List.of(3L)), writes);
        Assertions.assertEquals(3, collector.returned.size());
        Assertions.assertSame(collector.returned.get(0), collector.returned.get(1));
        Assertions.assertSame(collector.returned.get(1), collector.returned.get(2));
    }

    @Test
    public void failedWriteReturnsTheBufferWithoutReplayingItsSamples() {
        List<List<Long>> writes = new ArrayList<>();
        DataSyncHandler handler = new DataSyncHandler();
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(Mockito.mock(PaddingService.class));
        AtomicInteger calls = new AtomicInteger();
        Mockito.doAnswer(call -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("test write failure");
            }
            Map<Class<?>, List<Object>> batch = call.getArgument(0);
            writes.add(batch.getOrDefault(ClusterLongValue.class, List.of()).stream()
                .map(row -> ((ClusterLongValue) row).getValue()).toList());
            return null;
        }).when(engine).batchInsertByClass(Mockito.anyMap());
        SequenceCollector collector = collector(new SequenceCollector());
        var failed = handler.getDataSyncHandlerWrapper(1);
        int failedIndex = failed.getIndex();
        Assertions.assertThrows(IllegalStateException.class, () -> collector.collect(failedIndex, failed));
        failed.complete(failedIndex);
        Mockito.verify(engine, Mockito.times(1)).batchInsertByClass(Mockito.anyMap());
        var next = handler.getDataSyncHandlerWrapper(1);
        collector.collect(next.getIndex(), next);
        Mockito.verify(engine, Mockito.times(2)).batchInsertByClass(Mockito.anyMap());
        Assertions.assertEquals(List.of(List.of(2L)), writes);
        Assertions.assertEquals(2, collector.returned.size());
        Assertions.assertSame(collector.returned.get(0), collector.returned.get(1));
    }

    @Test
    public void overlappingCallCompletesWithoutBlockingOtherCollectors() throws Exception {
        List<List<Long>> writes = Collections.synchronizedList(new ArrayList<>());
        DataSyncHandler handler = handler(writes);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = executor();
        AbstractCollect slow = collector(new AbstractCollect() {
            @Override
            protected void doCollect() {
                sample(this, 1L);
                started.countDown();
                await(release);
            }
        });
        AbstractCollect healthy = collector(new AbstractCollect() {
            @Override
            protected void doCollect() {
                sample(this, 2L);
            }
        });
        try {
            var first = handler.getDataSyncHandlerWrapper(1);
            var overlap = handler.getDataSyncHandlerWrapper(1);
            var other = handler.getDataSyncHandlerWrapper(1);
            Future<?> firstJob = executor.submit(() -> slow.collect(first.getIndex(), first));
            Assertions.assertTrue(started.await(3, TimeUnit.SECONDS));
            Future<?> overlapJob = executor.submit(() -> slow.collect(overlap.getIndex(), overlap));
            overlapJob.get(1, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(List.of()), writes);
            Future<?> healthyJob = executor.submit(() -> healthy.collect(other.getIndex(), other));
            healthyJob.get(1, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(List.of(), List.of(2L)), writes);
            release.countDown();
            firstJob.get(2, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(List.of(), List.of(2L), List.of(1L)), writes);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5),
            new ThreadFactoryBuilder().setNameFormat("overlap-test-%d").build(), new ThreadPoolExecutor.AbortPolicy());
    }

    private DataSyncHandler handler(List<List<Long>> writes) {
        DataSyncHandler handler = new DataSyncHandler();
        ReportEngine engine = Mockito.mock(ReportEngine.class);
        handler.setReportEngine(engine);
        handler.setPaddingService(Mockito.mock(PaddingService.class));
        Mockito.doAnswer(call -> {
            Map<Class<?>, List<Object>> batch = call.getArgument(0);
            writes.add(batch.getOrDefault(ClusterLongValue.class, List.of()).stream()
                .map(row -> ((ClusterLongValue) row).getValue()).toList());
            return null;
        }).when(engine).batchInsertByClass(Mockito.anyMap());
        return handler;
    }

    private <T extends AbstractCollect> T collector(T collector) {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(11L);
        cluster.setOrganizationId(1L);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        CollectMetadata configuration = new CollectMetadata();
        configuration.setClusterId(11L);
        collector.setClusterMetadata(cluster);
        collector.setCollectMetadata(configuration);
        return collector;
    }

    private static void sample(AbstractCollect collector, long value) {
        ClusterLongValue sample = new ClusterLongValue();
        sample.setValue(value);
        collector.setData(sample);
    }

    private static class SequenceCollector extends AbstractCollect {
        private long sequence;
        private boolean fail;
        private final List<RestoreData> returned = new ArrayList<>();

        @Override
        public synchronized void restore(RestoreData data) {
            returned.add(data);
            super.restore(data);
        }

        @Override
        protected void doCollect() {
            sample(this, ++sequence);
            if (fail) {
                throw new IllegalStateException("test collection failure");
            }
        }
    }
}
