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

import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClientMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.ClientDataService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientDataServiceImpl implements ClientDataService {

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public Integer deActive(ClientEntity clientEntity) {
        return clientMapper.deactivate(clientEntity);
    }

    @Override
    public Integer deActiveByHostPort(ClientEntity clientEntity) {
        return clientMapper.deActiveByHostPort(clientEntity);
    }

    @SuppressWarnings("checkstyle:Indentation")
    @Override
    public void insertClient(ClientEntity clientEntity) {
         clientMapper.insert(clientEntity);
    }

    @Override
    public Integer batchInsert(List<ClientEntity> clientEntityList) {
        return clientMapper.batchInsert(clientEntityList);
    }

    @Override
    public List<ClientEntity> selectByHostPort(ClientEntity clientEntity) {
        return clientMapper.selectByHostPort(clientEntity);
    }

    @Override
    public List<ClientEntity> selectByClusterId(ClientEntity clientEntity) {
        return this.clientMapper.selectByClusterId(clientEntity);
    }

}
