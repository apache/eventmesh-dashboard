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


package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.HealthCheckService;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Health2Service {

    private static final Map<ClusterType, Class<?>> HEALTH_PING_CHECK_CLASS_CACHE = new HashMap<>();

    private static final Map<ClusterType, Class<?>> HEALTH_TOPIC_CHECK_CLASS_CACHE = new HashMap<>();

    static {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(HealthCheckService.class);
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(Health2Service.class).subPath("/check/impl/**").interfaceSet(interfaceSet).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(Health2Service::setClassCache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<String, HealthCheckWrapper> checkServiceMap = new ConcurrentHashMap<>();
    @Deprecated
    private final Map<Long, ClusterHealthCheckService> clusterHealthCheckServiceMap = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(32, 32, 5, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(@Nullable Runnable r) {
                return new Thread(r, "health-manager-" + counter.incrementAndGet());
            }
        });
    @Setter
    private HealthDataService dataService;
    private LocalDateTime beginTime = LocalDateTime.now();

    private static void setClassCache(Class<?> clazz) {
        HealthCheckType checkType = clazz.getAnnotation(HealthCheckType.class);
        if (Objects.isNull(checkType)) {
            return;
        }
        Map<ClusterType, Class<?>> map =
            Objects.equals(checkType.healthType(), HealthCheckTypeEnum.PING) ? HEALTH_PING_CHECK_CLASS_CACHE : HEALTH_TOPIC_CHECK_CLASS_CACHE;

        for (ClusterType clusterType : checkType.clusterType()) {
            log.info("cluster type:{}  health class name is {}", clusterType, clazz.getSimpleName());
            map.put(clusterType, clazz);
        }
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public void register(BaseSyncBase baseSyncBase) {
        try {
            HealthCheckWrapper healthCheckWrapper = this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.PING);
            if (baseSyncBase.getClusterType().isHealthTopic()) {
                this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.TOPIC);
            }
            if (log.isDebugEnabled()) {
                AbstractCreateSDKConfig abstractCreateSDKConfig = (AbstractCreateSDKConfig) healthCheckWrapper.getCheckService().getCreateSdkConfig();
                log.debug("register health check service for {} , metadata type {} , {} ,{}", baseSyncBase.getClusterType(),
                    baseSyncBase.getClass().getSimpleName(), baseSyncBase.getId(),
                    abstractCreateSDKConfig.getUniqueKey());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public void unRegister(BaseSyncBase baseSyncBase) {
        ClusterHealthCheckService clusterHealthCheckService = this.clusterHealthCheckServiceMap.remove(baseSyncBase.getClusterId());
        if (Objects.nonNull(clusterHealthCheckService)) {
            clusterHealthCheckService.unRegister(baseSyncBase);
        }
        this.checkServiceMap.remove(getKey(baseSyncBase, HealthCheckTypeEnum.PING));
        this.checkServiceMap.remove(getKey(baseSyncBase, HealthCheckTypeEnum.TOPIC));
        if (log.isDebugEnabled()) {
            RuntimeMetadata runtimeMetadata = (RuntimeMetadata) baseSyncBase;
            log.debug("unRegister health check service for {} , {} ,{},{}", baseSyncBase.getClusterType(), baseSyncBase.getId(),
                runtimeMetadata.getHost(), runtimeMetadata.getPort());
        }
    }

    @Deprecated
    public void unRegisterCluster(Long clusterId) {
        this.clusterHealthCheckServiceMap.remove(clusterId);
    }


    private HealthCheckWrapper createHealthCheckWrapper(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        Map<ClusterType, Class<?>> map =
            Objects.equals(healthCheckTypeEnum, HealthCheckTypeEnum.PING) ? HEALTH_PING_CHECK_CLASS_CACHE : HEALTH_TOPIC_CHECK_CLASS_CACHE;
        Class<?> clazz = map.get(baseSyncBase.getClusterType());
        AbstractHealthCheckService<Object> abstractHealthCheckService = SDKManage.getInstance().createAbstractClientInfo(clazz, baseSyncBase);
        HealthCheckWrapper healthCheckWrapper =
            this.createHealthCheckWrapper(baseSyncBase, abstractHealthCheckService, healthCheckTypeEnum);
        this.checkServiceMap.put(healthCheckWrapper.getKey(), healthCheckWrapper);
        return healthCheckWrapper;
    }

    private HealthCheckWrapper createHealthCheckWrapper(BaseSyncBase baseSyncBase,
        AbstractHealthCheckService<Object> healthCheckService, HealthCheckTypeEnum healthCheckTypeEnum) {
        HealthCheckWrapper healthCheckWrapper = new HealthCheckWrapper();
        healthCheckWrapper.setHealthCheckTypeEnum(healthCheckTypeEnum);
        healthCheckWrapper.setBaseSyncBase(baseSyncBase);
        healthCheckWrapper.setCheckService(healthCheckService);

        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();

        healthCheckWrapper.setHealthCheckResultEntity(healthCheckResultEntity);
        return healthCheckWrapper;
    }

    public void executeAll() {
        try {
            this.doExecuteAll();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public void doExecuteAll() {
        if (checkServiceMap.isEmpty()) {
            log.info("check service is empty");
            return;
        }
        long startTime = System.currentTimeMillis();
        this.beginTime = LocalDateTime.now();
        List<HealthCheckResultEntity> healthCheckResultEntityList = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(this.checkServiceMap.size());
        this.checkServiceMap.forEach((k, wrapper) -> {

            DefaultHealthCheckCallback healthExecutor = new DefaultHealthCheckCallback();
            healthExecutor.healthCheckWrapper = wrapper;
            healthExecutor.countDownLatch = countDownLatch;
            //
            if (wrapper.isCap()) {
                healthExecutor.healthCheckResultEntityMap = wrapper.createHealthCheckResultEntityMap();
                healthCheckResultEntityList.addAll(healthExecutor.healthCheckResultEntityMap.values());
            } else {
                healthCheckResultEntityList.add(wrapper.createHealthCheckResultEntity());
                healthExecutor.healthCheckResultEntity = wrapper.getHealthCheckResultEntity();
            }

            threadPoolExecutor.execute(() -> {
                try {
                    wrapper.checkService.check(healthExecutor);
                } catch (Exception e) {
                    healthExecutor.onFail(e);
                }
            });
        });
        try {
            dataService.batchInsertHealthCheckResult(healthCheckResultEntityList);
        } catch (Exception e) {
            log.error("batchInsertHealthCheckResult failed", e);
        }
        try {
            boolean await = countDownLatch.await(3, TimeUnit.SECONDS);
            log.info("await ia {} downLatch count {}, startup cost {} ms", await, countDownLatch.getCount(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                LocalDateTime endTime = LocalDateTime.now();
                healthCheckResultEntityList.forEach(healthCheckResultEntity -> {
                    if (healthCheckResultEntity.getResult() == HealthCheckStatus.ING) {
                        healthCheckResultEntity.setResult(HealthCheckStatus.TIMEOUT);
                        healthCheckResultEntity.setFinishTime(endTime);
                    }
                });
                dataService.batchInsertHealthCheckResult(healthCheckResultEntityList);
            } catch (Exception e) {
                log.error("batchUpdateCheckResultByClusterIdAndTypeAndTypeId failed", e);
            }
        }
    }


    public String getKey(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        return healthCheckTypeEnum.toString() + "-" + baseSyncBase.getId();
    }

    static class DefaultHealthCheckCallback implements HealthCheckCallback {

        private HealthCheckWrapper healthCheckWrapper;

        private CountDownLatch countDownLatch;

        private HealthCheckResultEntity healthCheckResultEntity;

        private Map<String, HealthCheckResultEntity> healthCheckResultEntityMap;

        @Override
        public void onSuccess() {
            healthCheckResultEntity.setResult(HealthCheckStatus.SUCCESS);
            healthCheckResultEntity.setFinishTime(LocalDateTime.now());
            countDownLatch.countDown();
        }

        @Override
        public void onFail(Exception e) {
            healthCheckResultEntity.setResult(HealthCheckStatus.FAILED);
            healthCheckResultEntity.setResultDesc(e.getMessage());
            countDownLatch.countDown();
            log.error("healthCheckCallback onFail Id:  ", e);
        }
    }

    @Data
    class HealthCheckWrapper {

        private BaseSyncBase baseSyncBase;

        private AbstractHealthCheckService<Object> checkService;

        private String address;

        private HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();


        private HealthCheckTypeEnum healthCheckTypeEnum;


        private HealthCheckResultEntity createHealthCheckResultEntity() {
            if (Objects.isNull(this.address)) {
                this.address = ((AbstractCreateSDKConfig) checkService.getCreateSdkConfig()).doUniqueKey();
            }
            return this.createHealthCheckResultEntity(this.address);
        }

        private HealthCheckResultEntity createHealthCheckResultEntity(String address) {
            HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
            healthCheckResultEntity.setClusterType(this.baseSyncBase.getClusterType());
            healthCheckResultEntity.setClusterId(this.baseSyncBase.getClusterId());
            healthCheckResultEntity.setProtocol("");
            healthCheckResultEntity.setType(2);
            healthCheckResultEntity.setTypeId(this.baseSyncBase.getId());
            healthCheckResultEntity.setAddress(this.address);
            healthCheckResultEntity.setHealthCheckType(this.healthCheckTypeEnum);
            healthCheckResultEntity.setResult(HealthCheckStatus.ING);
            healthCheckResultEntity.setResultDesc("");
            healthCheckResultEntity.setBeginTime(beginTime);
            this.healthCheckResultEntity = healthCheckResultEntity;
            return healthCheckResultEntity;
        }

        @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
        public Map<String, HealthCheckResultEntity> createHealthCheckResultEntityMap() {
            Map<String, HealthCheckResultEntity> healthCheckResultEntityMap = new HashMap<>();
            AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig = (AbstractMultiCreateSDKConfig) this.checkService.getCreateSdkConfig();
            for (String address : abstractMultiCreateSDKConfig.getNetAddresses()) {
                healthCheckResultEntityMap.put(address, createHealthCheckResultEntity(address));
            }
            return healthCheckResultEntityMap;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof HealthCheckWrapper wrapper) {
                return this.baseSyncBase.getId().equals(wrapper.getBaseSyncBase().getId()) && this.baseSyncBase.getClusterType()
                    .equals(wrapper.getBaseSyncBase().getClusterType());
            }
            return false;
        }


        public String getKey() {
            return Health2Service.this.getKey(this.baseSyncBase, this.healthCheckTypeEnum);
        }

        public boolean isCap() {
            return ClusterSyncMetadataEnum.getClusterFramework(this.baseSyncBase.getClusterType()).isCAP();
        }

    }


}
