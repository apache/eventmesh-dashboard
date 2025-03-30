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
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.MetadataConfig;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManage;

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


    private final Remoting2Manage remoting2Manage = new Remoting2Manage();


    private final Map<Class<?>, Map<Long, List<MetadataSyncBaseWrapper>>> metadataSyncBaseWRapperMap = new ConcurrentHashMap<>();


    private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(2);


    /**
     * db
     */
    private final Map<MetadataType, SyncMetadataCreateFactory> syncMetadataCreateFactoryMap = new HashMap<>();


    private Map<Class<?>, MetadataSyncWrapper> metadataSyncWrapperMap = new HashMap<>();

    private Map<String, List<MetadataSyncConfig>> metadataSyncConfigMap = new ConcurrentHashMap<>();

    @Setter
    private List<DataMetadataHandler<Object>> dataMetadataHandlerList;

    @Setter
    private List<DatabaseAndMetadataMapper> databaseAndMetadataMapperList;

    @Setter
    private RemotingManage remotingManage;


    @Setter
    private MetadataSyncResultHandler metadataSyncResultHandler;


    public void init(Integer initialDelay, Integer period) {

        Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap = new HashMap<>();
        databaseAndMetadataMapperList.forEach(value -> {
            databaseAndMetadataMapperMap.put(value.getDatabaseClass(), value);
        });
        dataMetadataHandlerList.forEach((v) -> {
            DatabaseAndMetadataMapper databaseAndMetadataMapper = databaseAndMetadataMapperMap.get(v.getClass());
            SyncMetadataCreateFactory syncMetadataCreateFactory = new SyncMetadataCreateFactory();
            syncMetadataCreateFactory.setDataMetadataHandler(v);
            syncMetadataCreateFactory.setConvertMetaData(databaseAndMetadataMapper.getConvertMetaData());
            syncMetadataCreateFactory.setMetadataType(databaseAndMetadataMapper.getMetaType());
            syncMetadataCreateFactory.setDatabaseAndMetadataMapper(databaseAndMetadataMapper);

            syncMetadataCreateFactoryMap.put(databaseAndMetadataMapper.getMetaType(), syncMetadataCreateFactory);
        });

        scheduledExecutorService.scheduleAtFixedRate(this::run, initialDelay, period, TimeUnit.SECONDS);
    }

    public void sync() {
        syncMetadataCreateFactoryMap.forEach((k, value) -> {
            dbThreadPoolExecutor.execute(() -> {
                value.loadData();
                value.persistence();
            });
        });
    }


    public void register(BaseSyncBase baseSyncBase) {
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.NO_TRUSTEESHIP)) {
            return;
        }
        List<MetadataSyncConfig> metadataSyncConfigList = new ArrayList<>();
        List<MetadataSyncResult> metadataSyncResultList = new ArrayList<>();

        ClusterType clusterType = baseSyncBase.getClusterType();

        ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterType);
        List<MetadataType> metadataTypeList = clusterSyncMetadata.getMetadataTypeList();
        metadataTypeList.forEach((value -> {
            SyncMetadataCreateFactory syncMetadataCreateFactory = syncMetadataCreateFactoryMap.get(value);
            MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
            metadataSyncConfigList.add(metadataSyncConfig);
            metadataSyncConfig.setDataBasesHandler(syncMetadataCreateFactory.createDataMetadataHandler(baseSyncBase, baseSyncBase.getId()));

            Object object =
                remoting2Manage.createProxy(syncMetadataCreateFactory.getDatabaseAndMetadataMapper().getMetadataClass(), baseSyncBase, clusterType);
            metadataSyncConfig.setClusterService((MetadataHandler) object);
            metadataSyncConfig.setConvertMetaData(syncMetadataCreateFactory.getConvertMetaData());

            metadataSyncConfig.setBaseSyncBase(baseSyncBase);
            metadataSyncConfig.setMetadataType(value);

            MetadataSyncResult metadataSyncResult = new MetadataSyncResult();
            metadataSyncResult.setMetadataType(value);
            metadataSyncResult.setKey(baseSyncBase.getUnique());
            metadataSyncResult.setBaseSyncBase(baseSyncBase);
            metadataSyncResultList.add(metadataSyncResult);

            metadataSyncConfig.setMetadataSyncResult(metadataSyncResult);


        }));
        this.metadataSyncConfigMap.put(baseSyncBase.getUnique(), metadataSyncConfigList);
        this.metadataSyncResultHandler.register(metadataSyncResultList);

    }

    public void unRegister(BaseSyncBase baseSyncBase) {
        this.metadataSyncConfigMap.remove(baseSyncBase.getUnique());
        this.metadataSyncResultHandler.unregister(baseSyncBase);
    }

    public void cancel() {

    }


    public void run() {
        for (MetadataSyncWrapper metadataSyncWrapper : metadataSyncWrapperMap.values()) {
            // 一个MetadataSyncWrapper 一个线程
            threadPoolExecutor.execute(metadataSyncWrapper);
        }
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

        private ConvertMetaData<Object, Object> convertMetaData;

        private MetadataHandler<BaseRuntimeIdBase> dataBasesHandler;

        // 直接映射 api？ config 是直接读取整个runtime 里面的数据
        // 还是封装一层？
        // 这里如何变简单。直接 remote api？
        private MetadataHandler<BaseRuntimeIdBase> clusterService;


    }

    static class MetadataSyncBaseWrapper {

        private MetadataSyncConfig metadataSyncConfigs;

        private SyncMetadataCreateFactory syncMetadataCreateFactory;

    }

    /**
     * 定时比对 1.
     */
    @Data
    public class MetadataSyncWrapper implements Runnable {


        private MetadataSyncConfig metadataSyncConfig;

        private CountDownLatch countDownLatch = new CountDownLatch(2);


        private Map<String, MetadataConfig> clusterData = new HashMap<>();

        private Map<String, MetadataConfig> dataBasesData = new HashMap<>();

        private List<MetadataConfig> toClusterUpdate = new ArrayList<>();


        private List<MetadataConfig> toDataUpdate = new ArrayList<>();

        private List<MetadataConfig> toDelete = new ArrayList<>();

        private List<MetadataConfig> toInsert = new ArrayList<>();

        private long lastTime = System.currentTimeMillis();

        private volatile boolean isCheck = false;

        @Override
        public void run() {

            try {
                this.lastTime = System.currentTimeMillis();
                // 全程托管，托管都需要，异步 读取 cluster 信息
                if (this.isSyncClusterData()) {
                    threadPoolExecutor.execute(this::syncClusterData);
                } else {
                    countDownLatch.countDown();
                }
                // 异步 读取数据库信息。是否进行增量处理
                if (this.isSyncDatabasesData()) {
                    threadPoolExecutor.execute(this::syncDatabasesData);
                } else {
                    countDownLatch.countDown();
                }
                countDownLatch.await();
                // 对读取的数据进行分类，
                this.difference();
                // 托管的进行差异化之后。进行写库。
                if (this.toInsert.isEmpty() && this.toClusterUpdate.isEmpty() && this.toDelete.isEmpty() && this.toDataUpdate.isEmpty()) {
                    return;
                }
                this.toTrusteeship();
                this.toFullTrusteeship();
                // 全程托管的  cluster 存在，数据库不存在就删除。 进行删除，  新增的直接操作 cluster

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                countDownLatch = new CountDownLatch(2);
                clusterData.clear();
                dataBasesData.clear();
                MetadataSyncManage.this.metadataSyncResultHandler.handleMetadataSyncResult(this.metadataSyncConfig.getMetadataSyncResult());
            }
        }

        private boolean isSyncClusterData() {
            if (this.isCheck) {
                return true;
            }
            BaseSyncBase baseSyncBase = this.metadataSyncConfig.getBaseSyncBase();
            if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.DASHBOARD) ||
                Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.COMPLETE)) {
                return true;
            }
            if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.SELF)) {
                return true;
            }
            if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP)) {
                return true;
            }
            return false;

        }

        private boolean isSyncDatabasesData() {
            if (this.isCheck) {
                return true;
            }
            BaseSyncBase baseSyncBase = this.metadataSyncConfig.getBaseSyncBase();
            if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.DASHBOARD) ||
                Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.COMPLETE)) {
                return true;
            }
            if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP_FIND)) {
                return true;
            }
            if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE)) {
                return true;
            }
            return false;
        }

        public void syncClusterData() {
            try {
                List<BaseRuntimeIdBase> metadataConfigList = this.metadataSyncConfig.clusterService.getData();
                this.arrange(this.clusterData, metadataConfigList);
            } finally {
                countDownLatch.countDown();
            }

        }

        public void syncDatabasesData() {
            try {
                List<BaseRuntimeIdBase> metadataConfigList = this.metadataSyncConfig.dataBasesHandler.getData();
                this.arrange(this.dataBasesData, metadataConfigList);
            } finally {
                countDownLatch.countDown();
            }

        }

        public void arrange(Map<String, BaseRuntimeIdBase> data, List<BaseRuntimeIdBase> metadataConfigList) {
            for (MetadataConfig metadataConfig : metadataConfigList) {
                data.put(metadataConfig.getUnique(), metadataConfig);
            }
        }

        /**
         * TODO 一定要注意。这个就得是 cluster 的差集。如果要对 database 操作 就要反翻过啦
         */
        public void difference() {

            this.toDataUpdate = new ArrayList<>();
            this.toClusterUpdate = new ArrayList<>();
            this.toDelete = new ArrayList<>();
            this.toInsert = new ArrayList<>();
            Map<String, MetadataConfig> newClusterData = new HashMap<>(this.clusterData);
            Map<String, MetadataConfig> newDataBasesData = new HashMap<>(this.dataBasesData);
            for (Map.Entry<String, MetadataConfig> entry : newClusterData.entrySet()) {
                MetadataConfig serviceObject = newDataBasesData.remove(entry.getKey());
                if (Objects.isNull(serviceObject)) {
                    toDelete.add(entry.getValue());
                } else {
                    if (!serviceObject.equals(entry.getValue())) {
                        this.toClusterUpdate.add(entry.getValue());
                        this.toDataUpdate.add(serviceObject);
                    }
                }
            }
            this.toInsert = new ArrayList<>(newDataBasesData.values());

        }

        public void toTrusteeship() {
            // 如果 update 中 cluster 与  databases 数据不一致。 以谁为主
            //
            this.metadataSyncConfig.dataBasesHandler.updateMetadata(this.filterTrusteeship(this.toClusterUpdate));
            this.metadataSyncConfig.dataBasesHandler.addMetadata(this.filterTrusteeship(this.toDelete));
        }

        public void toFullTrusteeship() {
            //
            this.metadataSyncConfig.clusterService.addMetadata(this.filterFullTrusteeship(this.toInsert));
            this.metadataSyncConfig.clusterService.addMetadata(this.filterFullTrusteeship(this.toDataUpdate));
            this.metadataSyncConfig.clusterService.deleteMetadata(this.filterTrusteeship(this.toDelete));
        }

        private List<MetadataConfig> filterFullTrusteeship(List<MetadataConfig> metadataConfigList) {
            return this.filter(metadataConfigList, ClusterTrusteeshipType.SELF);
        }

        private List<MetadataConfig> filterTrusteeship(List<MetadataConfig> metadataConfigList) {
            return this.filter(metadataConfigList, ClusterTrusteeshipType.TRUSTEESHIP);
        }

        private List<MetadataConfig> filter(List<MetadataConfig> metadataConfigList, ClusterTrusteeshipType clusterTrusteeshipType) {
            List<MetadataConfig> newMetadataConfigList = new ArrayList<>();
            for (MetadataConfig metadataConfig : metadataConfigList) {
                if (remotingManage.isClusterTrusteeshipType(metadataConfig.getClusterId(), clusterTrusteeshipType)) {
                    newMetadataConfigList.add(metadataConfig);
                }
            }
            return newMetadataConfigList;
        }
    }

}
