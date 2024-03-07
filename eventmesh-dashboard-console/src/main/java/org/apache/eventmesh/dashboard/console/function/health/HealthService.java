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

import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache.CheckResult;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.RedisCheck;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * HealthService is the manager of all health check services. It is responsible for creating, deleting and executing health check services.<br> In
 * this class there is a {@link HealthExecutor} which is used to execute health check services, and also a map to store all health check services.
 * when the function executeAll is called, health check service will be executed by {@link HealthExecutor}.
 */
@Slf4j
public class HealthService {

    private HealthExecutor healthExecutor;

    /**
     * class cache used to build healthCheckService instance.<br> key: HealthCheckObjectConfig.SimpleClassName value: HealthCheckService
     *
     * @see HealthCheckObjectConfig
     */
    private static final Map<String, Class<?>> HEALTH_CHECK_CLASS_CACHE = new HashMap<>();

    static {
        setClassCache(RedisCheck.class);
    }

    private static void setClassCache(Class<?> clazz) {
        HEALTH_CHECK_CLASS_CACHE.put(clazz.getSimpleName(), clazz);
    }

    /**
     * This map is used to store HealthExecutor.<br> Outside key is Type(runtime, storage etc.), inside key is the id of type instance(runtimeId,
     * storageId etc.).
     *
     * @see AbstractHealthCheckService
     */
    private final Map<String, Map<Long, AbstractHealthCheckService>> checkServiceMap = new ConcurrentHashMap<>();

    private ScheduledThreadPoolExecutor scheduledExecutor;

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
                    if (annotation != null && annotation.type().equals(config.getHealthCheckResourceType()) && annotation.subType()
                        .equals(config.getHealthCheckResourceSubType())) {
                        healthCheckService = createCheckService(clazz, config);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("create healthCheckService failed, healthCheckObjectConfig:{}", config, e);
        }

        // if all above creation method failed
        if (Objects.isNull(healthCheckService)) {
            throw new RuntimeException("No construct method of Health Check Service is found, config is {}" + config);
        }
        insertCheckService(healthCheckService);
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


    public void createExecutor(HealthDataService dataService, CheckResultCache cache) {
        healthExecutor = new HealthExecutor();
        healthExecutor.setDataService(dataService);
        healthExecutor.setMemoryCache(cache);
    }

    public void executeAll() {
        healthExecutor.startExecute();

        checkServiceMap.forEach((type, subMap) -> {
            subMap.forEach((typeId, healthCheckService) -> {
                healthExecutor.execute(healthCheckService);
            });
        });

        healthExecutor.endExecute();
    }

    private AbstractHealthCheckService createCheckService(Class<?> clazz, HealthCheckObjectConfig config)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> constructor = clazz.getConstructor(HealthCheckObjectConfig.class);
        return (AbstractHealthCheckService) constructor.newInstance(config);
    }

    /**
     * start scheduled execution of health check services
     *
     * @param initialDelay unit is second
     * @param period       unit is second
     */
    public void startScheduledExecution(long initialDelay, long period) {
        if (scheduledExecutor == null) {
            scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        }
        scheduledExecutor.scheduleAtFixedRate(this::executeAll, initialDelay, period, TimeUnit.SECONDS);
    }

    public void stopScheduledExecution() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
    }
}
