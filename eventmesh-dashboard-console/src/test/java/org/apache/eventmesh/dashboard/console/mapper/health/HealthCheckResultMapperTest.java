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

package org.apache.eventmesh.dashboard.console.mapper.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:health-test.sql")
class HealthCheckResultMapperTest {

    @Autowired
    private HealthCheckResultMapper healthCheckResultMapper;

    @Test
    public void testSelectById() {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity();
        healthCheckResultEntity.setId(1L);
        healthCheckResultEntity = healthCheckResultMapper.selectById(healthCheckResultEntity);
        assertEquals(1, healthCheckResultEntity.getId());
        assertEquals(0, healthCheckResultEntity.getStatus());
    }

    @Test
    public void testSelectByClusterIdAndTypeAndTypeId() {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity(1L, 1, 1L, "", 1);
        healthCheckResultEntity = healthCheckResultMapper.selectByClusterIdAndTypeAndTypeId(healthCheckResultEntity).get(0);
        assertEquals(1, healthCheckResultEntity.getId());
        assertEquals(0, healthCheckResultEntity.getStatus());
    }

    @Test
    public void testSelectByClusterIdAndType() {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity(1L, 1, 1L, "", 1);
        List<HealthCheckResultEntity> results = healthCheckResultMapper.selectByClusterIdAndType(healthCheckResultEntity);
        assertEquals(2, results.size());
    }

    @Test
    public void testSelectByClusterIdAndTimeRange() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date startDate = dateFormat.parse("2024-02-02 10:56:50");
        Timestamp startTimestamp = new Timestamp(startDate.getTime());
        Date endDate = dateFormat.parse("2024-02-03 10:56:52");
        Timestamp endTimestamp = new Timestamp(endDate.getTime());
        List<HealthCheckResultEntity> results = healthCheckResultMapper.selectByClusterIdAndCreateTimeRange(1L, startTimestamp, endTimestamp);
        assertEquals(4, results.size());
    }

    @Test
    public void testInsert() {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity(5L, 1, 5L, "", 1);
        healthCheckResultMapper.insert(healthCheckResultEntity);
        healthCheckResultEntity = healthCheckResultMapper.selectById(healthCheckResultEntity);
        assertEquals(7, healthCheckResultEntity.getId());
    }

    @Test
    public void testBatchInsert() {
        HealthCheckResultEntity healthCheckResultEntity1 = new HealthCheckResultEntity(1L, 1, 5L, "", 1);
        HealthCheckResultEntity healthCheckResultEntity2 = new HealthCheckResultEntity(1L, 1, 6L, "", 1);
        healthCheckResultMapper.batchInsert(Arrays.asList(healthCheckResultEntity1, healthCheckResultEntity2));
        List<HealthCheckResultEntity> results = healthCheckResultMapper.selectByClusterIdAndType(healthCheckResultEntity1);
        assertEquals(4, results.size());
    }

    @Test
    public void testUpdate() {
        HealthCheckResultEntity healthCheckResultEntity = new HealthCheckResultEntity(1L, 1, 1L, "reason", 0);
        healthCheckResultMapper.update(healthCheckResultEntity);
        healthCheckResultEntity = healthCheckResultMapper.selectByClusterIdAndTypeAndTypeId(healthCheckResultEntity).get(0);
        assertEquals(0, healthCheckResultEntity.getStatus());
    }

    @Test
    public void testBatchUpdate() {
        HealthCheckResultEntity healthCheckResultEntity1 = new HealthCheckResultEntity(1L, 1, 1L, "reason", 0);
        healthCheckResultEntity1.setId(1L);
        HealthCheckResultEntity healthCheckResultEntity2 = new HealthCheckResultEntity(1L, 1, 1L, "reason", 0);
        healthCheckResultEntity2.setId(2L);
        healthCheckResultMapper.batchUpdate(Arrays.asList(healthCheckResultEntity1, healthCheckResultEntity2));
        healthCheckResultEntity1 = healthCheckResultMapper.selectById(healthCheckResultEntity1);
        healthCheckResultEntity2 = healthCheckResultMapper.selectById(healthCheckResultEntity2);

        assertEquals(0, healthCheckResultEntity1.getStatus());
        assertEquals(0, healthCheckResultEntity2.getStatus());
    }

    @Test
    public void testUpdateByClusterIdAndTypeAndTypeId() {
        HealthCheckResultEntity entity1 = new HealthCheckResultEntity(1L, 1, 1L, "reason", 2);
        HealthCheckResultEntity entity2 = new HealthCheckResultEntity(1L, 1, 1L, "reason", 2);
        healthCheckResultMapper.batchInsert(Arrays.asList(entity1, entity2));

        List<HealthCheckResultEntity> toBeUpdate = healthCheckResultMapper.getIdsNeedToBeUpdateByClusterIdAndTypeAndTypeId(
            Arrays.asList(entity1, entity2));

        toBeUpdate.forEach(entity -> entity.setStatus(2));

        healthCheckResultMapper.batchUpdate(toBeUpdate);
        entity1.setId(7L);
        assertEquals(2, healthCheckResultMapper.selectById(entity1).getStatus());
        entity2.setId(1L);
        assertEquals(0, healthCheckResultMapper.selectById(entity2).getStatus());
    }

}