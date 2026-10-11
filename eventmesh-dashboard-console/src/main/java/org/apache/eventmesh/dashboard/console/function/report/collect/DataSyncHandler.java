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
                // MyBatis 的 foreach 通过 iterator 遍历样本；原 CollectList 的迭代器强转会失败，size 也只返回批次数。
                // 使用普通列表合并样本引用，保持 ReportEngine 的 List 契约，避免修改 SQL 生成和入库链路。
                classCollectListMap.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value);
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

        /**
         * 汇总同一轮采集结果，并由最后完成的任务触发入库。
         * AtomicInteger 的递减具有 volatile 读写语义，可使各任务递减前的写入对最后完成的线程可见。
         * 同步用于保护共享 ArrayList 的并发修改；可见性保障不能避免列表写入冲突，采集耗时也不保证互斥。
         *
         * @param restoreData 当前任务的采集结果
         */
        public synchronized void sync(RestoreData restoreData) {
            if (this.timeout) {
                log.error("DataSyncHandlerWrapper timeout, cluster id is {} cluster type is {}",
                    restoreData.getCollectMetadata().getClusterId(), restoreData.getCollectMetadata().getClusterType());
            }
            // 任务可能乱序完成，按任务编号向尚未填充的列表插入会越界；预分配容量不会创建元素。
            // 后续按模型类型及样本自身的集群、节点等维度处理结果，因此可以按完成顺序追加。
            restoreDataList.add(restoreData);
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
