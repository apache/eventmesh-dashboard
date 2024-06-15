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


import org.apache.eventmesh.dashboard.console.entity.connection.AddConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.connection.ConnectionControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.CreateConnectionDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.connection.GetConnectionListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.connection.ConnectionListVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster/connection")
public class ConnectionController {

    @Autowired
    private ConnectionDataService connectionDataService;

    /**
     * 'type' only can be Sink or Source
     *
     * @param type
     * @return
     */
    @GetMapping("/getConnectorBusinessType")
    public List<String> getConnectorBusinessType(String type) {
        return connectionDataService.getConnectorBusinessType(type);
    }

    @GetMapping("/getConnectorConfigs")
    public List<ConfigEntity> getConnectorConfigsByClassAndVersion(String version, String classType) {
        return connectionDataService.getConnectorConfigsByClassAndVersion(classType, version);
    }


    @GetMapping("/showCreateConnectionMessage")
    public AddConnectionEntity showCreateConnectionMessage() {
        return new AddConnectionEntity();
    }


    @PostMapping("/createConnection")
    public String createConnection(@Validated @RequestBody CreateConnectionDTO createConnectionDTO) {
        try {
            connectionDataService.createConnection(ConnectionControllerMapper.INSTANCE.queryCreateEntityByConnection(createConnectionDTO));
        } catch (Exception e) {
            return e.getMessage();
        }
        return "success";
    }


    @PostMapping("/getConnectionList")
    public List<ConnectionListVO> getConnectionList(@Validated @RequestBody GetConnectionListDTO getConnectionListDTO) {
        return connectionDataService.getConnectionToFrontByCluster(ConnectionControllerMapper.INSTANCE.queryEntityByConnection(getConnectionListDTO));
    }

    @GetMapping("/getConnectorDetail")
    public ConnectorEntity getConnectorDetail(Long connectorId) {
        return connectionDataService.getConnectorById(connectorId);
    }


}
