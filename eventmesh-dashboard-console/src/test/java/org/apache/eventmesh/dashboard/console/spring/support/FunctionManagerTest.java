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

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.function.health.HealthService;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataServiceWrapper;
import org.apache.eventmesh.dashboard.console.service.DataServiceWrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
//We don't have runtime and topic check for now, so runtime-test.sql and topic-test.sql is not used
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:client-test.sql", "classpath:connection-test.sql",
    "classpath:connector-test.sql", "classpath:meta-test.sql", "classpath:store-test.sql"})
class FunctionManagerTest {

    @Autowired
    private DataServiceWrapper dataServiceWrapper;

    @Autowired
    private SyncDataServiceWrapper syncDataServiceWrapper;

    private FunctionManager functionManager;

    private HealthService healthService;

    @BeforeEach
    void setUpData() {
        functionManager = new FunctionManager();
        FunctionManagerProperties properties = new FunctionManagerProperties();
        properties.setDataServiceContainer(dataServiceWrapper);
        properties.setSyncDataServiceWrapper(syncDataServiceWrapper);

        functionManager.setProperties(properties);
        healthService = new HealthService();
        healthService.createExecutor(properties.getDataServiceContainer().getHealthDataService(), CheckResultCache.getINSTANCE());
        functionManager.setHealthService(healthService);
    }

    @Test
    void testHealthCheck() {
        functionManager.getHealthService().updateHealthCheckConfigs(dataServiceWrapper);
        healthService.executeAll();
        healthService.executeAll();
        Assertions.assertNotEquals(4, dataServiceWrapper.getHealthDataService().selectAll().size());
    }
}