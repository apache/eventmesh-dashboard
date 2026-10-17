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


package org.apache.eventmesh.dashboard.console.service.cluster.impl;

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
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Persists connections between existing connectors in the requested cluster. */
@Service
public class ConnectionDataServiceImpl implements ConnectionDataService {

    private final ConnectionMapper connectionMapper;

    private final ConnectorMapper connectorMapper;

    private final RuntimeMapper runtimeMapper;

    public ConnectionDataServiceImpl(ConnectionMapper connectionMapper, ConnectorMapper connectorMapper, RuntimeMapper runtimeMapper) {
        this.connectionMapper = connectionMapper;
        this.connectorMapper = connectorMapper;
        this.runtimeMapper = runtimeMapper;
    }

    @Override
    public ConnectorEntity getConnectorById(Long connectorId) {
        requirePositiveId(connectorId, "connectorId");
        ConnectorEntity query = new ConnectorEntity();
        query.setId(connectorId);
        return this.connectorMapper.selectById(query);
    }

    @Override
    public List<String> getConnectorBusinessType(String type) {
        Assert.isTrue("Source".equalsIgnoreCase(type) || "Sink".equalsIgnoreCase(type), "type must be Source or Sink");
        return this.connectorMapper.selectAll().stream()
            .filter(connector -> type.equalsIgnoreCase(connector.getType()))
            .map(ConnectorEntity::getClassName)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<ConnectionEntity> getAllConnectionsByClusterId(Long clusterId) {
        requirePositiveId(clusterId, "clusterId");
        ConnectionEntity query = new ConnectionEntity();
        query.setClusterId(clusterId);
        return this.connectionMapper.selectByClusterId(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConnection(CreateConnectionDTO request) {
        Assert.notNull(request, "Connection request is required");
        requirePositiveId(request.getClusterId(), "clusterId");
        requirePositiveId(request.getRuntimeId(), "runtimeId");
        AddConnectionDTO details = request.getAddConnectionDTO();
        Assert.notNull(details, "addConnectionDTO is required");
        Assert.hasText(details.getTopicName(), "topicName is required");
        validateConnector(details.getSourceName(), details.getSourceClass(), details.getSourceHost(), details.getSourcePort(), "Source");
        validateConnector(details.getSinkName(), details.getSinkClass(), details.getSinkHost(), details.getSinkPort(), "Sink");
        validateUnchangedConfigs(request.getAddConnectorConfigDTO());

        RuntimeEntity runtimeQuery = new RuntimeEntity();
        runtimeQuery.setId(request.getRuntimeId());
        RuntimeEntity runtime = this.runtimeMapper.queryRuntimeEntityById(runtimeQuery);
        Assert.notNull(runtime, "runtimeId must identify an active runtime");
        Assert.isTrue(Objects.equals(request.getClusterId(), runtime.getClusterId()), "runtimeId must belong to clusterId");

        ConnectorEntity connectorQuery = new ConnectorEntity();
        connectorQuery.setClusterId(request.getClusterId());
        List<ConnectorEntity> connectors = this.connectorMapper.selectByClusterId(connectorQuery);
        ConnectorEntity source = findConnector(connectors, details.getSourceName(), details.getSourceClass(),
            details.getSourceHost(), details.getSourcePort(), "Source");
        ConnectorEntity sink = findConnector(connectors, details.getSinkName(), details.getSinkClass(),
            details.getSinkHost(), details.getSinkPort(), "Sink");

        ConnectionEntity connection = new ConnectionEntity();
        connection.setClusterId(request.getClusterId());
        connection.setRuntimeId(runtime.getId());
        connection.setSourceType("connector");
        connection.setSourceId(source.getId());
        connection.setSinkType("connector");
        connection.setSinkId(sink.getId());
        connection.setTopic(details.getTopicName());
        connection.setGroupId(details.getGroupId());
        connection.setDescription(details.getConnectionDescription());
        this.insert(connection);
        return true;
    }

    @Override
    public List<ConnectionEntity> getAllConnections() {
        return this.connectionMapper.selectAll();
    }

    @Override
    public List<ConnectionListVO> getConnectionToFrontByCluster(Long clusterId, GetConnectionListDTO request) {
        requirePositiveId(clusterId, "clusterId");
        Assert.notNull(request, "Connection query is required");
        Assert.isTrue(Objects.equals(clusterId, request.getClusterId()), "Query clusterId must match the requested cluster");
        ConnectionEntity query = new ConnectionEntity();
        query.setClusterId(clusterId);
        query.setTopic(request.getTopicName());
        return this.connectionMapper.selectToFrontByClusterId(query).stream()
            .map(this::toListView)
            .collect(Collectors.toList());
    }

    @Override
    public List<ConfigEntity> getConnectorConfigsByClassAndVersion(String classType, String version) {
        Assert.hasText(classType, "classType is required");
        Assert.hasText(version, "version is required");
        // The current schema has no connector-class template association or CONNECTOR metadata kind.
        throw new UnsupportedOperationException("Connector configuration templates are not available");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(ConnectionEntity connection) {
        Assert.notNull(connection, "Connection is required");
        Long affected = this.connectionMapper.insert(connection);
        if (affected == null || affected <= 0 || connection.getId() == null) {
            throw new IllegalStateException("Connection insert did not return a persisted id");
        }
        return connection.getId();
    }

    private ConnectionListVO toListView(ConnectionEntity connection) {
        ConnectorEntity source = displayConnector(connection.getSourceType(), connection.getSourceId(), connection.getClusterId());
        ConnectorEntity sink = displayConnector(connection.getSinkType(), connection.getSinkId(), connection.getClusterId());
        return new ConnectionListVO(sink == null ? null : sink.getId(), sink == null ? null : sink.getName(),
            source == null ? null : source.getId(), source == null ? null : source.getName(),
            sink == null ? null : sink.getClassName(), source == null ? null : source.getClassName(),
            connection.getStatus() == null ? null : Math.toIntExact(connection.getStatus()), connection.getTopic());
    }

    private ConnectorEntity displayConnector(String type, Long id, Long clusterId) {
        if (!"connector".equals(type) || id == null) {
            return null;
        }
        ConnectorEntity connector = this.getConnectorById(id);
        return connector != null && Objects.equals(clusterId, connector.getClusterId()) ? connector : null;
    }

    private static ConnectorEntity findConnector(List<ConnectorEntity> connectors, String name, String className,
        String host, Integer port, String role) {
        List<ConnectorEntity> matches = connectors.stream()
            .filter(connector -> role.equalsIgnoreCase(connector.getType()))
            .filter(connector -> Objects.equals(name, connector.getName()) && Objects.equals(className, connector.getClassName()))
            .filter(connector -> Objects.equals(host, connector.getHost()) && Objects.equals(port, connector.getPort()))
            .collect(Collectors.toList());
        Assert.isTrue(matches.size() == 1, role + " must match exactly one active connector in the requested cluster");
        return matches.get(0);
    }

    private static void validateConnector(String name, String className, String host, Integer port, String role) {
        Assert.hasText(name, role + " name is required");
        Assert.hasText(className, role + " class is required");
        Assert.hasText(host, role + " host is required");
        Assert.isTrue(port != null && port > 0 && port <= 65535, role + " port must be between 1 and 65535");
    }

    private static void validateUnchangedConfigs(AddConnectorConfigDTO configs) {
        if (configs != null) {
            Assert.isTrue(CollectionUtils.isEmpty(configs.getSourceConnectorConfigs()) && CollectionUtils.isEmpty(configs.getSinkConnectorConfigs()),
                "Existing connector configurations cannot be changed when creating a connection");
        }
    }

    private static void requirePositiveId(Long id, String field) {
        Assert.isTrue(id != null && id > 0, field + " must be positive");
    }
}
