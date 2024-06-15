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



import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.CreateConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connection.AddConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.modle.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ConnectionDataServiceDatabaseImpl implements ConnectionDataService {

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private ConnectorMapper connectorMapper;

    @Autowired
    private ConfigMapper configMapper;


    @Override
    public ConnectorEntity getConnectorById(Long connectorId) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(connectorId);
        return connectorMapper.selectById(connectorEntity);
    }

    @Override
    public List<String> getConnectorBusinessType(String type) {
        ConfigEntity config = new ConfigEntity();
        config.setBusinessType(type);
        return configMapper.selectConnectorBusinessType(config);
    }

    @Override
    public List<ConnectionEntity> getAllConnectionsByClusterId(Long clusterId) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        return connectionMapper.selectByClusterId(connectionEntity);
    }

    @Override
    public void insert(ConnectionEntity connectionEntity) {
        connectionMapper.insert(connectionEntity);
    }


    @EmLog(OprType = "add", OprTarget = "Connection")
    @Override
    public boolean createConnection(CreateConnectionEntity createConnectionEntity) {
        ConnectorEntity sinkConnector = this.createSinkConnector(createConnectionEntity.getClusterId(),
            createConnectionEntity.getAddConnectionEntity());
        ConnectorEntity sourceConnector = this.createSourceConnector(createConnectionEntity.getClusterId(),
            createConnectionEntity.getAddConnectionEntity());
        ConnectionEntity connectionEntity = this.setConnection(createConnectionEntity);
        connectionEntity.setSinkId(sinkConnector.getId());
        connectionEntity.setSourceId(sourceConnector.getId());
        connectionMapper.insert(connectionEntity);
        this.addConnectorConfigs(createConnectionEntity.getAddConnectorConfigEntity().getSinkConnectorConfigs(), sinkConnector);
        this.addConnectorConfigs(createConnectionEntity.getAddConnectorConfigEntity().getSourceConnectorConfigs(), sourceConnector);
        return false;
    }

    private ConnectionEntity setConnection(CreateConnectionEntity createConnectionEntity) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(createConnectionEntity.getClusterId());
        connectionEntity.setSourceType("connector");
        connectionEntity.setSinkType("connector");
        connectionEntity.setRuntimeId(-1L);
        connectionEntity.setGroupId(createConnectionEntity.getAddConnectionEntity().getGroupId());
        connectionEntity.setStatus(1);
        connectionEntity.setDescription(createConnectionEntity.getAddConnectionEntity().getConnectionDescription());
        connectionEntity.setTopic(createConnectionEntity.getAddConnectionEntity().getTopicName());
        return connectionEntity;
    }

    public void addConnectorConfigs(List<ConfigEntity> configEntityList, ConnectorEntity connectorEntity) {
        configEntityList.forEach(n -> {
            n.setInstanceId(connectorEntity.getId());
            n.setIsDefault(0);
            n.setClusterId(connectorEntity.getClusterId());
        });
        configMapper.batchInsert(configEntityList);
    }

    public ConnectorEntity createSinkConnector(Long clusterId, AddConnectionEntity addConnectionEntity) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setName(addConnectionEntity.getSinkName());
        connectorEntity.setHost(addConnectionEntity.getSinkHost());
        connectorEntity.setClusterId(clusterId);
        connectorEntity.setClassName(addConnectionEntity.getSinkClass());
        connectorEntity.setType("Connector");
        connectorEntity.setStatus(1);
        connectorEntity.setPodState(0);
        connectorEntity.setPort(addConnectionEntity.getSinkPort());
        connectorMapper.insert(connectorEntity);
        return connectorEntity;
    }

    public ConnectorEntity createSourceConnector(Long clusterId, AddConnectionEntity addConnectionEntity) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setName(addConnectionEntity.getSourceName());
        connectorEntity.setHost(addConnectionEntity.getSourceHost());
        connectorEntity.setClusterId(clusterId);
        connectorEntity.setClassName(addConnectionEntity.getSourceClass());
        connectorEntity.setType("Connector");
        connectorEntity.setStatus(1);
        connectorEntity.setPodState(0);
        connectorEntity.setPort(addConnectionEntity.getSourcePort());
        connectorMapper.insert(connectorEntity);
        return connectorEntity;
    }


    @Override
    public List<ConnectionEntity> getAllConnections() {
        return connectionMapper.selectAll();
    }

    @Override
    public List<ConnectionListVO> getConnectionToFrontByCluster(ConnectionEntity connectionEntity) {
        List<ConnectionEntity> allConnectionsByClusterId = connectionMapper.selectToFrontByClusterId(connectionEntity);
        List<ConnectionListVO> connectionListVOs = new ArrayList<>();
        allConnectionsByClusterId.forEach(n -> {
            connectionListVOs.add(this.setConnectionListVO(n));
        });
        return connectionListVOs;
    }

    private ConnectionListVO setConnectionListVO(ConnectionEntity connectionEntity) {
        ConnectionListVO connectionListVO = new ConnectionListVO();
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(connectionEntity.getSinkId());
        ConnectorEntity sinkConnector = connectorMapper.selectById(connectorEntity);
        connectorEntity.setId(connectionEntity.getSourceId());
        ConnectorEntity sourceConnector = connectorMapper.selectById(connectorEntity);
        connectionListVO.setSinkClass(sinkConnector.getClassName());
        connectionListVO.setSourceClass(sourceConnector.getClassName());
        connectionListVO.setSinkConnectorId(sinkConnector.getId());
        connectionListVO.setSourceConnectorId(sourceConnector.getId());
        connectionListVO.setSinkConnectorName(sinkConnector.getName());
        connectionListVO.setSourceConnectorName(sourceConnector.getName());
        connectionListVO.setTopicName(connectionEntity.getTopic());
        connectionListVO.setStatus(connectionEntity.getStatus());
        return connectionListVO;
    }

    @Override
    @Transactional
    public void replaceAllConnections(List<ConnectionEntity> connectionEntityList) {
        Map<Long, List<ConnectionEntity>> connectionsGroupedByClusterId = connectionEntityList.stream()
            .collect(Collectors.groupingBy(ConnectionEntity::getClusterId));

        connectionsGroupedByClusterId.forEach((clusterId, newConnections) -> {
            ConnectionEntity connectionEntity = new ConnectionEntity();
            connectionEntity.setClusterId(clusterId);
            List<ConnectionEntity> existingConnections = connectionMapper.selectByClusterId(connectionEntity);

            // Collect connections that are not in the new list
            List<ConnectionEntity> connectionsToInactive = existingConnections.stream()
                .filter(existingConnection -> !newConnections.contains(existingConnection))
                .collect(Collectors.toList());

            // Collect new connections that are not in the existing list
            List<ConnectionEntity> connectionsToInsert = newConnections.stream()
                .filter(connection -> !existingConnections.contains(connection))
                .collect(Collectors.toList());

            // Delete connections in batch
            if (!connectionsToInactive.isEmpty()) {
                connectionMapper.batchEndConnectionById(connectionsToInactive);
            }

            // Insert new connections in batch
            if (!connectionsToInsert.isEmpty()) {
                connectionMapper.batchInsert(connectionsToInsert);
            }
        });
    }


    @Override
    public List<ConfigEntity> getConnectorConfigsByClassAndVersion(String classType, String version) {
        ConfigEntity config = new ConfigEntity();
        config.setBusinessType(classType);
        List<ConfigEntity> configEntityList = configMapper.selectConnectorConfigsByBusinessType(config);
        configEntityList.forEach(n -> {
            if (!n.matchVersion(version)) {
                configEntityList.remove(n);
            }
        });
        return configEntityList;
    }
}

