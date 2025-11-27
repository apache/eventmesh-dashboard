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
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
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

import javax.annotation.Nullable;

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
            public Thread newThread(@Nullable Runnable r) {
                return new Thread(r, "db-metadata-manager-" + counter.incrementAndGet());
            }
        });

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(@Nullable Runnable r) {
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

    @Setter
    private ColonyDO<ClusterDO> colonyDO;


    private Integer period;


    public void init(Integer initialDelay, Integer period, List<DatabaseAndMetadataMapper> databaseAndMetadataMapperList) {

        Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap = new HashMap<>();
        databaseAndMetadataMapperList.forEach(value -> {
            databaseAndMetadataMapperMap.put(value.getDatabaseHandlerClass(), value);
        });
        this.init(initialDelay, period, databaseAndMetadataMapperMap);
    }

    public void init(Integer initialDelay, Integer period, Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap) {

        dataMetadataHandlerList.forEach((v) -> {
            Class<?> clazz = v.getClass();
            DatabaseAndMetadataMapper databaseAndMetadataMapper = databaseAndMetadataMapperMap.get(clazz);
            if (Objects.isNull(databaseAndMetadataMapper)) {
                String className = clazz.getName();
                className = className.substring(0, className.indexOf("$$"));
                try {
                    clazz = Class.forName(className);
                    databaseAndMetadataMapper = databaseAndMetadataMapperMap.get(clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            if (Objects.isNull(databaseAndMetadataMapper)) {
                log.error("can not find databaseAndMetadataMapper for class {}", v);
                return;
            }
            SyncMetadataCreateFactory syncMetadataCreateFactory = new SyncMetadataCreateFactory();
            syncMetadataCreateFactory.setDataMetadataHandler(v);
            syncMetadataCreateFactory.setConvertMetaData((ConvertMetaData<Object, BaseClusterIdBase>) databaseAndMetadataMapper.getConvertMetaData());
            syncMetadataCreateFactory.setMetadataType(databaseAndMetadataMapper.getMetaType());
            syncMetadataCreateFactory.setDatabaseAndMetadataMapper(databaseAndMetadataMapper);
            syncMetadataCreateFactory.setColonyDO(colonyDO);

            log.info("#sync type {} , isReadOnly {} databaseAndMetadataMapper class:{}",
                databaseAndMetadataMapper.getMetaType(),
                databaseAndMetadataMapper.getMetaType().isReadOnly(),
                databaseAndMetadataMapper.getDatabaseHandlerClass().getSimpleName());
            syncMetadataCreateFactoryMap.put(databaseAndMetadataMapper.getMetaType(), syncMetadataCreateFactory);
        });
        this.period = period;
        // TODO 测试时候，可以关闭
        scheduledExecutorService.scheduleAtFixedRate(this::run, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public void run() {
        final long startTime = System.currentTimeMillis();
        int index = atomicInteger.incrementAndGet();
        if (log.isTraceEnabled()) {
            log.trace("{} run , index {}", this.getClass().getSimpleName(), index);
        }
        // 先把 数据持久化到 databases
        // 然后从 databases 加载数据
        CountDownLatch countDownLatch = new CountDownLatch(syncMetadataCreateFactoryMap.size());
        syncMetadataCreateFactoryMap.forEach((k, value) -> {
            dbThreadPoolExecutor.execute(() -> {
                try {
                    value.persistence();
                    value.loadData();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                    if (log.isTraceEnabled()) {
                        log.trace("metadata {} index {} sync time {} ", value.getMetadataType(), index, (System.currentTimeMillis() - startTime));
                    }
                }
            });
        });
        // 把同步操作记录数据持久化到 database
        dbThreadPoolExecutor.execute(this::persistence);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        if (log.isTraceEnabled()) {
            log.trace("sync db and result data persistence complete. time:{}", (System.currentTimeMillis() - startTime));
        }
        // 执行同步行为
        metadataSyncConfigMap.forEach((key, value) -> {
            value.forEach(threadPoolExecutor::execute);
        });
    }


    private void persistence() {
        long startTime = System.currentTimeMillis();
        try {
            this.metadataSyncResultHandler.persistence();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("complete persistence, time {} ", (System.currentTimeMillis() - startTime));
            }
        }
    }


    public void register(BaseSyncBase baseSyncBase) {
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.NO_TRUSTEESHIP) ||
            Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.NOT)
        ) {
            log.info("type {} , is {} not sync , ClusterTrusteeshipType {}", baseSyncBase.getId(), baseSyncBase.getClusterType(),
                baseSyncBase.getTrusteeshipType());
            return;
        }
        List<MetadataSyncWrapper> metadataSyncWrappers = new ArrayList<>();
        List<MetadataSyncResult> metadataSyncResultList = new ArrayList<>();

        ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(baseSyncBase.getClusterType());
        List<MetadataType> metadataTypeList = clusterSyncMetadata.getMetadataTypeList();
        metadataTypeList.forEach((value -> {
            // build MetadataSyncResult
            MetadataSyncResult metadataSyncResult = createMetadataSyncResult(metadataSyncResultList, value, baseSyncBase);
            // build MetadataSyncConfig 是 io out 组织配置类
            MetadataSyncConfig metadataSyncConfig = createMetadataSyncConfig(value, metadataSyncResult, baseSyncBase);
            // build MetadataSyncWrapper , MetadataSyncWrapper 是同步执行对象
            createMetadataSyncWrapper(metadataSyncWrappers, metadataSyncConfig);

        }));
        this.metadataSyncConfigMap.put(baseSyncBase.getUnique(), metadataSyncWrappers);
        this.metadataSyncResultHandler.register(metadataSyncResultList);

    }

    public void unRegister(BaseSyncBase baseSyncBase) {
        this.metadataSyncConfigMap.remove(baseSyncBase.getUnique());
        this.metadataSyncResultHandler.unregister(baseSyncBase);
    }

    public MetadataSyncResult createMetadataSyncResult(List<MetadataSyncResult> metadataSyncResultList, MetadataType metadataType,
        BaseSyncBase baseSyncBase) {
        MetadataSyncResult metadataSyncResult = new MetadataSyncResult();
        metadataSyncResult.setMetadataType(metadataType);
        metadataSyncResult.setKey(baseSyncBase.getUnique());
        metadataSyncResult.setBaseSyncBase(baseSyncBase);
        metadataSyncResultList.add(metadataSyncResult);
        return metadataSyncResult;
    }

    private void createMetadataSyncWrapper(List<MetadataSyncWrapper> metadataSyncWrappers, MetadataSyncConfig metadataSyncConfig) {
        MetadataSyncWrapper metadataSyncWrapper = new MetadataSyncWrapper();
        metadataSyncWrapper.setMetadataSyncConfig(metadataSyncConfig);
        metadataSyncWrapper.setMetadataSyncResultHandler(metadataSyncResultHandler);
        metadataSyncWrapper.setPeriod(this.period);
        metadataSyncWrapper.createDifference();
        if (log.isDebugEnabled()) {
            log.debug("#sync metadata type {} , source type {} , source id {}, cluster type {}",
                metadataSyncConfig.getMetadataType(),
                metadataSyncConfig.getBaseSyncBase().getClass().getSimpleName(),
                metadataSyncConfig.getBaseSyncBase().getId(),
                metadataSyncConfig.getBaseSyncBase().getClusterType()
            );
        }
        metadataSyncWrappers.add(metadataSyncWrapper);

    }

    private MetadataSyncConfig createMetadataSyncConfig(MetadataType metadataType, MetadataSyncResult metadataSyncResult, BaseSyncBase baseSyncBase) {
        SyncMetadataCreateFactory syncMetadataCreateFactory = syncMetadataCreateFactoryMap.get(metadataType);
        // build MetadataSyncConfig 是 io out 组织配置类
        MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
        metadataSyncConfig.setMetadataSyncResult(metadataSyncResult);
        metadataSyncConfig.setClusterServiceType(baseSyncBase.getTrusteeshipType());
        metadataSyncConfig.setFirstToWhom(baseSyncBase.getFirstToWhom());
        metadataSyncConfig.setDataBasesHandler(syncMetadataCreateFactory.createDataMetadataHandler(baseSyncBase));

        DataMetadataHandler<BaseClusterIdBase> object = remoting2Manage.createDataMetadataHandler(
            syncMetadataCreateFactory.getDatabaseAndMetadataMapper().getMetadataHandlerClass(), baseSyncBase);
        metadataSyncConfig.setClusterService(object);
        metadataSyncConfig.setBaseSyncBase(baseSyncBase);
        metadataSyncConfig.setMetadataType(metadataType);
        return metadataSyncConfig;
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
         * 当 ClusterTrusteeshipType. TRUSTEESHIP 时，FirstToWhom 为 FirstToWhom. DASHBOARD ，无效
         */
        private FirstToWhom firstToWhom;

        private DataMetadataHandler<BaseClusterIdBase> dataBasesHandler;

        // 直接映射 api？ config 是直接读取整个runtime 里面的数据
        // 还是封装一层？
        // 这里如何变简单。直接 remote api？
        private DataMetadataHandler<BaseClusterIdBase> clusterService;


    }


}