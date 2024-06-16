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

package org.apache.eventmesh.dashboard.console.service.cluster;

import org.apache.eventmesh.dashboard.console.entity.CreateConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.connection.ConnectionListVO;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing ConnectionEntity data.
 */
@Service
public interface ConnectionDataService {

    ConnectorEntity getConnectorById(Long connectorId);

    List<String> getConnectorBusinessType(String type);

    List<ConnectionEntity> getAllConnectionsByClusterId(Long clusterId);

    boolean createConnection(CreateConnectionEntity connectionEntity);

    List<ConnectionEntity> getAllConnections();

    List<ConnectionListVO> getConnectionToFrontByCluster(ConnectionEntity connectionEntity);

    @Transactional
    void replaceAllConnections(List<ConnectionEntity> connectionEntityList);

    List<ConfigEntity> getConnectorConfigsByClassAndVersion(String classType, String version);

    void insert(ConnectionEntity connectionEntity);
}
