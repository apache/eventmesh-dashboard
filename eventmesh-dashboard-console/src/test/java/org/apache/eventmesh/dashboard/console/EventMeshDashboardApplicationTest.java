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

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
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


    @Test
    public void IntegrationTest() throws InterruptedException {
        //To make a test, add cluster with registry address ip:port in web endpoint

        if (Objects.equals(System.getenv("APPLICATION_TEST"), "on")) {
            EventMeshDashboardApplication.main(new String[] {});
            Thread.sleep(1000 * 60 * 10);
        }
    }
}