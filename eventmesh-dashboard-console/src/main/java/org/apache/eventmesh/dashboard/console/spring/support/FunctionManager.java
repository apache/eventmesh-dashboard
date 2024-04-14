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

package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.constant.health.HealthCheckTypeConstant;
import org.apache.eventmesh.dashboard.common.enums.StoreType;
import org.apache.eventmesh.dashboard.console.config.FunctionManagerConfigs;
import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.function.health.HealthService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.function.metadata.MetadataManager;
import org.apache.eventmesh.dashboard.console.function.metadata.MetadataServiceWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.MetadataServiceWrapper.SingleMetadataServiceWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * FunctionManager is in charge of tasks such as scheduled health checks
 */
@Slf4j
public class FunctionManager {

    @Setter
    private FunctionManagerProperties properties;

    @Setter
    private FunctionManagerConfigs configs;

    @Setter
    @Getter
    private HealthService healthService;

    @Setter
    @Getter
    private MetadataManager metadataManager;

    public void initFunctionManager() {
        //Health Check
        healthService = new HealthService();
        healthService.createExecutor(properties.getDataServiceContainer().getHealthDataService(), CheckResultCache.getINSTANCE());
        startScheduledUpdateConfig(configs.getHealthCheck().getUpdateConfig().getInitialDelay(),
            configs.getHealthCheck().getUpdateConfig().getPeriod());
        healthService.startScheduledExecution(configs.getHealthCheck().getDoCheck().getInitialDelay(),
            configs.getHealthCheck().getDoCheck().getPeriod());

        metadataManager = new MetadataManager();
        setUpSyncMetadataManager();
        metadataManager.init(configs.getSync().getInitialDelay(), configs.getSync().getPeriod());
    }

    void updateHealthCheckConfigs() {
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

            List<StoreEntity> stores = properties.getDataServiceContainer().getStoreDataService().selectAll();
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
                checkResultEntities.add(HealthCheckResultEntity.builder()
                    .clusterId(store.getClusterId())
                    .type(4)
                    .typeId(store.getId())
                    .state(4)
                    .resultDesc("initializing check client")
                    .build());
            }

            properties.getDataServiceContainer().getHealthDataService().batchInsertNewCheckResult(checkResultEntities);
            healthService.replaceCheckService(checkConfigs);
        } catch (Exception e) {
            log.error("updateHealthCheckConfigs error", e);
        }
    }

    private void startScheduledUpdateConfig(int initialDelay, int period) {
        ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(4);
        scheduledExecutor.scheduleAtFixedRate(this::updateHealthCheckConfigs, initialDelay, period, TimeUnit.SECONDS);
    }

    private void setUpSyncMetadataManager() {
        MetadataServiceWrapper metadataServiceWrapper = new MetadataServiceWrapper();
        SingleMetadataServiceWrapper singleMetadataServiceWrapper = SingleMetadataServiceWrapper.builder()
            .syncService(properties.getSyncDataServiceWrapper().getRuntimeSyncFromClusterService())
            .handler(properties.getMetadataHandlerWrapper().getRuntimeMetadataHandlerToDb()).build();
        metadataServiceWrapper.setServiceToDb(singleMetadataServiceWrapper);
        metadataManager.addMetadataService(metadataServiceWrapper);
    }
}
