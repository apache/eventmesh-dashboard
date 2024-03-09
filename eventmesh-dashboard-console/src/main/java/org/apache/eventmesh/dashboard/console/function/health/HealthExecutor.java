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

import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache.CheckResult;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthExecutor {

    @Setter
    private HealthDataService dataService;

    /**
     * memory cache is used to store real-time health check result.
     */
    @Getter
    @Setter
    private CheckResultCache memoryCache;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * execute function is where health check services work.
     *
     * @param service The health check service to be executed.
     */

    public void execute(AbstractHealthCheckService service) {
        final long startTime = System.currentTimeMillis();
        //TODO: execute is called by a ScheduledThreadPoolExecutor,
        // when called, it should check if current service should doCheck(check service check rate can be dynamically configured).
        try {
            memoryCache.update(service.getConfig().getHealthCheckResourceType(), service.getConfig().getInstanceId(), HealthCheckStatus.CHECKING, "",
                null, service.getConfig());
            //The callback interface is used to pass the processing methods for checking success and failure.
            executorService.submit(() -> service.doCheck(new HealthCheckCallback() {
                @Override
                public void onSuccess() {
                    //when the health check is successful, the result is updated to the memory cache.
                    Long latency = System.currentTimeMillis() - startTime;
                    HealthCheckStatus status =
                        latency > service.getConfig().getRequestTimeoutMillis() ? HealthCheckStatus.TIMEOUT : HealthCheckStatus.PASSED;
                    memoryCache.update(service.getConfig().getHealthCheckResourceType(), service.getConfig().getInstanceId(),
                        status, "Health check succeed.", latency
                    );
                }

                @Override
                public void onFail(Exception e) {
                    //when the health check fails, the result is updated to the memory cache, passing in the exception message.
                    log.error("Health check failed for reason: {}. Service config is {}", e, service.getConfig());
                    memoryCache.update(service.getConfig().getHealthCheckResourceType(), service.getConfig().getInstanceId(),
                        HealthCheckStatus.FAILED, e.getMessage(),
                        System.currentTimeMillis() - startTime);
                }
            }));

        } catch (Exception e) {
            log.error("Health check failed for reason: {}. Service config is {}", e, service.getConfig());
            memoryCache.update(service.getConfig().getHealthCheckResourceType(), service.getConfig().getInstanceId(), HealthCheckStatus.FAILED,
                e.getMessage(),
                System.currentTimeMillis() - startTime);
        }
    }

    /**
     * this function should be called before any actual execute behaviour.<br> It will check the execution result of the last check cycle in the
     * memory cache, set tasks that haven't finished status to time out and update the database.
     */
    public void startExecute() {
        ArrayList<HealthCheckResultEntity> resultList = new ArrayList<>();
        memoryCache.getCacheMap().forEach((type, subMap) -> {
            subMap.forEach((instanceId, result) -> {
                if (result.getStatus() == HealthCheckStatus.CHECKING) {
                    result.setStatus(HealthCheckStatus.TIMEOUT);
                }
                addToResultList(result, resultList);
            });
        });
        if (!resultList.isEmpty()) {
            dataService.batchUpdateCheckResultByClusterIdAndTypeAndTypeId(resultList);
        }
    }

    /**
     * this function should be called after all actual execute behaviour.<br> It will insert the result of this check cycle into the database. At this
     * point the status of the tasks may be CHECKING, they will be updated on the next startExecute.
     */
    public void endExecute() {
        ArrayList<HealthCheckResultEntity> resultList = new ArrayList<>();
        memoryCache.getCacheMap().forEach((type, subMap) -> {
            subMap.forEach((instanceId, result) -> {
                addToResultList(result, resultList);
            });
        });
        dataService.batchInsertHealthCheckResult(resultList);
    }

    /**
     * Helper function to add a CheckResult to the resultList.
     *
     * @param result     memory cached result object.
     * @param resultList entity list to be inserted into the database.
     */
    private void addToResultList(CheckResult result, ArrayList<HealthCheckResultEntity> resultList) {
        HealthCheckResultEntity newEntity = new HealthCheckResultEntity();
        newEntity.setClusterId(result.getConfig().getClusterId());
        newEntity.setType(HealthCheckType.toNumber(result.getConfig().getHealthCheckResourceType()));
        newEntity.setTypeId(result.getConfig().getInstanceId());
        newEntity.setResultDesc(result.getResultDesc());
        newEntity.setStatus(result.getStatus().getNumber());

        resultList.add(newEntity);
    }

}
