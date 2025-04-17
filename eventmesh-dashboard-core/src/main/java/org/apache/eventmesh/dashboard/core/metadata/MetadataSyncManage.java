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


package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动应该进行差异化对比。进行差异化增量，差异化删除 表 -> increment-> remote -> function -> data -> conver -> request
 * <p>
 * 怎么把 SyncMetadataManage 合进来
 */
@Slf4j
public class MetadataSyncManage {


    private final ThreadPoolExecutor dbThreadPoolExecutor = new ThreadPoolExecutor(10, 10, 20, TimeUnit.SECONDS, new LinkedBlockingDeque<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "db-metadata-manager-" + counter.incrementAndGet());
            }
        });

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "metadata-manager-" + counter.incrementAndGet());
            }
        });


    private final Remoting2Manage remoting2Manage = Remoting2Manage.getInstance();

    private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(2);


    /**
     * db
     */
    private final Map<MetadataType, SyncMetadataCreateFactory> syncMetadataCreateFactoryMap = new HashMap<>();


    private final Map<String, List<MetadataSyncWrapper>> metadataSyncConfigMap = new ConcurrentHashMap<>();

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @Setter
    private List<DataMetadataHandler<Object>> dataMetadataHandlerList;


    @Setter
    private MetadataSyncResultHandler metadataSyncResultHandler;


    public void init(Integer initialDelay, Integer period, List<DatabaseAndMetadataMapper> databaseAndMetadataMapperList) {

        Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap = new HashMap<>();
        databaseAndMetadataMapperList.forEach(value -> {
            databaseAndMetadataMapperMap.put(value.getDatabaseHandlerClass(), value);
        });
        this.init(initialDelay, period, databaseAndMetadataMapperMap);
    }

    public void init(Integer initialDelay, Integer period, Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap) {

        dataMetadataHandlerList.forEach((v) -> {
            DatabaseAndMetadataMapper databaseAndMetadataMapper = databaseAndMetadataMapperMap.get(v.getClass());
            SyncMetadataCreateFactory syncMetadataCreateFactory = new SyncMetadataCreateFactory();
            syncMetadataCreateFactory.setDataMetadataHandler(v);
            syncMetadataCreateFactory.setConvertMetaData((ConvertMetaData<Object, BaseClusterIdBase>) databaseAndMetadataMapper.getConvertMetaData());
            syncMetadataCreateFactory.setMetadataType(databaseAndMetadataMapper.getMetaType());
            syncMetadataCreateFactory.setDatabaseAndMetadataMapper(databaseAndMetadataMapper);

            syncMetadataCreateFactoryMap.put(databaseAndMetadataMapper.getMetaType(), syncMetadataCreateFactory);
        });

        scheduledExecutorService.scheduleAtFixedRate(this::run, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public void run() {
        final long startTime = System.currentTimeMillis();
        int index = atomicInteger.incrementAndGet();
        if (log.isDebugEnabled()) {
            log.debug("{} run , index {}", this.getClass().getSimpleName(), index);
        }
        CountDownLatch countDownLatch = new CountDownLatch(syncMetadataCreateFactoryMap.size());
        syncMetadataCreateFactoryMap.forEach((k, value) -> {
            dbThreadPoolExecutor.execute(() -> {
                try {
                    // 这里可以打印日志
                    if (log.isDebugEnabled()) {
                        log.debug("index {}  sync data is {} ", index, k);
                    }
                    value.persistence();
                    value.loadData();
                    if (log.isDebugEnabled()) {
                        log.debug("index {} sync time {} ", index, (System.currentTimeMillis() - startTime));
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        dbThreadPoolExecutor.execute(() -> {
            try {
                this.metadataSyncResultHandler.persistence();
                if (log.isDebugEnabled()) {
                    log.debug("index {} , result complete persistence, time {} ", index, (System.currentTimeMillis() - startTime));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        log.info("syc db and result data persistence complete. time:{}", (System.currentTimeMillis() - startTime));
        metadataSyncConfigMap.forEach((key, value) -> {
            value.forEach(v -> {
                threadPoolExecutor.execute(v);
            });
        });
    }


    public void register(BaseSyncBase baseSyncBase) {
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.NO_TRUSTEESHIP)) {
            return;
        }
        List<MetadataSyncWrapper> metadataSyncWrappers = new ArrayList<>();
        List<MetadataSyncResult> metadataSyncResultList = new ArrayList<>();

        ClusterType clusterType = baseSyncBase.getClusterType();

        ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterType);
        List<MetadataType> metadataTypeList = clusterSyncMetadata.getMetadataTypeList();
        metadataTypeList.forEach((value -> {
            // build MetadataSyncResult
            MetadataSyncResult metadataSyncResult = new MetadataSyncResult();
            metadataSyncResult.setMetadataType(value);
            metadataSyncResult.setKey(baseSyncBase.getUnique());
            metadataSyncResult.setBaseSyncBase(baseSyncBase);
            metadataSyncResultList.add(metadataSyncResult);

            SyncMetadataCreateFactory syncMetadataCreateFactory = syncMetadataCreateFactoryMap.get(value);
            // build MetadataSyncConfig
            MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
            metadataSyncConfig.setMetadataSyncResult(metadataSyncResult);
            metadataSyncConfig.setClusterServiceType(baseSyncBase.getTrusteeshipType());
            metadataSyncConfig.setFirstToWhom(baseSyncBase.getFirstToWhom());
            metadataSyncConfig.setDataBasesHandler(syncMetadataCreateFactory.createDataMetadataHandler(baseSyncBase));

            DataMetadataHandler<BaseClusterIdBase> object = remoting2Manage.createDataMetadataHandler(
                syncMetadataCreateFactory.getDatabaseAndMetadataMapper().getMetadataHandlerClass(), baseSyncBase);
            metadataSyncConfig.setClusterService(object);
            metadataSyncConfig.setBaseSyncBase(baseSyncBase);
            metadataSyncConfig.setMetadataType(value);

            // build MetadataSyncWrapper
            MetadataSyncWrapper metadataSyncWrapper = new MetadataSyncWrapper();
            metadataSyncWrapper.setMetadataSyncConfig(metadataSyncConfig);
            metadataSyncWrapper.setMetadataSyncResultHandler(metadataSyncResultHandler);

            metadataSyncWrapper.createDifference();
            metadataSyncWrappers.add(metadataSyncWrapper);

        }));
        this.metadataSyncConfigMap.put(baseSyncBase.getUnique(), metadataSyncWrappers);
        this.metadataSyncResultHandler.register(metadataSyncResultList);

    }

    public void unRegister(BaseSyncBase baseSyncBase) {
        this.metadataSyncConfigMap.remove(baseSyncBase.getUnique());
        this.metadataSyncResultHandler.unregister(baseSyncBase);
    }


    @Data
    public static class MetadataSyncConfig {

        private MetadataType metadataType;

        private BaseSyncBase baseSyncBase;

        private MetadataSyncResult metadataSyncResult;

        private ClusterTrusteeshipType clusterServiceType;

        /**
         * 当 ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE 时，FirstToWhom 为 FirstToWhom.RUNTIME ，无效
         * <p>
         * 当 ClusterTrusteeshipType.TRUSTEESHIP 时，FirstToWhom 为 FirstToWhom.DASHBOARD ，无效
         */
        private FirstToWhom firstToWhom;

        private DataMetadataHandler<BaseClusterIdBase> dataBasesHandler;

        // 直接映射 api？ config 是直接读取整个runtime 里面的数据
        // 还是封装一层？
        // 这里如何变简单。直接 remote api？
        private DataMetadataHandler<BaseClusterIdBase> clusterService;


    }


}