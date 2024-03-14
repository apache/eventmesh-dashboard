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

import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    HealthService healthService = new HealthService();

    @Mock
    private HealthDataService healthDataService;

    @Mock
    private CheckResultCache checkResultCache;

    @BeforeEach
    void init() {
        healthService.createExecutor(healthDataService, checkResultCache);
    }

    @Test
    void testInsertCheckServiceWithAnnotation() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(2L);
        config.setHealthCheckResourceType("storage");
        config.setHealthCheckResourceSubType("redis");
        config.setConnectUrl("redis://localhost:6379");
        healthService.insertCheckService(config);
    }

    @Test
    void testInsertCheckServiceWithSimpleClassName() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(1L);
        config.setSimpleClassName("RedisCheck");
        config.setHealthCheckResourceType("storage");
        config.setHealthCheckResourceSubType("redis");
        config.setClusterId(1L);
        config.setConnectUrl("redis://localhost:6379");
        healthService.insertCheckService(config);
    }

    @Test
    void testInsertCheckServiceWithClass() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(1L);
        config.setHealthCheckResourceType("storage");
        config.setHealthCheckResourceSubType("redis");
        config.setClusterId(1L);
        config.setCheckClass(TestHealthCheckService.class);
        healthService.insertCheckService(config);
    }

    @Test
    void testDeleteCheckService() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(1L);
        config.setHealthCheckResourceType("storage");
        config.setClusterId(1L);
        config.setCheckClass(TestHealthCheckService.class);
        healthService.insertCheckService(config);
        healthService.deleteCheckService("storage", 1L);
    }

    @Test
    void testExecuteAll() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(1L);
        config.setHealthCheckResourceType("storage");
        config.setClusterId(1L);
        config.setCheckClass(TestHealthCheckService.class);
        healthService.insertCheckService(config);
        healthService.executeAll();
    }

    public static class TestHealthCheckService extends AbstractHealthCheckService {


        public TestHealthCheckService(HealthCheckObjectConfig healthCheckObjectConfig) {
            super(healthCheckObjectConfig);
        }

        @Override
        public void doCheck(HealthCheckCallback callback) {
            callback.onSuccess();
        }

        @Override
        public void init() {

        }

        @Override
        public void destroy() {

        }
    }
}