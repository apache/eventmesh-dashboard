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

import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.mapper.connection.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.service.connection.ConnectionDataService;

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


    @Override
    public List<ConnectionEntity> getAllConnectionsByClusterId(Long clusterId) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        return connectionMapper.selectByClusterId(connectionEntity);
    }


    @Override
    public Integer selectConnectionNumByCluster(Long clusterId) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setClusterId(clusterId);
        return connectionMapper.selectConnectionNumByCluster(connectionEntity);
    }

    @Override
    public List<ConnectionEntity> getAllConnections() {
        return connectionMapper.selectAll();
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
}

