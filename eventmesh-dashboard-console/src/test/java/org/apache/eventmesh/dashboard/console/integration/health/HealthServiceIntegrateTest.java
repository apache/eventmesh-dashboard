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

package org.apache.eventmesh.dashboard.console.integration.health;

import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.function.health.HealthService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
public class HealthServiceIntegrateTest {

    HealthService healthService = new HealthService();

    @Autowired
    private HealthDataService healthDataService;

    private final CheckResultCache checkResultCache = new CheckResultCache();

    @BeforeEach
    void init() {
        healthService.createExecutor(healthDataService, checkResultCache);
    }

    @Test
    void testStorageRedis() throws InterruptedException {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setClusterId(1L);
        config.setInstanceId(1L);
        config.setHealthCheckResourceType("storage");
        config.setHealthCheckResourceSubType("redis");
        config.setConnectUrl("redis://localhost:6379");
        healthService.insertCheckService(config);
        healthService.executeAll();
        Thread.sleep(1000);
        healthService.executeAll();

        HealthCheckResultEntity queryEntity = new HealthCheckResultEntity();
        queryEntity.setClusterId(1L);
        queryEntity.setType(HealthCheckType.STORAGE.getNumber());
        queryEntity.setTypeId(1L);
        List<HealthCheckResultEntity> results = healthDataService.queryHealthCheckResultByClusterIdAndTypeAndTypeId(queryEntity);
        Assertions.assertEquals(2, results.size());
    }
}
