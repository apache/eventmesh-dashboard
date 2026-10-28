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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

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
        dataSyncHandlerWrapper.restoreDataList.forEach((restoreData -> {
            restoreData.getDataMap().forEach((key, value) -> {
                classCollectListMap.computeIfAbsent(key, k -> new CollectList<>()).addAll(value);
            });
            restoreData.restore();
        }));
        // TODO 这是是否有性能问题
        classCollectListMap.values().forEach(list -> list.forEach(value -> paddingService.padding((ClusterId) value)));
        reportEngine.batchInsertByClass(classCollectListMap);
    }

    static class CollectList<T> extends AbstractList<T> {

        private final List<Collection<? extends T>> datas = new ArrayList<>();

        @Override
        public void forEach(Consumer<? super T> action) {
            this.datas.forEach((data) -> data.forEach(action));
        }

        @Override
        public T get(int index) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<T> iterator() {
            return new CollectIterator<>((Iterator<List<T>>) this.datas);
        }

        @Override
        public int size() {
            return this.datas.size();
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends T> data) {
            return this.datas.add(data);
        }

    }

    static class CollectIterator<T> implements Iterator<T> {

        private final Iterator<List<T>> listIterator;

        private Iterator<T> iterator;

        public CollectIterator(List<List<T>> datas) {
            this(datas.iterator());
        }

        public CollectIterator(Iterator<List<T>> listIterator) {
            this.listIterator = listIterator;
            if (this.listIterator.hasNext()) {
                this.iterator = listIterator.next().iterator();
            }
        }

        @Override
        public boolean hasNext() {
            if (Objects.isNull(this.iterator)) {
                return false;
            }
            if (this.iterator.hasNext()) {
                return true;
            }
            if (listIterator.hasNext()) {
                iterator = listIterator.next().iterator();
                return true;
            }
            return false;
        }

        @Override
        public T next() {
            return iterator.next();
        }

    }

    public class DataSyncHandlerWrapper {


        final List<RestoreData> restoreDataList;
        private final AtomicInteger atomicInteger;
        private final LocalDateTime startTime = LocalDateTime.now();
        private int index = 0;
        private volatile boolean timeout = false;

        public DataSyncHandlerWrapper(int count) {
            this.atomicInteger = new AtomicInteger(count);
            this.restoreDataList = new ArrayList<>(count + count / 2);
        }

        public void sync(RestoreData restoreData) {
            if (this.timeout) {
                log.error("DataSyncHandlerWrapper timeout, cluster id is {} cluster type is {}",
                    restoreData.getCollectMetadata().getClusterId(), restoreData.getCollectMetadata().getClusterType());
            }
            // TODO 我赌，时间差，可见性到位了
            restoreDataList.add(restoreData.getIndex(), restoreData);
            if (this.atomicInteger.decrementAndGet() == 0) {
                LocalDateTime now = LocalDateTime.now();
                log.info("sync finished , count is {} start time is {}  end time is {} , time consuming is {} ", restoreDataList.size(), startTime,
                    now,
                    ChronoUnit.MILLENNIA.between(startTime, now));
                padding(this);
            }
        }

        public void shutdown() {
            if (this.atomicInteger.get() > 0) {
                log.error(" There are still tasks that have not been executed yet , num is {}", atomicInteger.get());
                this.timeout = true;
            }
        }

        public int getIndex() {
            return this.index++;
        }

    }
}
