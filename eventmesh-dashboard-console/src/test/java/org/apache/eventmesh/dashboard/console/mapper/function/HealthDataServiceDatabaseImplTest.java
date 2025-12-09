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

package org.apache.eventmesh.dashboard.console.mapper.function;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
public class HealthDataServiceDatabaseImplTest {


    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;


    @Test
    public void test_batchInsertNewCheckResult() {
        List<HealthCheckResultEntity> healthCheckResultEntityList = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        for (long i = 1; i <= 100; i++) {

            HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
            healthCheckResultEntity.setClusterType(ClusterType.EVENTMESH_JVM_RUNTIME);
            healthCheckResultEntity.setClusterId(i % 10);
            healthCheckResultEntity.setProtocol("");
            healthCheckResultEntity.setType(2);
            healthCheckResultEntity.setTypeId(i);
            healthCheckResultEntity.setAddress("127.0.0.1:9898");
            healthCheckResultEntity.setHealthCheckType(HealthCheckTypeEnum.PING);
            healthCheckResultEntity.setResult(HealthCheckStatus.ING);
            healthCheckResultEntity.setResultDesc("");
            healthCheckResultEntity.setBeginTime(localDateTime);

            healthCheckResultEntityList.add(healthCheckResultEntity);
        }
        this.healthCheckResultMapper.batchInsert(healthCheckResultEntityList);
    }
}
