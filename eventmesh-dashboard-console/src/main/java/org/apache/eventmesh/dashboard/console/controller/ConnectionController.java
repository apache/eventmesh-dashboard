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


package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.model.dto.connection.AddConnectionDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.CreateConnectionDTO;
import org.apache.eventmesh.dashboard.console.model.dto.connection.GetConnectionListDTO;
import org.apache.eventmesh.dashboard.console.model.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionController {


    private ConnectionDataService connectionDataService;

    /**
     * 'type' only can be Sink or Source
     *
     * @param type
     * @return
     */
    @GetMapping("/cluster/connection/getConnectorBusinessType")
    public List<String> getConnectorBusinessType(String type) {
        return connectionDataService.getConnectorBusinessType(type);
    }

    @GetMapping("/cluster/connection/getConnectorConfigs")
    public List<ConfigEntity> getConnectorConfigsByClassAndVersion(String version, String classType) {
        return connectionDataService.getConnectorConfigsByClassAndVersion(classType, version);
    }


    @GetMapping("/cluster/connection/showCreateConnectionMessage")
    public AddConnectionDTO showCreateConnectionMessage() {
        return new AddConnectionDTO();
    }


    @PostMapping("/cluster/connection/createConnection")
    public String createConnection(@Validated @RequestBody CreateConnectionDTO createConnectionDTO) {
        try {
            connectionDataService.createConnection(createConnectionDTO);

        } catch (Exception e) {
            return e.getMessage();
        }
        return "success";
    }


    @PostMapping("/cluster/connection/getConnectionList")
    public List<ConnectionListVO> getConnectionList(@Validated @RequestBody GetConnectionListDTO getConnectionListDTO) {
        return connectionDataService.getConnectionToFrontByCluster(getConnectionListDTO.getClusterId(), getConnectionListDTO);
    }

    @GetMapping("/cluster/connection/getConnectorDetail")
    public ConnectorEntity getConnectorDetail(Long connectorId) {
        return connectionDataService.getConnectorById(connectorId);
    }


}
