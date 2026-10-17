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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.model.dto.connection.AddConnectionDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.AddConnectorConfigDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.CreateConnectionDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.GetConnectionListDTO;
import org.apache.eventmesh.dashboard.console.model.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ConnectionDataServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ConnectionServiceTest {

    private ConnectionMapper connectionMapper;

    private ConnectorMapper connectorMapper;

    private RuntimeMapper runtimeMapper;

    private ConnectionDataServiceImpl service;

    @Before
    public void setUp() {
        this.connectionMapper = mock(ConnectionMapper.class);
        this.connectorMapper = mock(ConnectorMapper.class);
        this.runtimeMapper = mock(RuntimeMapper.class);
        this.service = new ConnectionDataServiceImpl(this.connectionMapper, this.connectorMapper, this.runtimeMapper);
    }

    @Test
    public void createConnectionPersistsResolvedConnectorIdsAndRuntimeScope() {
        RuntimeEntity runtime = runtime(31L, 7L);
        ConnectorEntity source = connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "sOuRcE");
        ConnectorEntity sink = connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "SINK");
        ArgumentCaptor<RuntimeEntity> runtimeQuery = ArgumentCaptor.forClass(RuntimeEntity.class);
        ArgumentCaptor<ConnectorEntity> connectorQuery = ArgumentCaptor.forClass(ConnectorEntity.class);
        when(this.runtimeMapper.queryRuntimeEntityById(runtimeQuery.capture())).thenReturn(runtime);
        when(this.connectorMapper.selectByClusterId(connectorQuery.capture())).thenReturn(Arrays.asList(source, sink));
        doAnswer(invocation -> {
            ConnectionEntity entity = invocation.getArgument(0);
            entity.setId(901L);
            return 1L;
        }).when(this.connectionMapper).insert(any(ConnectionEntity.class));
        CreateConnectionDTO request = request(7L, 31L, details());
        request.getAddConnectionDTO().setGroupId(45L);
        request.getAddConnectionDTO().setConnectionDescription("orders route");

        assertTrue(this.service.createConnection(request));

        ArgumentCaptor<ConnectionEntity> captor = ArgumentCaptor.forClass(ConnectionEntity.class);
        verify(this.connectionMapper).insert(captor.capture());
        ConnectionEntity persisted = captor.getValue();
        assertEquals(Long.valueOf(7L), persisted.getClusterId());
        assertEquals(Long.valueOf(31L), persisted.getRuntimeId());
        assertEquals("connector", persisted.getSourceType());
        assertEquals(Long.valueOf(11L), persisted.getSourceId());
        assertEquals("connector", persisted.getSinkType());
        assertEquals(Long.valueOf(12L), persisted.getSinkId());
        assertEquals("orders", persisted.getTopic());
        assertEquals(Long.valueOf(45L), persisted.getGroupId());
        assertEquals("orders route", persisted.getDescription());
        assertEquals(Long.valueOf(31L), runtimeQuery.getValue().getId());
        assertEquals(Long.valueOf(7L), connectorQuery.getValue().getClusterId());
    }

    @Test
    public void createConnectionAcceptsPortBoundaries() {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(runtime(31L, 7L));
        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(Arrays.asList(
            connector(11L, 7L, "source", "SourceClass", "source-host", 1, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 65535, "Sink")));
        doAnswer(invocation -> {
            invocation.<ConnectionEntity>getArgument(0).setId(901L);
            return 1L;
        }).when(this.connectionMapper).insert(any(ConnectionEntity.class));
        AddConnectionDTO boundaryPorts = details();
        boundaryPorts.setSourcePort(1);
        boundaryPorts.setSinkPort(65535);
        CreateConnectionDTO request = request(7L, 31L, boundaryPorts);
        request.setAddConnectorConfigDTO(null);

        assertTrue(this.service.createConnection(request));
    }

    @Test
    public void createConnectionRejectsRuntimeOutsideRequestedClusterBeforeInsert() {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(runtime(31L, 8L));

        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
        verify(this.connectorMapper, never()).selectByClusterId(any(ConnectorEntity.class));
    }

    @Test
    public void createConnectionRejectsInactiveOrMissingRuntime() {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
    }

    @Test
    public void createConnectionRejectsMissingOrAmbiguousConnectorMatches() {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(runtime(31L, 7L));
        ConnectorEntity source = connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source");
        ConnectorEntity sink = connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink");
        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(Collections.singletonList(source));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(Arrays.asList(source, source, sink));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(Arrays.asList(source, sink, sink));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
    }

    @Test
    public void createConnectionRejectsConnectorCandidatesWithPartialIdentityOrWrongRole() {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(runtime(31L, 7L));
        List<ConnectorEntity> candidates = Arrays.asList(
            connector(11L, 7L, "source", "SourceClass", "other-host", 8081, "Source"),
            connector(12L, 7L, "source", "SourceClass", "source-host", 8082, "Source"),
            connector(13L, 7L, "source", "OtherClass", "source-host", 8081, "Source"),
            connector(14L, 7L, "other-name", "SourceClass", "source-host", 8081, "Source"),
            connector(15L, 7L, "source", "SourceClass", "source-host", 8081, "Sink"),
            connector(16L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink"));
        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(candidates);

        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, details())));

        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
    }

    @Test
    public void createConnectionRejectsEachSingleFieldConnectorMismatch() {
        AddConnectionDTO sourceName = details();
        assertConnectorMismatch(sourceName, connector(11L, 7L, "wrong-name", "SourceClass", "source-host", 8081, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink"));
        AddConnectionDTO sourceClass = details();
        assertConnectorMismatch(sourceClass, connector(11L, 7L, "source", "wrong-class", "source-host", 8081, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink"));
        AddConnectionDTO sourceHost = details();
        assertConnectorMismatch(sourceHost, connector(11L, 7L, "source", "SourceClass", "wrong-host", 8081, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink"));
        AddConnectionDTO sourcePort = details();
        assertConnectorMismatch(sourcePort, connector(11L, 7L, "source", "SourceClass", "source-host", 8083, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink"));
        AddConnectionDTO sinkName = details();
        assertConnectorMismatch(sinkName, connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source"),
            connector(12L, 7L, "wrong-name", "SinkClass", "sink-host", 8082, "Sink"));
        AddConnectionDTO sinkClass = details();
        assertConnectorMismatch(sinkClass, connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source"),
            connector(12L, 7L, "sink", "wrong-class", "sink-host", 8082, "Sink"));
        AddConnectionDTO sinkHost = details();
        assertConnectorMismatch(sinkHost, connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "wrong-host", 8082, "Sink"));
        AddConnectionDTO sinkPort = details();
        assertConnectorMismatch(sinkPort, connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source"),
            connector(12L, 7L, "sink", "SinkClass", "sink-host", 8083, "Sink"));
    }

    @Test
    public void createConnectionRejectsInvalidInputsAndConfigMutationsBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(0L, 31L, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(null, 31L, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(-1L, 31L, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 0L, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, null, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, -1L, details())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, null)));
        assertInvalidDetails(value -> value.setSourceName(null));
        assertInvalidDetails(value -> value.setSourceClass(null));
        assertInvalidDetails(value -> value.setSourceHost(null));
        assertInvalidDetails(value -> value.setSourcePort(null));
        assertInvalidDetails(value -> value.setSourcePort(0));
        assertInvalidDetails(value -> value.setSourcePort(65536));
        assertInvalidDetails(value -> value.setSinkName(" "));
        assertInvalidDetails(value -> value.setSinkClass(""));
        assertInvalidDetails(value -> value.setSinkHost(null));
        assertInvalidDetails(value -> value.setSinkPort(null));
        assertInvalidDetails(value -> value.setSinkPort(0));
        assertInvalidDetails(value -> value.setSinkPort(65536));
        assertInvalidDetails(value -> value.setTopicName(" "));
        CreateConnectionDTO changedConfigs = request(7L, 31L, details());
        changedConfigs.setAddConnectorConfigDTO(new AddConnectorConfigDTO(Collections.singletonList(new ConfigEntity()),
            Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(changedConfigs));
        changedConfigs.setAddConnectorConfigDTO(new AddConnectorConfigDTO(Collections.emptyList(),
            Collections.singletonList(new ConfigEntity())));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(changedConfigs));

        verify(this.runtimeMapper, never()).queryRuntimeEntityById(any(RuntimeEntity.class));
        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
    }

    @Test
    public void insertRequiresMapperSuccessAndGeneratedId() {
        assertThrows(IllegalArgumentException.class, () -> this.service.insert(null));
        ConnectionEntity connection = new ConnectionEntity();
        connection.setId(71L);
        when(this.connectionMapper.insert(connection)).thenReturn(0L);
        assertThrows(IllegalStateException.class, () -> this.service.insert(connection));

        when(this.connectionMapper.insert(connection)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> this.service.insert(connection));

        when(this.connectionMapper.insert(connection)).thenReturn(1L);
        connection.setId(null);
        assertThrows(IllegalStateException.class, () -> this.service.insert(connection));

        ConnectionEntity inserted = new ConnectionEntity();
        doAnswer(invocation -> {
            invocation.<ConnectionEntity>getArgument(0).setId(72L);
            return 1L;
        }).when(this.connectionMapper).insert(any(ConnectionEntity.class));
        assertEquals(Long.valueOf(72L), this.service.insert(inserted));
    }

    @Test
    public void listQueriesUseRequestedClusterAndHydrateOnlySameClusterConnectors() {
        ConnectionEntity stored = new ConnectionEntity();
        stored.setClusterId(7L);
        stored.setRuntimeId(31L);
        stored.setSourceType("connector");
        stored.setSourceId(11L);
        stored.setSinkType("connector");
        stored.setSinkId(12L);
        stored.setTopic("orders");
        stored.setStatus(1L);
        ConnectorEntity source = connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source");
        ConnectorEntity foreignSink = connector(12L, 8L, "foreign", "SinkClass", "sink-host", 8082, "Sink");
        when(this.connectionMapper.selectToFrontByClusterId(any(ConnectionEntity.class))).thenReturn(Collections.singletonList(stored));
        when(this.connectorMapper.selectById(any(ConnectorEntity.class))).thenAnswer(invocation -> {
            ConnectorEntity query = invocation.getArgument(0);
            return query.getId().equals(11L) ? source : foreignSink;
        });
        GetConnectionListDTO request = new GetConnectionListDTO(7L, "ord");

        List<ConnectionListVO> result = this.service.getConnectionToFrontByCluster(7L, request);

        ArgumentCaptor<ConnectionEntity> queryCaptor = ArgumentCaptor.forClass(ConnectionEntity.class);
        verify(this.connectionMapper).selectToFrontByClusterId(queryCaptor.capture());
        assertEquals(Long.valueOf(7L), queryCaptor.getValue().getClusterId());
        assertEquals("ord", queryCaptor.getValue().getTopic());
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(11L), result.get(0).getSourceConnectorId());
        assertEquals("source", result.get(0).getSourceConnectorName());
        assertNull(result.get(0).getSinkConnectorId());
        assertNull(result.get(0).getSinkConnectorName());
        assertEquals("orders", result.get(0).getTopicName());
    }

    @Test
    public void serviceQueriesDelegateAndConfigTemplatesExposeUnsupportedSchemaBoundary() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setClusterId(7L);
        List<ConnectionEntity> all = Collections.singletonList(entity);
        when(this.connectionMapper.selectAll()).thenReturn(all);
        ArgumentCaptor<ConnectionEntity> connectionQuery = ArgumentCaptor.forClass(ConnectionEntity.class);
        when(this.connectionMapper.selectByClusterId(connectionQuery.capture())).thenReturn(all);
        ConnectorEntity connector = new ConnectorEntity();
        connector.setId(19L);
        ArgumentCaptor<ConnectorEntity> connectorQuery = ArgumentCaptor.forClass(ConnectorEntity.class);
        when(this.connectorMapper.selectById(connectorQuery.capture())).thenReturn(connector);
        when(this.connectorMapper.selectAll()).thenReturn(Collections.singletonList(connector));

        assertSame(all, this.service.getAllConnections());
        assertSame(all, this.service.getAllConnectionsByClusterId(7L));
        assertEquals(Long.valueOf(7L), connectionQuery.getValue().getClusterId());
        assertSame(connector, this.service.getConnectorById(19L));
        assertEquals(Long.valueOf(19L), connectorQuery.getValue().getId());
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectorById(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectorById(-1L));
        assertThrows(IllegalArgumentException.class, () -> this.service.getAllConnectionsByClusterId(0L));
        assertThrows(UnsupportedOperationException.class, () -> this.service.getConnectorConfigsByClassAndVersion("some.Class", "1.0"));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectorConfigsByClassAndVersion(" ", "1.0"));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectorConfigsByClassAndVersion("some.Class", null));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectorBusinessType("Other"));
    }

    @Test
    public void businessTypeQueryFiltersCaseInsensitivelyAndReturnsDistinctNonblankClasses() {
        ConnectorEntity source = connector(1L, 7L, "source", "SourceClass", "host", 10, "source");
        ConnectorEntity duplicateSource = connector(2L, 7L, "source2", "SourceClass", "host", 11, "SOURCE");
        ConnectorEntity blankSource = connector(3L, 7L, "source3", " ", "host", 12, "Source");
        ConnectorEntity sink = connector(4L, 7L, "sink", "SinkClass", "host", 13, "Sink");
        when(this.connectorMapper.selectAll()).thenReturn(Arrays.asList(source, duplicateSource, blankSource, sink));

        assertEquals(Collections.singletonList("SourceClass"), this.service.getConnectorBusinessType("SoUrCe"));
        assertEquals(Collections.singletonList("SinkClass"), this.service.getConnectorBusinessType("sink"));
    }

    @Test
    public void connectionListRejectsMismatchedClusterAndOmitsMissingOrNonConnectorLinks() {
        GetConnectionListDTO wrongScope = new GetConnectionListDTO(8L, null);
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectionToFrontByCluster(7L, wrongScope));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectionToFrontByCluster(7L, null));
        assertThrows(IllegalArgumentException.class, () -> this.service.getConnectionToFrontByCluster(0L, wrongScope));
        assertThrows(IllegalArgumentException.class,
            () -> this.service.getConnectionToFrontByCluster(0L, new GetConnectionListDTO(0L, null)));
        assertThrows(IllegalArgumentException.class,
            () -> this.service.getConnectionToFrontByCluster(null, new GetConnectionListDTO(null, null)));
        assertThrows(IllegalArgumentException.class,
            () -> this.service.getConnectionToFrontByCluster(-1L, new GetConnectionListDTO(-1L, null)));

        ConnectionEntity missingLink = new ConnectionEntity();
        missingLink.setClusterId(7L);
        missingLink.setSourceType("client");
        missingLink.setSourceId(15L);
        missingLink.setSinkType("connector");
        missingLink.setSinkId(16L);
        when(this.connectionMapper.selectToFrontByClusterId(any(ConnectionEntity.class)))
            .thenReturn(Collections.singletonList(missingLink));
        when(this.connectorMapper.selectById(any(ConnectorEntity.class))).thenReturn(null);

        ConnectionListVO result = this.service.getConnectionToFrontByCluster(7L, new GetConnectionListDTO(7L, null)).get(0);

        assertNull(result.getSourceConnectorId());
        assertNull(result.getSinkConnectorId());
        assertNull(result.getStatus());
        verify(this.connectorMapper, never()).selectById(argThat(query -> Long.valueOf(15L).equals(query.getId())));
    }

    @Test
    public void connectionListHydratesBothActiveSameClusterConnectors() {
        ConnectionEntity stored = new ConnectionEntity();
        stored.setClusterId(7L);
        stored.setSourceType("connector");
        stored.setSourceId(11L);
        stored.setSinkType("connector");
        stored.setSinkId(12L);
        stored.setStatus(1L);
        ConnectorEntity source = connector(11L, 7L, "source", "SourceClass", "source-host", 8081, "Source");
        ConnectorEntity sink = connector(12L, 7L, "sink", "SinkClass", "sink-host", 8082, "Sink");
        when(this.connectionMapper.selectToFrontByClusterId(any(ConnectionEntity.class))).thenReturn(Collections.singletonList(stored));
        when(this.connectorMapper.selectById(any(ConnectorEntity.class))).thenAnswer(invocation -> {
            ConnectorEntity query = invocation.getArgument(0);
            return query.getId().equals(11L) ? source : sink;
        });

        ConnectionListVO result = this.service.getConnectionToFrontByCluster(7L, new GetConnectionListDTO(7L, null)).get(0);

        assertEquals(Long.valueOf(11L), result.getSourceConnectorId());
        assertEquals("source", result.getSourceConnectorName());
        assertEquals(Long.valueOf(12L), result.getSinkConnectorId());
        assertEquals("sink", result.getSinkConnectorName());
        assertEquals("SinkClass", result.getSinkClass());
        assertEquals("SourceClass", result.getSourceClass());
        assertEquals(Integer.valueOf(1), result.getStatus());
    }

    @Test
    public void connectionListOmitsConnectorReferenceWithoutId() {
        ConnectionEntity stored = new ConnectionEntity();
        stored.setClusterId(7L);
        stored.setSourceType("connector");
        stored.setSourceId(null);
        stored.setSinkType("client");
        stored.setSinkId(12L);
        when(this.connectionMapper.selectToFrontByClusterId(any(ConnectionEntity.class))).thenReturn(Collections.singletonList(stored));

        ConnectionListVO result = this.service.getConnectionToFrontByCluster(7L, new GetConnectionListDTO(7L, null)).get(0);

        assertNull(result.getSourceConnectorId());
        assertNull(result.getSinkConnectorId());
        verify(this.connectorMapper, never()).selectById(any(ConnectorEntity.class));
    }

    private static RuntimeEntity runtime(Long id, Long clusterId) {
        RuntimeEntity runtime = new RuntimeEntity();
        runtime.setId(id);
        runtime.setClusterId(clusterId);
        return runtime;
    }

    private static ConnectorEntity connector(Long id, Long clusterId, String name, String className,
        String host, Integer port, String type) {
        ConnectorEntity connector = new ConnectorEntity();
        connector.setId(id);
        connector.setClusterId(clusterId);
        connector.setName(name);
        connector.setClassName(className);
        connector.setHost(host);
        connector.setPort(port);
        connector.setType(type);
        return connector;
    }

    private static CreateConnectionDTO request(Long clusterId, Long runtimeId, AddConnectionDTO details) {
        CreateConnectionDTO request = new CreateConnectionDTO();
        request.setClusterId(clusterId);
        request.setRuntimeId(runtimeId);
        request.setAddConnectionDTO(details);
        request.setAddConnectorConfigDTO(new AddConnectorConfigDTO(Collections.emptyList(), Collections.emptyList()));
        return request;
    }

    private static AddConnectionDTO details() {
        AddConnectionDTO details = new AddConnectionDTO();
        details.setSourceName("source");
        details.setSourceClass("SourceClass");
        details.setSourceHost("source-host");
        details.setSourcePort(8081);
        details.setSinkName("sink");
        details.setSinkClass("SinkClass");
        details.setSinkHost("sink-host");
        details.setSinkPort(8082);
        details.setTopicName("orders");
        return details;
    }

    private void assertInvalidDetails(Consumer<AddConnectionDTO> mutation) {
        AddConnectionDTO invalidDetails = details();
        mutation.accept(invalidDetails);
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, invalidDetails)));
    }

    private void assertConnectorMismatch(AddConnectionDTO requestDetails, ConnectorEntity first, ConnectorEntity second) {
        when(this.runtimeMapper.queryRuntimeEntityById(any(RuntimeEntity.class))).thenReturn(runtime(31L, 7L));
        when(this.connectorMapper.selectByClusterId(any(ConnectorEntity.class))).thenReturn(Arrays.asList(first, second));
        assertThrows(IllegalArgumentException.class, () -> this.service.createConnection(request(7L, 31L, requestDetails)));
        verify(this.connectionMapper, never()).insert(any(ConnectionEntity.class));
    }
}
