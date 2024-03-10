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

package org.apache.eventmesh.dashboard.console.service.client.Impl;

import org.apache.eventmesh.dashboard.console.entity.client.ClientEntity;
import org.apache.eventmesh.dashboard.console.mapper.client.ClientMapper;
import org.apache.eventmesh.dashboard.console.service.client.ClientDataService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientDataServiceImpl implements ClientDataService {

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public List<ClientEntity> selectAll() {
        return clientMapper.selectAll();
    }

    @Override
    public void batchInsert(List<ClientEntity> clientEntityList) {
        clientMapper.batchInsert(clientEntityList);
    }
}
