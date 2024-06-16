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

import org.apache.eventmesh.dashboard.common.constant.health.HealthCheckTypeConstant;
import org.apache.eventmesh.dashboard.common.enums.StoreType;
import org.apache.eventmesh.dashboard.console.entity.StoreEntity;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache.CheckResult;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.RedisCheck;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4.Rocketmq4BrokerCheck;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4.Rocketmq4NameServerCheck;
import org.apache.eventmesh.dashboard.console.service.DataServiceWrapper;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * HealthService is the manager of all health check services. It is responsible for creating, deleting and executing health check services.<p> In this
 * class there is a {@link HealthExecutor} which is used to execute health check services, and also a map to store all health check services. when the
 * function executeAll is called, health check service will be executed by {@link HealthExecutor}.
 */
@Slf4j
public class HealthService {

    /**
     * class cache used to build healthCheckService instance.<p> key: HealthCheckObjectConfig.SimpleClassName value: HealthCheckService
     *
     * @see HealthCheckObjectConfig
     */
    private static final Map<String, Class<?>> HEALTH_CHECK_CLASS_CACHE = new HashMap<>();

    static {
        setClassCache(RedisCheck.class);
        setClassCache(Rocketmq4BrokerCheck.class);
        setClassCache(Rocketmq4NameServerCheck.class);
    }

    /**
     * This map is used to store HealthExecutor.<p> Outside key is Type(runtime, storage etc.), inside key is the id of type instance(runtimeId,
     * storageId etc.).
     *
     * @see AbstractHealthCheckService
     */
    private final Map<String, Map<Long, AbstractHealthCheckService>> checkServiceMap = new ConcurrentHashMap<>();
    private HealthExecutor healthExecutor;
    private ScheduledThreadPoolExecutor scheduledExecutor;

    private static void setClassCache(Class<?> clazz) {
        HEALTH_CHECK_CLASS_CACHE.put(clazz.getSimpleName(), clazz);
    }

    public Map<String, HashMap<Long, CheckResult>> getCheckResultCacheMap() {
        return healthExecutor.getMemoryCache().getCacheMap();
    }

    public void insertCheckService(List<HealthCheckObjectConfig> configList) {
        configList.forEach(this::insertCheckService);
    }

    public void insertCheckService(HealthCheckObjectConfig config) {
        AbstractHealthCheckService healthCheckService = null;

        try {
            if (Objects.nonNull(config.getSimpleClassName())) {
                Class<?> clazz = HEALTH_CHECK_CLASS_CACHE.get(config.getSimpleClassName());
                healthCheckService = createCheckService(clazz, config);
                // you can pass an object to create a HealthCheckService(not commonly used)
            } else if (Objects.nonNull(config.getCheckClass())) {
                healthCheckService = createCheckService(config.getCheckClass(), config);
                //if simpleClassName and CheckClass are both null, use type(storage) and subType(redis) to create healthCheckService
                //This is the default create method.
                //healthCheckService is annotated with @HealthCheckType(type = "storage", subType = "redis")
            } else if (Objects.nonNull(config.getHealthCheckResourceType()) && Objects.nonNull(
                config.getHealthCheckResourceSubType())) {
                for (Entry<String, Class<?>> entry : HEALTH_CHECK_CLASS_CACHE.entrySet()) {
                    Class<?> clazz = entry.getValue();
                    HealthCheckType annotation = clazz.getAnnotation(HealthCheckType.class);
                    if (Objects.isNull(annotation)) {
                        continue;
                    }
                    if (annotation.type().equals(config.getHealthCheckResourceType()) && annotation.subType()
                        .equals(config.getHealthCheckResourceSubType())) {
                        healthCheckService = createCheckService(clazz, config);
                    }
                }
            }
            // if all above creation method failed
            if (Objects.isNull(healthCheckService)) {
                throw new RuntimeException("No construct method of Health Check Service is found, config is {}" + config);
            }
            insertCheckService(healthCheckService);
        } catch (Exception e) {
            log.error("create healthCheckService failed, healthCheckObjectConfig:{}", config, e);
        }
    }

    public void insertCheckService(AbstractHealthCheckService checkService) {
        Map<Long, AbstractHealthCheckService> subMap = checkServiceMap.computeIfAbsent(checkService.getConfig().getHealthCheckResourceType(),
            k -> new ConcurrentHashMap<>());
        subMap.put(checkService.getConfig().getInstanceId(), checkService);
    }

    public void deleteCheckService(String resourceType, Long resourceId) {
        Map<Long, AbstractHealthCheckService> subMap = checkServiceMap.get(resourceType);
        if (Objects.isNull(subMap)) {
            return;
        }
        subMap.get(resourceId).destroy();
        subMap.remove(resourceId);
        if (subMap.isEmpty()) {
            checkServiceMap.remove(resourceType);
        }
    }

    public void replaceCheckService(List<HealthCheckObjectConfig> configList) {
        checkServiceMap.clear();
        insertCheckService(configList);
    }

    public void createExecutor(HealthDataService dataService, CheckResultCache cache) {
        healthExecutor = new HealthExecutor();
        healthExecutor.setDataService(dataService);
        healthExecutor.setMemoryCache(cache);
    }

    public void executeAll() {
        try {

            healthExecutor.startExecute();

            checkServiceMap.forEach((type, subMap) -> {
                subMap.forEach((typeId, healthCheckService) -> {
                    healthExecutor.execute(healthCheckService);
                });
            });
        } catch (Exception e) {
            log.error("execute health check failed", e);
        }

        healthExecutor.endExecute();
    }

    @NotNull
    private AbstractHealthCheckService createCheckService(Class<?> clazz, HealthCheckObjectConfig config) {
        try {
            Constructor<?> constructor = clazz.getConstructor(HealthCheckObjectConfig.class);
            return (AbstractHealthCheckService) constructor.newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("createCheckService failed", e);
        }
    }

    /**
     * start scheduled execution of health check services
     *
     * @param initialDelay unit is second
     * @param period       unit is second
     */
    public void startScheduledExecution(long initialDelay, int period) {
        if (scheduledExecutor == null) {
            scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        }
        scheduledExecutor.scheduleAtFixedRate(this::executeAll, initialDelay, period, TimeUnit.SECONDS);
    }

    public void startScheduledUpdateConfig(int initialDelay, int period, DataServiceWrapper dataServiceWrapper) {
        if (scheduledExecutor == null) {
            scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        }
        scheduledExecutor.scheduleAtFixedRate(() -> this.updateHealthCheckConfigs(dataServiceWrapper), initialDelay,
            period, TimeUnit.SECONDS);
    }

    public void stopScheduledExecution() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
    }

    public void updateHealthCheckConfigs(DataServiceWrapper dataServiceWrapper) {
        try {
            List<HealthCheckObjectConfig> checkConfigs = new ArrayList<>();
            List<HealthCheckResultEntity> checkResultEntities = new ArrayList<>();
            //TODO add health check service, only storage check is usable for now

            //            List<ClusterEntity> clusters = properties.getDataServiceContainer().getClusterDataService().selectAll();
            //            for (ClusterEntity cluster : clusters) {
            //                checkConfigs.add(HealthCheckObjectConfig.builder()
            //                    .instanceId(cluster.getId())
            //                    .healthCheckResourceType(HealthCheckTypeConstant.HEALTH_CHECK_TYPE_CLUSTER)
            //                    .connectUrl(cluster.getRegistryAddress())
            //                    .build());
            //                checkResultEntities.add(HealthCheckResultEntity.builder()
            //                    .clusterId(cluster.getId())
            //                    .type(1)
            //                    .typeId(cluster.getId())
            //                    .state(4)
            //                    .resultDesc("initializing check client")
            //                    .build());
            //            }
            //
            //            List<RuntimeEntity> runtimes = properties.getDataServiceContainer().getRuntimeDataService().selectAll();
            //            for (RuntimeEntity runtime : runtimes) {
            //                checkConfigs.add(HealthCheckObjectConfig.builder()
            //                    .instanceId(runtime.getId())
            //                    .healthCheckResourceType(HealthCheckTypeConstant.HEALTH_CHECK_TYPE_RUNTIME)
            //                    .connectUrl(runtime.getHost() + ":" + runtime.getPort())
            //                    .build());
            //                checkResultEntities.add(HealthCheckResultEntity.builder()
            //                    .clusterId(runtime.getClusterId())
            //                    .type(2)
            //                    .typeId(runtime.getId())
            //                    .state(4)
            //                    .resultDesc("initializing check client")
            //                    .build());
            //            }
            //
            //            List<TopicEntity> topics = properties.getDataServiceContainer().getTopicDataService().selectAll();
            //            for (TopicEntity topic : topics) {
            //                checkConfigs.add(HealthCheckObjectConfig.builder()
            //                    .instanceId(topic.getId())
            //                    .healthCheckResourceType(HealthCheckTypeConstant.HEALTH_CHECK_TYPE_TOPIC)
            //                    .build());
            //                checkResultEntities.add(HealthCheckResultEntity.builder()
            //                    .clusterId(topic.getClusterId())
            //                    .type(3)
            //                    .typeId(topic.getId())
            //                    .state(4)
            //                    .resultDesc("initializing check client")
            //                    .build());
            //            }

            List<StoreEntity> stores = dataServiceWrapper.getStoreDataService().selectAll();
            for (StoreEntity store : stores) {
                checkConfigs.add(HealthCheckObjectConfig.builder()
                    .instanceId(store.getId())
                    .clusterId(store.getClusterId())
                    .healthCheckResourceType(HealthCheckTypeConstant.HEALTH_CHECK_TYPE_STORAGE)
                    .healthCheckResourceSubType(
                        StoreType.fromNumber(store.getStoreType()).toString())
                    .host(store.getHost())
                    .port(store.getPort())
                    .build());
                HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
                healthCheckResultEntity.setClusterId(store.getClusterId());
                healthCheckResultEntity.setType(4);
                healthCheckResultEntity.setTypeId(store.getId());
                healthCheckResultEntity.setState(4);
                healthCheckResultEntity.setResultDesc("initializing check client");
                checkResultEntities.add(healthCheckResultEntity);
            }

            dataServiceWrapper.getHealthDataService().batchInsertNewCheckResult(checkResultEntities);
            this.replaceCheckService(checkConfigs);
        } catch (Exception e) {
            log.error("updateHealthCheckConfigs error", e);
        }
    }
}
