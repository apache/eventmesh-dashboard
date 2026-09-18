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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class RocketMQDataSyncHandler implements AutoCloseable {
    @Autowired
    @Qualifier("rocketMQMetricsEngine")
    private ReportEngine reportEngine;
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(32), task -> {
            Thread thread = new Thread(task, "report-iotdb-write");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());

    public RocketMQDataSyncHandlerWrapper getRocketMQDataSyncHandlerWrapper(int count) {
        return new RocketMQDataSyncHandlerWrapper(count);
    }

    @Override
    public void close() {
        // Drain admitted writes; rejection of new writes completes their futures exceptionally.
        writer.shutdown();
    }

    public class RocketMQDataSyncHandlerWrapper {
        private final RestoreData[] results;
        private final boolean[] completed;
        private final AtomicInteger nextIndex = new AtomicInteger();
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private int remaining;

        public RocketMQDataSyncHandlerWrapper(int count) {
            this.results = new RestoreData[count];
            this.completed = new boolean[count];
            this.remaining = count;
            if (count == 0) {
                completion.complete(null);
            }
        }

        public CompletableFuture<Void> completion() {
            return completion;
        }

        public void sync(RestoreData data) {
            complete(data.getIndex(), data, null);
        }

        public synchronized void complete(int index, RestoreData data, Throwable error) {
            if (completed[index]) {
                return;
            }
            completed[index] = true;
            results[index] = data;
            if (error != null) {
                log.warn("Collector {} failed", index, error);
            }
            if (--remaining != 0) {
                return;
            }
            Map<Class<?>, List<Object>> batches = new HashMap<>();
            for (RestoreData result : results) {
                if (result != null) {
                    result.getDataMap().forEach((type, rows) -> batches.computeIfAbsent(type, key -> new ArrayList<>()).addAll(rows));
                }
            }
            try {
                writer.execute(() -> {
                    try {
                        if (!batches.isEmpty()) {
                            // Same timestamps/tags make retry idempotent in IoTDB's table model.
                            try {
                                reportEngine.batchInsertByClass(batches);
                            } catch (RuntimeException first) {
                                reportEngine.batchInsertByClass(batches);
                            }
                        }
                        completion.complete(null);
                    } catch (Exception e) {
                        completion.completeExceptionally(e);
                    }
                });
            } catch (RuntimeException e) {
                completion.completeExceptionally(e);
            }
        }

        public synchronized void shutdown() {
            for (int i = 0; i < completed.length; i++) {
                if (!completed[i]) {
                    complete(i, null, new TimeoutException("Collection batch closed"));
                }
            }
        }

        public int getIndex() {
            return nextIndex.getAndIncrement();
        }
    }
}
