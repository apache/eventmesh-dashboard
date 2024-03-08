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

package org.apache.eventmesh.dashboard.console.service.config.synchronous;

import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;
import org.apache.eventmesh.dashboard.console.service.config.instanceoperation.ConnectorConfigService;
import org.apache.eventmesh.dashboard.console.service.connector.ConnectorDataService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Synchronous DB To Instance
 */
@Service
public class SyncConnectorConfigTask {

    @Autowired
    private ConnectorDataService connectorDataService;

    @Autowired
    private ConnectorConfigService connectorConfigService;
    @Autowired
    private ConfigService configService;

    public void synchronousConnectorConfig(Long clusterId) {
        List<ConnectorEntity> connectorEntities = connectorDataService.selectConnectorByCluster(clusterId);
        for (ConnectorEntity connectorEntity : connectorEntities) {

            ConcurrentHashMap<String, String> connectorConfigMapFromInstance = this.configListToMap(
                connectorConfigService.getConnectorConfigFromInstance(clusterId, connectorEntity.getId()));

            ConfigEntity configEntity = this.getConfigEntityBelongInstance(clusterId, connectorEntity.getId());

            ConcurrentHashMap<String, String> connectorConfigMapFromDb = this.configListToMap(configService.selectByInstanceId(configEntity));

            ConcurrentHashMap<String, String> updateConfigMap = new ConcurrentHashMap<>();

            connectorConfigMapFromInstance.entrySet().forEach(n -> {
                if (connectorConfigMapFromDb.remove(n.getKey(), n.getValue())) {
                    connectorConfigMapFromInstance.remove(n.getKey());
                }
                if (connectorConfigMapFromDb.get(n.getKey()) != null) {
                    updateConfigMap.put(n.getKey(), connectorConfigMapFromDb.get(n.getKey()));
                    connectorConfigMapFromInstance.remove(n.getKey());
                    connectorConfigMapFromDb.remove(n.getKey());
                }
            });
            //add  connectorConfigMapFromDb

            //update  updateConfigMap

            //delete connectorConfigMapFromInstance
        }
    }

    private ConcurrentHashMap<String, String> configListToMap(List<ConfigEntity> configEntityList) {
        ConcurrentHashMap<String, String> connectorConfigMap = new ConcurrentHashMap<>();
        configEntityList.forEach(n -> {
                connectorConfigMap.put(n.getConfigName(), n.getConfigValue());
            }
        );
        return connectorConfigMap;
    }


    private ConfigEntity getConfigEntityBelongInstance(Long clusterId, Long id) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setClusterId(clusterId);
        configEntity.setInstanceId(id);
        configEntity.setInstanceType(2);
        return configEntity;
    }
}
