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

package org.apache.eventmesh.dashboard.console.service.connection.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ConnectionDataServiceDatabaseImpl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:connection-test.sql")
@SpringBootTest
public class ConnectionDataServiceDatabaseImplTest {

    @Autowired
    private ConnectionDataServiceDatabaseImpl connectionServiceDatabaseImpl;

    @Test
    public void testGetAllConnectionsByClusterId() {
        List<ConnectionEntity> connectionEntityList = connectionServiceDatabaseImpl.getAllConnectionsByClusterId(1L);
        assertEquals(3, connectionEntityList.size());
    }

    @Test
    public void testGetAllConnections() {
        List<ConnectionEntity> connectionEntityList = connectionServiceDatabaseImpl.getAllConnections();
        assertEquals(6, connectionEntityList.size());
    }
}
