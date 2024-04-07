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

package org.apache.eventmesh.dashboard.console.mapper.client;

import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.client.ClientEntity;

import java.util.List;

import org.junit.jupiter.api.Assertions;
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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:client-test.sql")
class ClientMapperTest {

    @Autowired
    private ClientMapper clientMapper;

    @Test
    public void testSelectById() {
        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setId(1L);
        ClientEntity result = clientMapper.selectById(clientEntity);
        Assertions.assertEquals("java", result.getLanguage());
        Assertions.assertEquals(3, result.getClusterId());
    }

    @Test
    public void testSelectByClusterId() {
        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setClusterId(3L);
        List<ClientEntity> results = clientMapper.selectByClusterId(clientEntity);
        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("java", results.get(0).getLanguage());
        Assertions.assertEquals("go", results.get(2).getLanguage());
    }

    @Test
    public void testInsert() {
        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setHost("127.0.0.1");
        clientEntity.setClusterId(4L);
        clientEntity.setName("clientName");
        clientEntity.setDescription("");
        clientEntity.setPid(1L);
        clientEntity.setPort(8080);
        clientEntity.setStatusEntity(RecordStatus.ACTIVE);
        clientEntity.setConfigIds("");
        clientEntity.setLanguage("rust");
        clientEntity.setPlatform("");
        clientEntity.setProtocol("http");
        clientMapper.insert(clientEntity);

        ClientEntity result = clientMapper.selectById(clientEntity);
        Assertions.assertEquals("127.0.0.1", result.getHost());

        Assertions.assertEquals(2, clientMapper.selectByClusterId(clientEntity).size());
    }

    @Test
    public void testDeactivate() {
        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setId(1L);
        clientMapper.deactivate(clientEntity);
        ClientEntity result = clientMapper.selectById(clientEntity);
        Assertions.assertEquals(0, result.getStatus());
        Assertions.assertNotEquals(result.getCreateTime(), result.getEndTime());
    }
}