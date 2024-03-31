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

package org.apache.eventmesh.dashboard.console.mapper.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;

import java.util.Arrays;
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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:connection-test.sql")
class ConnectionMapperTest {

    @Autowired
    private ConnectionMapper connectionMapper;

    @Test
    public void testSelectAll() {
        assertEquals(6, connectionMapper.selectAll().size());
    }

    @Test
    public void testSelectByClusterId() {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(1L);
        assertEquals(3, connectionMapper.selectByClusterId(connectionEntity).size());
    }

    @Test
    public void testSelectByClusterIdSourceTypeAndSourceId() {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(1L);
        connectionEntity.setSourceId(1L);
        connectionEntity.setSourceType("connector");
        List<ConnectionEntity> results = connectionMapper.selectByClusterIdSourceTypeAndSourceId(connectionEntity);
        assertEquals(1, results.size());
        assertEquals("connector", results.get(0).getSinkType());
        assertEquals(1, results.get(0).getSinkId());
    }

    @Test
    public void testSelectByClusterIdSinkTypeAndSinkId() {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(1L);
        connectionEntity.setSinkId(2L);
        connectionEntity.setSinkType("connector");
        List<ConnectionEntity> results = connectionMapper.selectByClusterIdSinkTypeAndSinkId(connectionEntity);
        assertEquals(1, results.size());
        assertEquals("connector", results.get(0).getSourceType());
        assertEquals(2, results.get(0).getSourceId());
    }

    @Test
    public void testInsert() {
        ConnectionEntity connectionEntity = new ConnectionEntity(1L, "connector", 1L, "connector", 2L, 1L, 0, "topic", 3L, null, "description");
        connectionMapper.insert(connectionEntity);
        assertEquals(7, connectionMapper.selectAll().size());
    }

    @Test
    public void testBatchInsert() {
        ConnectionEntity connectionEntity1 = new ConnectionEntity(1L, "connector", 1L, "connector", 2L, 1L, 0, "topic", 3L, null, "description");
        ConnectionEntity connectionEntity2 = new ConnectionEntity(1L, "connector", 1L, "connector", 2L, 1L, 0, "topic", 3L, null, "description");
        connectionMapper.batchInsert(Arrays.asList(connectionEntity1, connectionEntity2));
        assertEquals(8, connectionMapper.selectAll().size());
    }

    @Test
    public void testEndConnectionById() {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setId(1L);
        connectionMapper.endConnectionById(connectionEntity);
        List<ConnectionEntity> results = connectionMapper.selectAll();
        ConnectionEntity result = results.get(0);
        assertEquals(1, result.getStatus());
        assertNotNull(result.getEndTime());
    }

    @Test
    public void testBatchEndConnection() {
        ConnectionEntity connectionEntity1 = new ConnectionEntity();
        connectionEntity1.setId(1L);
        ConnectionEntity connectionEntity2 = new ConnectionEntity();
        connectionEntity2.setId(2L);
        connectionMapper.batchEndConnectionById(Arrays.asList(connectionEntity1, connectionEntity2));

        List<ConnectionEntity> all = connectionMapper.selectAll();
        assertEquals(1, all.get(0).getStatus());
        assertEquals(1, all.get(1).getStatus());
        assertNotEquals(all.get(0).getCreateTime(), all.get(0).getEndTime());
    }
}