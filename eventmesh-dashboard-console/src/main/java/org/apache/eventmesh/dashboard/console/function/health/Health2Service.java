package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.HealthCheckService;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;

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


    private static void setClassCache(Class<?> clazz) {
        HealthCheckType checkType = clazz.getAnnotation(HealthCheckType.class);
        if (Objects.isNull(checkType)) {
            return;
        }
        Map<ClusterType, Class<?>> map =
            Objects.equals(checkType.healthType(), HealthCheckTypeEnum.PING) ? HEALTH_PING_CHECK_CLASS_CACHE : HEALTH_TOPIC_CHECK_CLASS_CACHE;

        for (ClusterType clusterType : checkType.clusterType()) {
            map.put(clusterType, clazz);
        }
    }


    private final Map<String, HealthCheckWrapper> checkServiceMap = new ConcurrentHashMap<>();

    @Deprecated
    private final Map<Long, ClusterHealthCheckService> clusterHealthCheckServiceMap = new ConcurrentHashMap<>();


    private final ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(32, 32, 5, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "health-manager-" + counter.incrementAndGet());
            }
        });

    @Setter
    private HealthDataService dataService;


    public void register(BaseSyncBase baseSyncBase) {
        try {
            this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.PING);
            if (baseSyncBase.getClusterType().isHealthTopic()) {
                this.createHealthCheckWrapper(baseSyncBase, HealthCheckTypeEnum.TOPIC);
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
    }

    @Deprecated
    public void unRegisterCluster(Long clusterId) {
        this.clusterHealthCheckServiceMap.remove(clusterId);
    }


    void createHealthCheckWrapper(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        Map<ClusterType, Class<?>> map =
            Objects.equals(healthCheckTypeEnum, HealthCheckTypeEnum.PING) ? HEALTH_PING_CHECK_CLASS_CACHE : HEALTH_TOPIC_CHECK_CLASS_CACHE;
        Class<?> clazz = map.get(baseSyncBase.getClusterType());
        AbstractHealthCheckService<Object> abstractHealthCheckService = SDKManage.getInstance().createAbstractClientInfo(clazz, baseSyncBase);
        HealthCheckWrapper healthCheckWrapper =
            this.createHealthCheckWrapper(baseSyncBase, abstractHealthCheckService, healthCheckTypeEnum);
        this.checkServiceMap.put(healthCheckWrapper.getKey(), healthCheckWrapper);
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
        long startTime = System.currentTimeMillis();
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
            boolean await = countDownLatch.await(3, TimeUnit.SECONDS);
            log.info("await ia {} downLatch count {}", await, countDownLatch.getCount());
            log.info(" startup cost {} ms", System.currentTimeMillis() - startTime);
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


    public String getKey(BaseSyncBase baseSyncBase, HealthCheckTypeEnum healthCheckTypeEnum) {
        return healthCheckTypeEnum.toString() + "-" + baseSyncBase.getId();
    }

    static class DefaultHealthCheckCallback implements HealthCheckCallback {

        private HealthCheckWrapper healthCheckWrapper;

        private CountDownLatch countDownLatch;

        private HealthCheckResultEntity healthCheckResultEntity;

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
            healthCheckResultEntity.setFinishTime(LocalDateTime.now());
            countDownLatch.countDown();
            log.error("healthCheckCallback onFail Id:  ", e);
        }
    }

    @Data
    class HealthCheckWrapper {

        private BaseSyncBase baseSyncBase;

        private AbstractHealthCheckService<Object> checkService;


        private HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();


        private HealthCheckTypeEnum healthCheckTypeEnum;


        private HealthCheckResultEntity createHealthCheckResultEntity() {
            HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
            healthCheckResultEntity.setClusterId(this.baseSyncBase.getClusterId());
            healthCheckResultEntity.setInterfaces(this.baseSyncBase.getId().toString());
            healthCheckResultEntity.setHealthCheckTypeEnum(this.healthCheckTypeEnum);
            healthCheckResultEntity.setClusterType(this.baseSyncBase.getClusterType());
            healthCheckResultEntity.setBeginTime(LocalDateTime.now());
            this.healthCheckResultEntity = healthCheckResultEntity;
            return healthCheckResultEntity;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof HealthCheckWrapper) {
                HealthCheckWrapper wrapper = (HealthCheckWrapper) object;
                return this.baseSyncBase.getId().equals(wrapper.getBaseSyncBase().getId()) && this.baseSyncBase.getClusterType()
                    .equals(wrapper.getBaseSyncBase().getClusterType());
            }
            return false;
        }


        public String getKey() {
            return Health2Service.this.getKey(this.baseSyncBase, this.healthCheckTypeEnum);
        }

    }


}
