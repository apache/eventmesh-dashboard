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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClientDataService;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;
import org.apache.eventmesh.dashboard.console.service.connector.ConnectorDataService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

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
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:client-test.sql", "classpath:connector-test.sql",
    "classpath:connection-test.sql"})
class ConnectionMetadataHandlerToDbImplTest {

    @Autowired
    MetadataHandler<ConnectionMetadata> connectionMetadataMetadataHandler;

    @Autowired
    ConnectionDataService connectionDataService;

    @Autowired
    ConnectorDataService connectorDataService;

    @Autowired
    ClientDataService clientDataService;

    @Test
    public void testNewConnectorNewClientNewConnection() {
        ConnectionMetadata connectionMetadata = new ConnectionMetadata();
        connectionMetadata.setSourceType("connector");
        connectionMetadata.setSourceId(0L);
        connectionMetadata.setSourceHost("192.168.1.1");
        connectionMetadata.setSourcePort(8888);
        connectionMetadata.setSourceName("");
        connectionMetadata.setSinkType("client");
        connectionMetadata.setSinkHost("");
        connectionMetadata.setSinkPort(0);
        connectionMetadata.setSinkName("");
        connectionMetadata.setSinkId(0L);
        connectionMetadata.setRuntimeId(0L);
        connectionMetadata.setTopic("");
        connectionMetadata.setGroupId(0L);
        connectionMetadata.setDescription("");
        connectionMetadata.setRegistryAddress("");
        connectionMetadata.setClusterId(0L);

        connectionMetadataMetadataHandler.addMetadata(connectionMetadata);

        assertEquals(7, connectionDataService.getAllConnections().size());
        assertEquals(6, connectorDataService.selectAll().size());
        assertEquals(5, clientDataService.selectAll().size());
    }

    @Test
    public void testOldConnectorNewClientNewConnection() {
        ConnectionMetadata connectionMetadata = new ConnectionMetadata();

        connectionMetadata.setSourceType("connector");
        connectionMetadata.setSourceHost("192.168.3.1");
        connectionMetadata.setSourcePort(8888);
        connectionMetadata.setSourceName("");
        connectionMetadata.setSinkType("client");
        connectionMetadata.setSinkHost("");
        connectionMetadata.setSinkPort(0);
        connectionMetadata.setSinkName("");
        connectionMetadata.setRuntimeId(0L);
        connectionMetadata.setTopic("");
        connectionMetadata.setGroupId(0L);
        connectionMetadata.setDescription("");
        connectionMetadata.setRegistryAddress("");
        connectionMetadata.setClusterId(0L);

        connectionMetadataMetadataHandler.addMetadata(connectionMetadata);

        List<ConnectionEntity> connections = connectionDataService.getAllConnections();
        List<ConnectorEntity> connectors = connectorDataService.selectAll();
        List<ClientEntity> clients = clientDataService.selectAll();

        assertEquals(7, connections.size());
        assertEquals(5, connectors.size());
        assertEquals(5, clients.size());

        assertEquals(2, connections.get(connections.size() - 1).getSourceId().intValue());
        assertEquals(5, connections.get(connections.size() - 1).getSinkId().intValue());
    }
}