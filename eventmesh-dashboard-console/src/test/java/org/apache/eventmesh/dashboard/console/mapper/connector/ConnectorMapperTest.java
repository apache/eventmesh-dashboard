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

package org.apache.eventmesh.dashboard.console.mapper.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;

import java.util.ArrayList;
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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:connector-test.sql")
class ConnectorMapperTest {

    @Autowired
    private ConnectorMapper connectorMapper;

    @Test
    public void testSelectById() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(1L);

        connectorEntity = connectorMapper.selectById(connectorEntity);

        assertNotNull(connectorEntity);
        assertEquals(1L, connectorEntity.getClusterId());
        assertEquals("the", connectorEntity.getClassName());
    }

    @Test
    public void testSelectByClusterId() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setClusterId(1L);

        List<ConnectorEntity> results = connectorMapper.selectByClusterId(connectorEntity);

        assertEquals(3, results.size());
        assertEquals("quick", results.get(1).getClassName());
    }

    @Test
    public void testInsert() {
        ConnectorEntity connectorEntity = new ConnectorEntity(1L, "test", "test", "test", 0, 2, "test");
        connectorMapper.insert(connectorEntity);

        assertNotNull(connectorEntity);
        assertEquals(6, connectorEntity.getId());
    }

    @Test
    public void testBatchInsert() {
        ConnectorEntity connectorEntity1 = new ConnectorEntity(1L, "test", "test", "test", 0, 2, "test");
        ConnectorEntity connectorEntity2 = new ConnectorEntity(1L, "test", "test", "test", 0, 2, "test");
        ConnectorEntity connectorEntity3 = new ConnectorEntity(1L, "test", "test", "test", 0, 2, "test");
        List<ConnectorEntity> connectorEntityList = new ArrayList<>();
        connectorEntityList.add(connectorEntity1);
        connectorEntityList.add(connectorEntity2);
        connectorEntityList.add(connectorEntity3);

        connectorMapper.batchInsert(connectorEntityList);

        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setClusterId(1L);
        List<ConnectorEntity> results = connectorMapper.selectByClusterId(connectorEntity);
        assertEquals(6, results.size());
    }

    @Test
    public void testUpdateK8sStatus() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(1L);
        connectorEntity.setPodState(3);
        connectorMapper.updatePodState(connectorEntity);

        connectorEntity = connectorMapper.selectById(connectorEntity);
        assertEquals(3, connectorEntity.getPodState());
    }

    @Test
    public void testUpdateConfigIds() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(1L);
        connectorEntity.setConfigIds("1,3,4,5,6,7");
        connectorMapper.updateConfigIds(connectorEntity);

        connectorEntity = connectorMapper.selectById(connectorEntity);
        assertEquals("1,3,4,5,6,7", connectorEntity.getConfigIds());
    }

    @Test
    public void testDeActiveById() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(1L);
        connectorMapper.deActiveById(connectorEntity);

        connectorEntity = connectorMapper.selectById(connectorEntity);
        assertEquals(1, connectorEntity.getStatus());
    }
}