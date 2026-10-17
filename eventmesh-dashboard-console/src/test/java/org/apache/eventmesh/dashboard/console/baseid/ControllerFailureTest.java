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

package org.apache.eventmesh.dashboard.console.baseid;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.eventmesh.dashboard.console.controller.ConnectionController;
import org.apache.eventmesh.dashboard.console.controller.cluster.AclController;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.model.dto.connection.CreateConnectionDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.GetConnectionListDTO;
import org.apache.eventmesh.dashboard.console.model.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.cluster.AclService;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ControllerFailureTest {

    @Test
    public void selectAclReturnsTheStoredEntity() {
        AclService service = mock(AclService.class);
        AclController controller = new AclController();
        AclEntity request = new AclEntity();
        AclEntity stored = new AclEntity();
        when(service.selectById(request)).thenReturn(stored);
        setAclService(controller, service);

        AclEntity result = controller.selectAcl(request);

        assertSame(stored, result);
        verify(service).selectById(request);
    }

    @Test
    public void createConnectionReturnsSuccessOnlyWhenServiceCreatesConnection() {
        ConnectionDataService service = mock(ConnectionDataService.class);
        ConnectionController controller = new ConnectionController(service);
        CreateConnectionDTO request = new CreateConnectionDTO();
        when(service.createConnection(request)).thenReturn(true);

        org.junit.Assert.assertEquals("success", controller.createConnection(request));
        verify(service).createConnection(request);
    }

    @Test
    public void createConnectionFalseResultRaisesFailure() {
        ConnectionDataService service = mock(ConnectionDataService.class);
        ConnectionController controller = new ConnectionController(service);
        CreateConnectionDTO request = new CreateConnectionDTO();
        when(service.createConnection(request)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> controller.createConnection(request));
    }

    @Test
    public void createConnectionPropagatesServiceFailure() {
        ConnectionDataService service = mock(ConnectionDataService.class);
        ConnectionController controller = new ConnectionController(service);
        CreateConnectionDTO request = new CreateConnectionDTO();
        IllegalArgumentException failure = new IllegalArgumentException("invalid connection");
        when(service.createConnection(request)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> controller.createConnection(request)));
    }

    @Test
    public void connectionQueriesDelegateAndReturnServiceResults() {
        ConnectionDataService service = mock(ConnectionDataService.class);
        ConnectionController controller = new ConnectionController(service);
        List<String> businessTypes = Collections.singletonList("Sink");
        List<ConfigEntity> configs = Collections.singletonList(new ConfigEntity());
        List<ConnectionListVO> connections = Collections.singletonList(new ConnectionListVO());
        ConnectorEntity connector = new ConnectorEntity();
        GetConnectionListDTO request = new GetConnectionListDTO(73L, "orders");
        when(service.getConnectorBusinessType("Sink")).thenReturn(businessTypes);
        when(service.getConnectorConfigsByClassAndVersion("example.Connector", "1.0")).thenReturn(configs);
        when(service.getConnectionToFrontByCluster(73L, request)).thenReturn(connections);
        when(service.getConnectorById(19L)).thenReturn(connector);

        assertSame(businessTypes, controller.getConnectorBusinessType("Sink"));
        assertSame(configs, controller.getConnectorConfigsByClassAndVersion("1.0", "example.Connector"));
        assertSame(connections, controller.getConnectionList(request));
        assertSame(connector, controller.getConnectorDetail(19L));
    }

    @Test
    public void invalidConnectionQueryReturnsBadRequestWithMessage() throws Exception {
        ConnectionDataService service = mock(ConnectionDataService.class);
        when(service.getConnectorBusinessType("Other")).thenThrow(new IllegalArgumentException("type must be Source or Sink"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service)).build();

        mvc.perform(get("/cluster/connection/getConnectorBusinessType").param("type", "Other"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("type must be Source or Sink"));
    }

    @Test
    public void unsupportedConnectorConfigurationQueryReturnsNotImplemented() throws Exception {
        ConnectionDataService service = mock(ConnectionDataService.class);
        when(service.getConnectorConfigsByClassAndVersion("example.Connector", "1.0"))
            .thenThrow(new UnsupportedOperationException("Connector configuration templates are not supported by the current schema"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service)).build();

        mvc.perform(get("/cluster/connection/getConnectorConfigs")
                .param("classType", "example.Connector").param("version", "1.0"))
            .andExpect(status().isNotImplemented())
            .andExpect(content().string("Connector configuration templates are not supported by the current schema"));
    }

    private void setAclService(AclController controller, AclService service) {
        try {
            java.lang.reflect.Field field = AclController.class.getDeclaredField("aclService");
            field.setAccessible(true);
            field.set(controller, service);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
