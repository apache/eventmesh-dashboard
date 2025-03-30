package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractTopicHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.HealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Constructor;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Health2Service {

    private static final Map<ClusterType, Class<?>> HEALTH_CHECK_CLASS_CACHE = new HashMap<>();

    private static final Map<ClusterType, Class<?>> HEALTH_CLUSTER_CHECK_CLASS_CACHE = new HashMap<>();

    static {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(HealthCheckService.class);
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(SDKManage.class).subPath("/operation").interfaceSet(interfaceSet).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(Health2Service::setClassCache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static void setClassCache(Class<?> clazz) {
        clazz.getSuperclass();
        if (Objects.equals(clazz.getSuperclass(), AbstractTopicHealthCheckService.class)) {

        } else {

        }
        Map<ClusterType, Class<?>> map =
            Objects.equals(clazz, AbstractHealthCheckService.class) ? HEALTH_CHECK_CLASS_CACHE : HEALTH_CLUSTER_CHECK_CLASS_CACHE;
        //map.put(clazz.getSimpleName(), clazz);
    }


    private final Map<String, HealthCheckWrapper> checkServiceMap = new ConcurrentHashMap<>();

    private final Map<Long, ClusterHealthCheckService> clusterHealthCheckServiceMap = new ConcurrentHashMap<>();


    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 5, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    @Setter
    private HealthDataService dataService;


    public void register(BaseSyncBase baseSyncBase) {
        try {
            this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.PING);
            if (baseSyncBase.getClusterType().isHealthTopic()) {
                this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.PING);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public void unRegister(BaseSyncBase baseSyncBase) {
        ClusterHealthCheckService clusterHealthCheckService = this.clusterHealthCheckServiceMap.remove(baseSyncBase.getClusterId());
        if (Objects.nonNull(clusterHealthCheckService)) {
            clusterHealthCheckService.unRegister(this.toHealthCheckObjectConfig(baseSyncBase));
        }
        this.checkServiceMap.remove(getKey(baseSyncBase, HealthCheckTypeEnum.PING));
        this.checkServiceMap.remove(getKey(baseSyncBase, HealthCheckTypeEnum.TOPIC));
    }

    public void unRegisterCluster(Long clusterId) {
        this.clusterHealthCheckServiceMap.remove(clusterId);
    }

    private HealthCheckObjectConfig toHealthCheckObjectConfig(BaseSyncBase runtimeEntity) {
        HealthCheckObjectConfig healthCheckObjectConfig = new HealthCheckObjectConfig();
        return healthCheckObjectConfig;
    }


    AbstractHealthCheckService createHealthCheckWrapper(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        Map<ClusterType, Class<?>> map =
            Objects.equals(healthCheckTypeEnum, HealthCheckTypeEnum.PING) ? HEALTH_CHECK_CLASS_CACHE : HEALTH_CLUSTER_CHECK_CLASS_CACHE;
        Class<?> clazz = map.get(baseSyncBase.getClusterType());
        AbstractHealthCheckService abstractHealthCheckService = SDKManage.getInstance().createAbstractClientInfo(clazz, baseSyncBase.getUnique());
        SDKManage.getInstance().getClient(SDKTypeEnum.PING, null);
        HealthCheckWrapper healthCheckWrapper =
            this.createHealthCheckWrapper(baseSyncBase, abstractHealthCheckService, HealthCheckTypeEnum.PING);
        this.checkServiceMap.put(healthCheckWrapper.getKey(), healthCheckWrapper);
        return abstractHealthCheckService;
    }


    public void executeAll() {
        List<HealthCheckResultEntity> healthCheckResultEntityList = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(this.checkServiceMap.size());
        this.checkServiceMap.forEach((k, wrapper) -> {
            healthCheckResultEntityList.add(wrapper.createHealthCheckResultEntity());
            DefaultHealthCheckCallback healthExecutor = new DefaultHealthCheckCallback();
            healthExecutor.healthCheckWrapper = wrapper;
            healthExecutor.countDownLatch = countDownLatch;
            healthExecutor.healthCheckResultEntity = wrapper.getHealthCheckResultEntity();

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
            countDownLatch.await(3000, TimeUnit.MILLISECONDS);
            log.info("downLatch count {}", countDownLatch.getCount());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                dataService.batchUpdateCheckResultByClusterIdAndTypeAndTypeId(healthCheckResultEntityList);
            } catch (Exception e) {
                log.error("batchUpdateCheckResultByClusterIdAndTypeAndTypeId failed", e);
            }
        }
    }

    private HealthCheckWrapper createHealthCheckWrapper(BaseSyncBase baseSyncBase,
        AbstractHealthCheckService healthCheckService, HealthCheckTypeEnum healthCheckTypeEnum) {
        HealthCheckWrapper healthCheckWrapper = new HealthCheckWrapper();
        healthCheckWrapper.setHealthCheckTypeEnum(healthCheckTypeEnum);
        healthCheckWrapper.setBaseSyncBase(baseSyncBase);
        healthCheckWrapper.setCheckService(healthCheckService);

        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();

        healthCheckWrapper.setHealthCheckResultEntity(healthCheckResultEntity);
        return healthCheckWrapper;
    }

    public String getKey(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        return healthCheckTypeEnum.toString() + "-" + baseSyncBase.getId();
    }

    class DefaultHealthCheckCallback implements HealthCheckCallback {

        private HealthCheckWrapper healthCheckWrapper;

        private CountDownLatch countDownLatch;

        private HealthCheckResultEntity healthCheckResultEntity;

        @Override
        public void onSuccess() {
            healthCheckResultEntity.setResult(HealthCheckStatus.SUCCESS);
            healthCheckResultEntity.setFinishTime(LocalDateTime.now());
        }

        @Override
        public void onFail(Exception e) {
            healthCheckResultEntity.setResult(HealthCheckStatus.FAILED);
            healthCheckResultEntity.setResultDesc(e.getMessage());
            healthCheckResultEntity.setFinishTime(LocalDateTime.now());
            log.error("healthCheckCallback onFail Id:  ", e);
        }
    }

    @Data
    static class HealthCheckWrapper {

        private BaseSyncBase baseSyncBase;

        private Object user;

        private AbstractHealthCheckService checkService;


        private HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();


        private HealthCheckTypeEnum healthCheckTypeEnum;


        private HealthCheckResultEntity createHealthCheckResultEntity() {
            HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
            healthCheckResultEntity.setClusterId(this.baseSyncBase.getClusterId());
            healthCheckResultEntity.setInterfaces(this.baseSyncBase.getId().toString());
            healthCheckResultEntity.setHealthCheckTypeEnum(this.healthCheckTypeEnum);
            healthCheckResultEntity.setClusterType(this.baseSyncBase.getClusterType());
            healthCheckResultEntity.setBeginTime(LocalDateTime.now());
            return healthCheckResultEntity;
        }

        public boolean equals(Object object) {
            if (object instanceof HealthCheckWrapper) {
                HealthCheckWrapper wrapper = (HealthCheckWrapper) object;
                return this.baseSyncBase.getId().equals(wrapper.getBaseSyncBase().getId()) && this.baseSyncBase.getClusterType()
                    .equals(wrapper.getBaseSyncBase().getClusterType());
            }
            return false;
        }


        public String getKey() {
            return getKey(this.baseSyncBase, this.healthCheckTypeEnum);
        }

    }


}
