package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.HealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.RedisCheck;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4.Rocketmq4BrokerCheck;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4.Rocketmq4NameServerCheck;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private static final Map<String, Class<?>> HEALTH_CHECK_CLASS_CACHE = new HashMap<>();

    private static final Map<String, Class<?>> HEALTH_CLUSTER_CHECK_CLASS_CACHE = new HashMap<>();

    static {
        setClassCache(RedisCheck.class);
        setClassCache(Rocketmq4BrokerCheck.class);
        setClassCache(Rocketmq4NameServerCheck.class);
    }

    private final Map<String, HealthCheckWrapper> checkServiceMap = new ConcurrentHashMap<>();

    private final Map<Long, ClusterHealthCheckService> clusterHealthCheckServiceMap = new ConcurrentHashMap<>();

    private static void setClassCache(Class<?> clazz) {
        Map<String, Class<?>> map =
            Objects.equals(clazz, AbstractHealthCheckService.class) ? HEALTH_CHECK_CLASS_CACHE : HEALTH_CLUSTER_CHECK_CLASS_CACHE;
        map.put(clazz.getSimpleName(), clazz);
    }

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(32, 32, 5, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    @Setter
    private HealthDataService dataService;

    public void register(List<RuntimeEntity> runtimeEntityList) {
        runtimeEntityList.forEach(v -> {
            this.register(v, this.toHealthCheckObjectConfig(v));
        });
    }

    public void register(RuntimeEntity runtimeEntity) {
        this.register(runtimeEntity, this.toHealthCheckObjectConfig(runtimeEntity));
    }

    public void register(RuntimeEntity runtimeEntity, HealthCheckObjectConfig config) {
        try {
            AbstractHealthCheckService healthCheckService = (AbstractHealthCheckService) this.createCheckService(config);
            HealthCheckWrapper healthCheckWrapper = this.createHealthCheckWrapper(runtimeEntity, config, healthCheckService, HealthCheckTypeEnum.PING);
            this.checkServiceMap.put(healthCheckWrapper.getKey(), healthCheckWrapper);
            if (config.getClusterType().isHealthTopic()) {
                healthCheckService = (AbstractHealthCheckService) this.createCheckService(config);
                healthCheckWrapper = this.createHealthCheckWrapper(runtimeEntity, config, healthCheckService, HealthCheckTypeEnum.TOPIC);
                this.checkServiceMap.put(healthCheckWrapper.getKey(), healthCheckWrapper);
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    private HealthCheckWrapper createHealthCheckWrapper(RuntimeEntity runtimeEntity, HealthCheckObjectConfig config,
        AbstractHealthCheckService healthCheckService, HealthCheckTypeEnum healthCheckTypeEnum) {
        HealthCheckWrapper healthCheckWrapper = new HealthCheckWrapper();
        healthCheckWrapper.setHealthCheckTypeEnum(healthCheckTypeEnum);
        healthCheckWrapper.setRuntimeEntity(runtimeEntity);
        healthCheckWrapper.setHealthCheckObjectConfig(config);
        healthCheckWrapper.setCheckService(healthCheckService);
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();

        healthCheckWrapper.setHealthCheckResultEntity(healthCheckResultEntity);
        return healthCheckWrapper;
    }

    public void unRegister(RuntimeEntity runtimeEntity) {
        ClusterHealthCheckService clusterHealthCheckService = this.clusterHealthCheckServiceMap.remove(runtimeEntity.getClusterId());
        if (Objects.nonNull(clusterHealthCheckService)) {
            clusterHealthCheckService.unRegister(this.toHealthCheckObjectConfig(runtimeEntity));
        }
        this.checkServiceMap.remove(getKey(runtimeEntity, HealthCheckTypeEnum.PING));
        this.checkServiceMap.remove(getKey(runtimeEntity, HealthCheckTypeEnum.TOPIC));
    }

    private HealthCheckObjectConfig toHealthCheckObjectConfig(RuntimeEntity runtimeEntity) {
        HealthCheckObjectConfig healthCheckObjectConfig = new HealthCheckObjectConfig();
        return healthCheckObjectConfig;
    }


    /**
     * @param config
     * @return
     */
    HealthCheckService createCheckService(HealthCheckObjectConfig config) {
        HealthCheckService healthCheckService = null;
        if (Objects.nonNull(config.getSimpleClassName())) {
            Class<?> clazz = HEALTH_CHECK_CLASS_CACHE.get(config.getSimpleClassName());
            healthCheckService = createCheckService(clazz, config);
            // you can pass an object to create a HealthCheckService(not commonly used)
        } else if (Objects.nonNull(config.getCheckClass())) {
            healthCheckService = createCheckService(config.getCheckClass(), config);
        }
        // if all above creation method failed
        if (Objects.isNull(healthCheckService)) {
            throw new RuntimeException("No construct method of Health Check Service is found, config is {}" + config);
        }
        return healthCheckService;
    }


    /**
     * @param clazz  CheckService
     * @param config
     * @return
     */
    private AbstractHealthCheckService createCheckService(Class<?> clazz, HealthCheckObjectConfig config) {
        try {
            Constructor<?> constructor = clazz.getConstructor(HealthCheckObjectConfig.class);
            return (AbstractHealthCheckService) constructor.newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("createCheckService failed", e);
        }
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

        } finally {
            try {
                dataService.batchUpdateCheckResultByClusterIdAndTypeAndTypeId(healthCheckResultEntityList);
            } catch (Exception e) {
                log.error("batchUpdateCheckResultByClusterIdAndTypeAndTypeId failed", e);
            }
        }
    }

    public static String getKey(RuntimeEntity runtimeEntity, HealthCheckTypeEnum healthCheckTypeEnum) {
        return healthCheckTypeEnum.toString() + "-" + runtimeEntity.getId();
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
            log.error("healthCheckCallback onFail Id:  "+, e);
        }
    }

    @Data
    static class HealthCheckWrapper {

        private RuntimeEntity runtimeEntity;

        private Object user;

        private AbstractHealthCheckService checkService;


        private HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();

        private HealthCheckObjectConfig healthCheckObjectConfig;

        private HealthCheckTypeEnum healthCheckTypeEnum;


        private HealthCheckResultEntity createHealthCheckResultEntity() {
            healthCheckResultEntity = new HealthCheckResultEntity();
            healthCheckResultEntity.setClusterId(this.runtimeEntity.getClusterId());
            healthCheckResultEntity.setInterfaces(this.runtimeEntity.getId().toString());
            healthCheckResultEntity.setHealthCheckTypeEnum(this.healthCheckTypeEnum);
            healthCheckResultEntity.setClusterType(this.runtimeEntity.getClusterType());
            healthCheckResultEntity.setBeginTime(LocalDateTime.now());
        }

        public boolean equals(Object object) {
            if (object instanceof HealthCheckWrapper) {
                HealthCheckWrapper wrapper = (HealthCheckWrapper) object;
                return this.runtimeEntity.getId().equals(wrapper.getRuntimeEntity().getId()) && this.runtimeEntity.getClusterType()
                    .equals(wrapper.getRuntimeEntity().getClusterType());
            }
            return false;
        }


        public String getKey() {
            return Health2Service.getKey(this.runtimeEntity, this.healthCheckTypeEnum);
        }

    }


}
