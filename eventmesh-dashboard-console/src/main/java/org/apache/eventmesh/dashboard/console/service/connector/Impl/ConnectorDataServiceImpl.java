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

package org.apache.eventmesh.dashboard.console.service.connector.Impl;

import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.service.connector.ConnectorDataService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorDataServiceImpl implements ConnectorDataService {

    @Autowired
    private ConnectorMapper connectorMapper;

    @Override
    public List<ConnectorEntity> selectConnectorByCluster(Long clusterId) {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setClusterId(clusterId);
        return connectorMapper.selectByClusterId(connectorEntity);
    }

    @Override
    public List<ConnectorEntity> selectByHostPort(ConnectorEntity connectorEntity) {
        return null;
    }
}
