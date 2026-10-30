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
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Setter
@Slf4j
public class DataSyncHandler {

    private PaddingService paddingService;

    private ReportEngine reportEngine;


    public DataSyncHandlerWrapper getDataSyncHandlerWrapper(int count) {
        return new DataSyncHandlerWrapper(count);
    }

    /**
     *  4000个 rocketmq broker 节点
     */
    public void padding(DataSyncHandlerWrapper dataSyncHandlerWrapper) {
        Map<Class<?>, List<Object>> classCollectListMap = new HashMap<>();
        try {
            // 原包装列表不能正确迭代且可能提前归还缓冲，复制样本并在写入完成后归还。
            dataSyncHandlerWrapper.restoreDataList.forEach(restoreData ->
                restoreData.getDataMap().forEach((key, value) ->
                    classCollectListMap.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value)));
            classCollectListMap.values().forEach(list -> list.forEach(value -> paddingService.padding((ClusterId) value)));
            this.write(reportEngine, classCollectListMap);
        } finally {
            dataSyncHandlerWrapper.restoreDataList.forEach(RestoreData::restore);
        }
    }

    /** 不同存储写入方式复用同一份批次整理与缓冲归还流程。 */
    protected void write(ReportEngine engine, Map<Class<?>, List<Object>> samples) {
        engine.batchInsertByClass(samples);
    }

    public class DataSyncHandlerWrapper {


        final List<RestoreData> restoreDataList;
        private final AtomicInteger atomicInteger;
        private final boolean[] completed;
        private final LocalDateTime startTime = LocalDateTime.now();
        private final AtomicInteger index = new AtomicInteger();
        private volatile boolean timeout = false;

        public DataSyncHandlerWrapper(int count) {
            this.atomicInteger = new AtomicInteger(count);
            this.completed = new boolean[count];
            this.restoreDataList = new ArrayList<>(count + count / 2);
        }

        public void sync(RestoreData restoreData) {
            if (this.timeout) {
                log.error("DataSyncHandlerWrapper timeout, cluster id is {} cluster type is {}",
                    restoreData.getCollectMetadata().getClusterId(), restoreData.getCollectMetadata().getClusterType());
            }
            finish(restoreData.getIndex(), restoreData);
        }

        /** 采集任务异常结束时完成对应批次位置，不覆盖已经交付的结果。 */
        public void complete(int index) {
            finish(index, null);
        }

        private void finish(int index, RestoreData restoreData) {
            boolean finished;
            // 任务可能并发或乱序结束，按索引去重，并只在锁内登记结果。
            synchronized (restoreDataList) {
                Objects.checkIndex(index, completed.length);
                if (completed[index]) {
                    return;
                }
                completed[index] = true;
                if (restoreData != null) {
                    restoreDataList.add(restoreData);
                }
                finished = this.atomicInteger.decrementAndGet() == 0;
            }
            if (finished) {
                LocalDateTime now = LocalDateTime.now();
                log.info("sync finished , count is {} start time is {}  end time is {} , time consuming is {} ", restoreDataList.size(), startTime,
                    now,
                    ChronoUnit.MILLENNIA.between(startTime, now));
                try {
                    padding(this);
                } finally {
                    restoreDataList.clear();
                }
            }
        }

        public void shutdown() {
            if (this.atomicInteger.get() > 0) {
                log.error(" There are still tasks that have not been executed yet , num is {}", atomicInteger.get());
                this.timeout = true;
            }
        }

        public int getIndex() {
            // 索引在多个采集线程中分配，必须保证每个任务的位置唯一。
            return this.index.getAndIncrement();
        }

    }
}
