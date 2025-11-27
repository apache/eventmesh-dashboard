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


package org.apache.eventmesh.dashboard.console.service.deploy.impl;

import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;
import org.apache.eventmesh.dashboard.console.mapper.deploy.PortMapper;
import org.apache.eventmesh.dashboard.console.model.deploy.GetPortsDO;
import org.apache.eventmesh.dashboard.console.service.deploy.PortService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortServiceImpl implements PortService {

    @Autowired
    private PortMapper portMapper;

    @Override
    public List<String> getPorts(GetPortsDO getPortsDO) {
        PortEntity portEntity = new PortEntity();
        portEntity.setClusterId(getPortsDO.getClusterId());
        PortEntity newPort = this.portMapper.lockPort(portEntity);
        if (Objects.isNull(newPort)) {
            portEntity.setCurrentPort(2000);
            this.portMapper.insertPort(portEntity);
            newPort = this.portMapper.lockPort(portEntity);
        }
        List<String> ports = new ArrayList<>();
        this.portMapper.updatePort(portEntity);
        for (int i = 1; i <= getPortsDO.getPortNum(); i++) {
            ports.add((newPort.getCurrentPort() + i) + "");
        }
        return ports;
    }
}
