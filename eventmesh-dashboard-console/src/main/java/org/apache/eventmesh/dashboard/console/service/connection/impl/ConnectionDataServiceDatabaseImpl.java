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


import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.mapper.config.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.connection.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.AddConnectionDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.CreateConnectionDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.DynamicGetConnectionDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.GetConnectionListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.connection.ConnectionDataService;

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
    public Long insert(ConnectionEntity connectionEntity) {
        return connectionMapper.insert(connectionEntity);
    }


    @EmLog(OprType = "add", OprTarget = "Connection")
    @Override
    public boolean createConnection(CreateConnectionDTO createConnectionDTO) {
        ConnectorEntity sinkConnector = this.createSinkConnector(createConnectionDTO.getClusterId(), createConnectionDTO.getAddConnectionDTO());
        ConnectorEntity sourceConnector = this.createSourceConnector(createConnectionDTO.getClusterId(), createConnectionDTO.getAddConnectionDTO());
        ConnectionEntity connectionEntity = this.setConnection(createConnectionDTO);
        connectionEntity.setSinkId(sinkConnector.getId());
        connectionEntity.setSourceId(sourceConnector.getId());
        connectionMapper.insert(connectionEntity);
        this.addConnectorConfigs(createConnectionDTO.getAddConnectorConfigDTO().getSinkConnectorConfigs(), sinkConnector);
        this.addConnectorConfigs(createConnectionDTO.getAddConnectorConfigDTO().getSourceConnectorConfigs(), sourceConnector);
        return false;
    }

    private ConnectionEntity setConnection(CreateConnectionDTO createConnectionDTO) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(createConnectionDTO.getClusterId());
        connectionEntity.setSourceType("connector");
        connectionEntity.setSinkType("connector");
        connectionEntity.setRuntimeId(-1L);
        connectionEntity.setGroupId(createConnectionDTO.getAddConnectionDTO().getGroupId());
        connectionEntity.setStatus(1);
        connectionEntity.setDescription(createConnectionDTO.getAddConnectionDTO().getConnectionDescription());
        connectionEntity.setTopic(createConnectionDTO.getAddConnectionDTO().getTopicName());
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

    public ConnectorEntity createSinkConnector(Long clusterId, AddConnectionDTO addConnectionDTO) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setName(addConnectionDTO.getSinkName());
        connectorEntity.setHost(addConnectionDTO.getSinkHost());
        connectorEntity.setClusterId(clusterId);
        connectorEntity.setClassName(addConnectionDTO.getSinkClass());
        connectorEntity.setType("Connector");
        connectorEntity.setStatus(1);
        connectorEntity.setPodState(0);
        connectorEntity.setPort(addConnectionDTO.getSinkPort());
        connectorMapper.insert(connectorEntity);
        return connectorEntity;
    }

    public ConnectorEntity createSourceConnector(Long clusterId, AddConnectionDTO addConnectionDTO) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setName(addConnectionDTO.getSourceName());
        connectorEntity.setHost(addConnectionDTO.getSourceHost());
        connectorEntity.setClusterId(clusterId);
        connectorEntity.setClassName(addConnectionDTO.getSourceClass());
        connectorEntity.setType("Connector");
        connectorEntity.setStatus(1);
        connectorEntity.setPodState(0);
        connectorEntity.setPort(addConnectionDTO.getSourcePort());
        connectorMapper.insert(connectorEntity);
        return connectorEntity;
    }


    @Override
    public List<ConnectionEntity> getAllConnections() {
        return connectionMapper.selectAll();
    }

    public ConnectionEntity setSearchCriteria(DynamicGetConnectionDTO dynamicGetConnectionDTO, ConnectionEntity connectionEntity) {
        if (dynamicGetConnectionDTO != null) {
            if (dynamicGetConnectionDTO.getTopicName() != null) {
                connectionEntity.setTopic(dynamicGetConnectionDTO.getTopicName());
            }
        }
        return connectionEntity;
    }

    @Override
    public List<ConnectionListVO> getConnectionToFrontByCluster(Long clusterId, GetConnectionListDTO getConnectionListDTO) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        connectionEntity = this.setSearchCriteria(getConnectionListDTO.getDynamicGetConnectionDTO(), connectionEntity);
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

