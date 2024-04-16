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

package org.apache.eventmesh.dashboard.console.function.metadata;

import org.apache.eventmesh.dashboard.console.function.metadata.MetadataServiceWrapper.SingleMetadataServiceWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataServiceWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.ConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.validation.constraints.NotNull;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * MetadataManager is a manager for metadata service, it will sync the data between cluster service and database. database should be empty when this
 * manager booted
 */
@Slf4j
public class MetadataManager {

    @Setter
    private Boolean toDbSync = true;

    private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(2);

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "metadata-manager-" + counter.incrementAndGet());
            }
        });
    /**
     * singleton id for service wrapper map, even if the cache is not on, the id should be increased.
     */
    private static final AtomicLong staticServiceId = new AtomicLong(0);

    private static final ConcurrentHashMap<Long, Boolean> firstRunToDb = new ConcurrentHashMap<>();

    private final Map<Long, MetadataServiceWrapper> metaDataServiceWrapperMap = new ConcurrentHashMap<>();

    private final Map<Long, List<Object>> cacheData = new ConcurrentHashMap<>();


    public void init(Integer initialDelay, Integer period) {

        scheduledExecutorService.scheduleAtFixedRate(() -> MetadataManager.this.run(toDbSync, true), initialDelay, period, TimeUnit.SECONDS);
    }


    /**
     * entrance of a sync scheduled task
     *
     * @param metaDataServiceWrapper
     */
    public void addMetadataService(MetadataServiceWrapper metaDataServiceWrapper) {
        Long cacheId = staticServiceId.incrementAndGet();
        metaDataServiceWrapper.setCacheId(cacheId);
        metaDataServiceWrapperMap.put(cacheId, metaDataServiceWrapper);
    }

    public void run() {
        metaDataServiceWrapperMap.forEach(this::handlers);
    }

    public void run(Boolean toDbOn, Boolean toServiceOn) {
        try {
            metaDataServiceWrapperMap.forEach((cacheId, metaDataServiceWrapper) -> handlers(cacheId, metaDataServiceWrapper, toDbOn, toServiceOn));
        } catch (Exception e) {
            log.error("metadata manager run error", e);
        }
    }

    public void handlers(Long cacheId, MetadataServiceWrapper metaDataServiceWrapper, Boolean toDbOn, Boolean toServiceOn) {
        this.threadPoolExecutor.execute(() -> {
            try {
                if (toDbOn) {
                    this.handler(cacheId, metaDataServiceWrapper.getDbToService(), true);
                }
                if (toServiceOn) {
                    this.handler(cacheId, metaDataServiceWrapper.getServiceToDb(), false);
                }
            } catch (Throwable e) {
                log.error("metadata manager handler error", e);
            }
        });
    }

    public void handlers(Long cacheId, MetadataServiceWrapper metaDataServiceWrapper) {
        handlers(cacheId, metaDataServiceWrapper, true, true);
    }

    public void handler(Long cacheID, SingleMetadataServiceWrapper singleMetadataServiceWrapper, boolean isDbToService) {
        if (singleMetadataServiceWrapper == null) {
            return;
        }

        try {
            List<Object> newObjectList = (List<Object>) singleMetadataServiceWrapper.getSyncService().getData();
            if (newObjectList.isEmpty()) {
                return;
            }

            //if cache is false, we don't need to compare the data
            // full volume updates
            if (!singleMetadataServiceWrapper.getCache()) {
                singleMetadataServiceWrapper.getHandler()
                    .replaceMetadata(newObjectList);
                return;
            }

            List<Object> cacheDataList = cacheData.get(cacheID);
            //update old cache
            cacheData.put(cacheID, newObjectList);

            Converter<?, ?> converter = ConverterFactory.createConverter(newObjectList.get(0).getClass());

            Map<String, Object> newObjectMap = converter.getUniqueKeyMapFromObject(newObjectList);
            Map<String, Object> oldObjectMap = converter.getUniqueKeyMapFromObject(cacheDataList);

            //these three List are in target type
            List<Object> toUpdate = new ArrayList<>();
            List<Object> toDelete = new ArrayList<>();
            List<Object> toInsert;

            for (Entry<String, Object> entry : oldObjectMap.entrySet()) {

                Object serviceObject = newObjectMap.remove(entry.getKey());
                //if new Data don't have a key in oldMap,
                if (serviceObject == null) {
                    toDelete.add(entry.getValue());
                } else {
                    //primary id, creat time and update time should not be compared
                    //if not equal, we need to update fields except unique key(they are equal)
                    //cause entry is from the oldMap, it should contain the primary key.
                    if (!serviceObject.equals(entry.getValue())) {
                        toUpdate.add(entry.getValue());
                    }
                }
            }

            toInsert = new ArrayList<>(newObjectMap.values());

            //if target is db, we use handler to provide transaction
            if (!isDbToService) {
                firstRunToDb.putIfAbsent(cacheID, false);
                singleMetadataServiceWrapper.getHandler().handleAllObject(toInsert, toUpdate, toDelete);
                //if target is eventmesh, we just use that 3 basic method
            } else {
                toInsert.forEach(singleMetadataServiceWrapper.getHandler()::addMetadataObject);
                toUpdate.forEach(singleMetadataServiceWrapper.getHandler()::updateMetadataObject);
                toDelete.forEach(singleMetadataServiceWrapper.getHandler()::deleteMetadataObject);
            }
        } catch (Throwable e) {
            log.error("metadata manager handler error", e);
        }
    }

    public void setUpSyncMetadataManager(SyncDataServiceWrapper syncDataServiceWrapper, MetadataHandlerWrapper metadataHandlerWrapper) {
        MetadataServiceWrapper metadataServiceWrapper = new MetadataServiceWrapper();
        SingleMetadataServiceWrapper singleMetadataServiceWrapper = SingleMetadataServiceWrapper.builder()
            .syncService(syncDataServiceWrapper.getRuntimeSyncFromClusterService())
            .handler(metadataHandlerWrapper.getRuntimeMetadataHandlerToDb()).build();
        metadataServiceWrapper.setServiceToDb(singleMetadataServiceWrapper);
        this.addMetadataService(metadataServiceWrapper);
    }

    //TODO if database is modified by other service, we need to update the cache
}
