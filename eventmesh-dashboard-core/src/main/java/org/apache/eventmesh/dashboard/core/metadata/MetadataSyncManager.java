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

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.model.metadata.MetadataConfig;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import lombok.Setter;

/**
 * 启动应该进行差异化对比。进行差异化增量，差异化删除 表 -> increment-> remote -> function -> data -> conver -> request
 */
public class MetadataSyncManager {

    private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "metadata-manager-" + counter.incrementAndGet());
            }
        });
    @Setter
    private Map<Class<?>, MetadataSyncWrapper> metadataSyncWrapperMap = new HashMap<>();
    @Setter
    private RemotingManager remotingManager;


    public void register(MetadataSyncConfig metadataSyncConfig) {
        MetadataSyncWrapper metadataSyncWrapper = new MetadataSyncWrapper();
        metadataSyncWrapper.setMetadataClass(metadataSyncConfig.getMetadataClass());
        metadataSyncWrapper.setEntityClass(metadataSyncConfig.getEntityClass());
        metadataSyncWrapper.setDataBasesHandler(metadataSyncConfig.getDataBasesHandler());
        metadataSyncWrapper.setClusterService(metadataSyncConfig.getClusterService());
        metadataSyncWrapperMap.put(metadataSyncWrapper.getMetadataClass(), metadataSyncWrapper);
    }

    public void init(Integer initialDelay, Integer period) {
        scheduledExecutorService.scheduleAtFixedRate(this::run, initialDelay, period, TimeUnit.SECONDS);
    }


    public void run() {
        for (MetadataSyncWrapper metadataSyncWrapper : metadataSyncWrapperMap.values()) {
            // 一个MetadataSyncWrapper 一个线程
            threadPoolExecutor.execute(metadataSyncWrapper);
        }
    }


    @Data
    public static class MetadataSyncConfig {

        private Class<?> metadataClass;

        private Class<?> entityClass;

        private MetadataHandler dataBasesHandler;

        // 直接映射 api？ config 是直接读取整个runtime 里面的数据
        // 还是封装一层？
        // 这里如何变简单。直接 remote api？
        private MetadataHandler clusterService;
    }

    @Data
    public class MetadataSyncWrapper implements Runnable {

        private Class<?> metadataClass;

        private Class<?> entityClass;

        private MetadataHandler dataBasesHandler;

        // 直接映射 api？ config 是直接读取整个runtime 里面的数据
        // 还是封装一层？
        // 这里如何变简单。直接 remote api？
        private MetadataHandler clusterService;

        private CountDownLatch countDownLatch = new CountDownLatch(2);


        private Map<String, MetadataConfig> clusterData = new HashMap<>();

        private Map<String, MetadataConfig> dataBasesData = new HashMap<>();

        private List<MetadataConfig> toClusterUpdate = new ArrayList<>();


        private List<MetadataConfig> toDataUpdate = new ArrayList<>();

        private List<MetadataConfig> toDelete = new ArrayList<>();

        private List<MetadataConfig> toInsert = new ArrayList<>();

        private long lastTime = System.currentTimeMillis();

        @Override
        public void run() {

            try {
                // 全程托管，托管都需要，异步 读取 cluster 信息
                threadPoolExecutor.execute(this::syncClusterData);
                // 异步 读取数据库信息。是否进行增量处理
                threadPoolExecutor.execute(this::syncDatabasesData);
                this.lastTime = System.currentTimeMillis();
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

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                countDownLatch = new CountDownLatch(2);
                clusterData.clear();
                dataBasesData.clear();
            }


        }

        public void syncClusterData() {
            try {
                List<MetadataConfig> metadataConfigList = this.clusterService.getData();
                this.arrange(this.clusterData, metadataConfigList);
            } finally {
                countDownLatch.countDown();
            }

        }

        public void syncDatabasesData() {
            try {
                List<MetadataConfig> metadataConfigList = this.dataBasesHandler.getData();
                this.arrange(this.dataBasesData, metadataConfigList);
            } finally {
                countDownLatch.countDown();
            }
        }

        public void arrange(Map<String, MetadataConfig> data, List<MetadataConfig> metadataConfigList) {
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
            this.dataBasesHandler.updateMetadata(this.filterTrusteeship(this.toClusterUpdate));
            this.dataBasesHandler.addMetadata(this.filterTrusteeship(this.toDelete));
        }

        public void toFullTrusteeship() {
            //
            this.clusterService.addMetadata(this.filterFullTrusteeship(this.toInsert));
            this.clusterService.addMetadata(this.filterFullTrusteeship(this.toDataUpdate));
            this.clusterService.deleteMetadata(this.filterTrusteeship(this.toDelete));
        }

        private List<MetadataConfig> filterFullTrusteeship(List<MetadataConfig> metadataConfigList) {
            return this.filter(metadataConfigList, ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP);
        }

        private List<MetadataConfig> filterTrusteeship(List<MetadataConfig> metadataConfigList) {
            return this.filter(metadataConfigList, ClusterTrusteeshipType.TRUSTEESHIP);
        }

        private List<MetadataConfig> filter(List<MetadataConfig> metadataConfigList, ClusterTrusteeshipType clusterTrusteeshipType) {
            List<MetadataConfig> newMetadataConfigList = new ArrayList<>();
            for (MetadataConfig metadataConfig : metadataConfigList) {
                if (remotingManager.isClusterTrusteeshipType(metadataConfig.getClusterId(), clusterTrusteeshipType)) {
                    newMetadataConfigList.add(metadataConfig);
                }
            }
            return newMetadataConfigList;
        }
    }

}
