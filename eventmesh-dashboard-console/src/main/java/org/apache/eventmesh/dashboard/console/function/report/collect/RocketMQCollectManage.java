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

import org.apache.eventmesh.dashboard.common.enums.MetricFamily;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.RocketMQDataSyncHandler.RocketMQDataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.RocketMQCollect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

/** Opt-in asynchronous coordinator; existing CollectManage and metadata synchronization are untouched. */
public class RocketMQCollectManage implements AutoCloseable {
    private final Map<String, RocketMQCollect> collectors = new ConcurrentHashMap<>();
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    private final Set<CompletableFuture<Void>> pending = ConcurrentHashMap.newKeySet();
    @Autowired
    private RocketMQDataSyncHandler handler;
    private final ThreadPoolExecutor dispatcher = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(128), task -> {
            Thread thread = new Thread(task, "rocketmq-collect-dispatch");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    private volatile boolean closed;

    public void register(ClusterMetadata cluster, RuntimeMetadata runtime) {
        if (closed || cluster == null || runtime == null || cluster.getOrganizationId() == null
            || cluster.getId() == null || runtime.getId() == null || !cluster.getId().equals(runtime.getClusterId())) {
            throw new IllegalArgumentException("Collector requires matching cluster/runtime IDs and an organization ID");
        }
        for (MetricFamily family :
            MetricFamily.values()) {
            RocketMQCollect item = new RocketMQCollect();
            item.setClusterMetadata(cluster);
            item.setRuntimeMetadata(runtime);
            collectors.put(runtime.getUnique() + "/" + family, item);
        }
    }

    public void unregister(RuntimeMetadata runtime) {
        collectors.keySet().removeIf(key -> key.startsWith(runtime.getUnique() + "/"));
    }

    public CompletableFuture<Void> collectAsync(MetricFamily family) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Collector is closed"));
        }
        Map<String, RocketMQCollect> targets = new HashMap<>();
        collectors.forEach((key, value) -> {
            if (key.endsWith("/" + family) && active.add(key)) {
                targets.put(key, value);
            }
        });
        RocketMQDataSyncHandlerWrapper batch = handler.getRocketMQDataSyncHandlerWrapper(targets.size());
        pending.add(batch.completion());
        batch.completion().whenComplete((unused, error) -> {
            active.removeAll(targets.keySet());
            pending.remove(batch.completion());
        });
        targets.forEach((key, collector) -> {
            int index = batch.getIndex();
            try {
                dispatcher.execute(() -> {
                    try {
                        collector.collectAsync(CollectContext.now(family)).orTimeout(5, TimeUnit.SECONDS)
                            .whenComplete((data, error) -> batch.complete(index, data, error));
                    } catch (Exception e) {
                        batch.complete(index, null, e);
                    }
                });
            } catch (RuntimeException e) {
                batch.complete(index, null, e);
            }
        });
        return batch.completion();
    }

    @Override
    public void close() {
        closed = true;
        dispatcher.shutdown();
        try {
            CompletableFuture.allOf(pending.toArray(new CompletableFuture<?>[0])).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            pending.forEach(future -> future.completeExceptionally(e));
        } finally {
            handler.close();
        }
    }
}
