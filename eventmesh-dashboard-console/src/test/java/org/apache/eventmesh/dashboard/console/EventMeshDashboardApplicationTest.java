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

package org.apache.eventmesh.dashboard.console;

import org.apache.eventmesh.dashboard.common.enums.RecordStatus;

import java.sql.Timestamp;

import static org.mockito.ArgumentMatchers.any;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeResult;
import org.apache.eventmesh.dashboard.console.cache.ClusterCache;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.cluster.RuntimeSyncFromClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.registry.RegistryDataService;
import org.apache.eventmesh.dashboard.service.remoting.MetaRemotingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@ActiveProfiles("dev")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "USE eventmesh_dashboard", scripts = {"classpath:eventmesh-dashboard.sql"})
@TestPropertySource(properties = {
    "function.healthCheck.doCheck.initialDelay=5",
    "function.healthCheck.doCheck.period=10",
    "function.healthCheck.updateConfig.initialDelay=5",
    "function.healthCheck.updateConfig.period=20",
    "function.sync.enable=true",
    "function.sync.initialDelay=5",
    "function.sync.period=10",
    "function.sync.toDb.runtime=true"
})
class EventMeshDashboardApplicationTest {

    @Autowired
    private RegistryDataService registryDataService;

    @Test
    public void IntegrationTest() throws InterruptedException {
        //To make a test, add cluster with registry address 175.27.155.139:8848 in web endpoint

        if (Objects.equals(System.getenv("APPLICATION_TEST"), "on")) {
            EventMeshDashboardApplication.main(new String[]{});
            Thread.sleep(1000 * 60 * 10);
        }
    }
}