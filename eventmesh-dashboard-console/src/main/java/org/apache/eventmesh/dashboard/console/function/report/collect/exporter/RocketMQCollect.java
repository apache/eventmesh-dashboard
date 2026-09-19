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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import static org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQAsync.PROCESSING;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.apache.eventmesh.dashboard.common.annotation.ClusterTypeMark;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetricFamily;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.metrics.MetricSample;
import org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.CollectContext;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;
import org.apache.eventmesh.dashboard.console.function.report.collect.RocketMQMetricModels;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;
import org.apache.eventmesh.dashboard.console.mapstruct.report.RocketMQMetricMapper;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.MetricsRemotingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@ClusterTypeMark(clusterType = {ClusterType.STORAGE_ROCKETMQ_CLUSTER, ClusterType.STORAGE_ROCKETMQ_BROKER,
    ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE, ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT})
public class RocketMQCollect extends AbstractCollect {

    @Getter
    @Setter
    private ClusterMetadata clusterMetadata;

    @Getter
    @Setter
    private RuntimeMetadata runtimeMetadata;

    private final Map<String, Long> failures = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSuccess = new ConcurrentHashMap<>();

    private final AtomicReference<Round> activeRound = new AtomicReference<>();

    public CompletableFuture<RestoreData> collectAsync(CollectContext context) {
        Round round = new Round(context);
        if (!activeRound.compareAndSet(null, round)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A collection round is already active"));
        }
        try {
            this.doCollect();
        } catch (Exception e) {
            activeRound.compareAndSet(round, null);
            round.result.completeExceptionally(e);
        }
        return round.result;
    }

    @Override
    public void collect(int index,
        DataSyncHandlerWrapper wrapper) {
        CompletableFuture<RestoreData> result = CompletableFuture.completedFuture(new RestoreData());
        for (MetricFamily family :
            MetricFamily.values()) {
            result = result.thenComposeAsync(all -> this.collectAsync(CollectContext.now(family)).thenApply(data -> {
                data.getDataMap().values().forEach(rows -> rows.forEach(all::setData));
                return all;
            }), PROCESSING);
        }
        result.whenComplete((data, error) -> {
            if (error != null) {
                LoggerFactory.getLogger(RocketMQCollect.class).warn("Collection failed", error);
            }
            RestoreData completed = data == null ? new RestoreData() : data;
            completed.setIndex(index);
            completed.setAbstractCollect(this);
            wrapper.sync(completed);
        });
    }

    @Override
    protected void doCollect() {
        Round round = activeRound.get();
        CollectContext context = round.context;
        MetricsRemotingService service =
            Remoting2Manage.getInstance().createRemotingService(
                MetricsRemotingService.class, this.getRuntimeMetadata());
        // Copy dimension values before launching requests; later topology updates cannot relabel this round.
        Long organization = this.getClusterMetadata().getOrganizationId();
        Long cluster = this.getClusterMetadata().getId();
        Long runtime = this.getRuntimeMetadata().getId();
        String clusterName = this.getClusterMetadata().getName();
        String runtimeName = this.getRuntimeMetadata().getName();
        Map<String, CompletableFuture<List<MetricSample>>> families = service.collectAsync(context.getFamily(), context.getDeadlineMillis());
        List<CompletableFuture<List<RuntimeId>>> completed = new ArrayList<>();
        families.forEach((family, future) -> {
            long start = System.nanoTime();
            completed.add(future.orTimeout(Math.max(1, context.getDeadlineMillis() - System.currentTimeMillis()),
                MILLISECONDS).handleAsync((samples, error) -> {
                    List<MetricSample> observations = new ArrayList<>();
                    if (error == null) {
                        observations.addAll(samples);
                        lastSuccess.put(family, System.currentTimeMillis());
                    } else {
                        failures.merge(family, 1L, Long::sum);
                        LoggerFactory.getLogger(RocketMQCollect.class).warn("Collection failed runtime={} family={}", runtime, family, error);
                        if ("runtime".equals(family)) {
                            observations.add(new MetricSample(
                                "broker_reachable", "", "", "", "", 0));
                        }
                    }
                    observations.add(new MetricSample(
                        "collection_duration_ms", "", "", "", "", (System.nanoTime() - start) / 1_000_000.0));
                    observations.add(new MetricSample(
                        "collection_failures", "", "", "", "", failures.getOrDefault(family, 0L)));
                    if (lastSuccess.containsKey(family)) {
                        observations.add(new MetricSample(
                            "collection_last_success_ms", "", "", "", "", lastSuccess.get(family)));
                    }
                    List<RuntimeId> rows = RocketMQMetricModels.toReports(observations, family);
                    for (RuntimeId row : rows) {
                        RocketMQMetricMapper.INSTANCE.fillMetadata(context, organization, cluster, clusterName,
                            runtime, runtimeName, row);
                    }
                    return rows;
                }, PROCESSING));
        });
        CompletableFuture<RestoreData> result =
            CompletableFuture.completedFuture(new RestoreData());
        for (CompletableFuture<List<RuntimeId>> family : completed) {
            result = result.thenCombine(family, (data, rows) -> {
                rows.forEach(data::setData);
                return data;
            });
        }
        result.whenComplete((data, error) -> {
            activeRound.compareAndSet(round, null);
            if (error == null) {
                round.result.complete(data);
            } else {
                round.result.completeExceptionally(error);
            }
        });
    }

    private static final class Round {
        private final CollectContext context;
        private final CompletableFuture<RestoreData> result = new CompletableFuture<>();

        private Round(CollectContext context) {
            this.context = context;
        }
    }
}
