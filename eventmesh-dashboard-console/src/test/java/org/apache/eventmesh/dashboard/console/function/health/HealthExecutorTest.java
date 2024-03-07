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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.console.service.health.impl.HealthDataServiceDatabaseImpl;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
class HealthExecutorTest {

    private HealthExecutor healthExecutor = new HealthExecutor();
    private CheckResultCache memoryCache = new CheckResultCache();

    @Autowired
    HealthDataServiceDatabaseImpl healthDataService;

    @Mock
    AbstractHealthCheckService successHealthCheckService;

    @Mock
    AbstractHealthCheckService failHealthCheckService;

    @Mock
    AbstractHealthCheckService timeoutHealthCheckService;

    @BeforeEach
    public void initMock() {
        Mockito.lenient().doAnswer((Answer<Void>) invocation -> {
            HealthCheckCallback callback = invocation.getArgument(0);
            callback.onSuccess();
            return null;
        }).when(successHealthCheckService).doCheck(any(HealthCheckCallback.class));
        Mockito.lenient().doAnswer((Answer<Void>) invocation -> {
            HealthCheckCallback callback = invocation.getArgument(0);
            callback.onFail(new RuntimeException("TestRuntimeException: This check is designed to be failed. Ignore This!"));
            return null;
        }).when(failHealthCheckService).doCheck(any(HealthCheckCallback.class));
        Mockito.lenient().doAnswer((Answer<Void>) invocation -> {
            HealthCheckCallback callback = invocation.getArgument(0);
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return;
                }
                callback.onFail(new RuntimeException("TestRuntimeException"));
            });
            return null;
        }).when(timeoutHealthCheckService).doCheck(any(HealthCheckCallback.class));

        healthExecutor.setDataService(healthDataService);
        healthExecutor.setMemoryCache(memoryCache);
        HealthCheckObjectConfig config1 = new HealthCheckObjectConfig();
        config1.setInstanceId(1L);
        config1.setHealthCheckResourceType("storage");
        config1.setHealthCheckResourceSubType("redis");
        config1.setConnectUrl("redis://localhost:6379");
        config1.setSimpleClassName("RedisCheck");
        config1.setClusterId(1L);
        Mockito.lenient().when(successHealthCheckService.getConfig()).thenReturn(config1);
        Mockito.lenient().when(timeoutHealthCheckService.getConfig()).thenReturn(config1);
        HealthCheckObjectConfig config2 = new HealthCheckObjectConfig();
        config2.setInstanceId(2L);
        config2.setHealthCheckResourceType("storage");
        config2.setHealthCheckResourceSubType("redis");
        config2.setConnectUrl("redis://localhost:6379");
        config2.setSimpleClassName("RedisCheck");
        config2.setClusterId(1L);
        Mockito.lenient().when(failHealthCheckService.getConfig()).thenReturn(config2);
    }

    @Test
    public void testExecute() throws InterruptedException {
        healthExecutor.execute(successHealthCheckService);
        healthExecutor.execute(failHealthCheckService);
        Thread.sleep(1000);
        assertEquals(2, memoryCache.getCacheMap().get("storage").size());
        assertNotEquals(memoryCache.getCacheMap().get("storage").get(1L).getStatus(), memoryCache.getCacheMap().get("storage").get(2L).getStatus());
    }


    @Test
    public void testEndExecute() {
        healthExecutor.execute(successHealthCheckService);
        healthExecutor.execute(failHealthCheckService);
        healthExecutor.endExecute();
        HealthCheckResultEntity query = new HealthCheckResultEntity();
        query.setClusterId(1L);
        query.setType(HealthCheckType.STORAGE.getNumber());
        query.setTypeId(2L);
        assertNotNull(healthDataService.queryHealthCheckResultByClusterIdAndTypeAndTypeId(query).get(0).getStatus());
    }

    @Test
    public void testStartExecute() {
        healthExecutor.execute(successHealthCheckService);
        healthExecutor.execute(failHealthCheckService);
        //to test startExecute(), we need to call endExecute() first
        healthExecutor.endExecute();
        healthExecutor.startExecute();
        HealthCheckResultEntity query = new HealthCheckResultEntity();
        query.setClusterId(1L);
        query.setType(HealthCheckType.STORAGE.getNumber());
        query.setTypeId(1L);
        assertEquals(1, healthDataService.queryHealthCheckResultByClusterIdAndTypeAndTypeId(query).get(0).getStatus());
    }

    @Test
    public void testTimeout() {
        healthExecutor.execute(timeoutHealthCheckService);
        healthExecutor.endExecute();
        healthExecutor.startExecute();

        HealthCheckResultEntity query = new HealthCheckResultEntity();
        query.setClusterId(1L);
        query.setType(HealthCheckType.STORAGE.getNumber());
        query.setTypeId(1L);
        assertEquals(HealthCheckStatus.TIMEOUT.getNumber(),
            healthDataService.queryHealthCheckResultByClusterIdAndTypeAndTypeId(query).get(0).getStatus());
    }

    @Test
    public void testFull() throws InterruptedException {
        healthExecutor.startExecute();
        healthExecutor.execute(successHealthCheckService);
        healthExecutor.execute(failHealthCheckService);
        healthExecutor.endExecute();
        Thread.sleep(1000);
        healthExecutor.startExecute();
        healthExecutor.execute(successHealthCheckService);
        healthExecutor.execute(failHealthCheckService);
        healthExecutor.endExecute();
        HealthCheckResultEntity query = new HealthCheckResultEntity();
        query.setClusterId(1L);
        query.setType(HealthCheckType.STORAGE.getNumber());
        query.setTypeId(1L);
        assertEquals(2, healthDataService.queryHealthCheckResultByClusterIdAndTypeAndTypeId(query).size());
    }

}