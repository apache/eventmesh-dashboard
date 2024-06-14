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

package org.apache.eventmesh.dashboard.console.service.store.Impl;

import org.apache.eventmesh.dashboard.console.entity.StoreEntity;
import org.apache.eventmesh.dashboard.console.mapper.storage.StoreMapper;
import org.apache.eventmesh.dashboard.console.service.store.StoreService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;

    @Override
    public StoreEntity selectStoreToFrontListByCluster(Long clusterId) {
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setClusterId(clusterId);
        return storeMapper.selectStoreByCluster(storeEntity);
    }


    @Override
    public List<StoreEntity> selectAll() {
        return storeMapper.selectAll();
    }

    @Override
    public StoreEntity selectById(Long storeId) {
        StoreEntity query = new StoreEntity();
        query.setId(storeId);
        return storeMapper.selectById(query);
    }

    @Override
    public StoreEntity selectByHostPort(String host, Integer port) {
        StoreEntity query = new StoreEntity();
        query.setHost(host);
        query.setPort(port);
        return storeMapper.selectByHostPort(query);
    }

    @Override
    public Integer batchInsert(List<StoreEntity> storeEntities) {
        return storeMapper.batchInsert(storeEntities);
    }

    @Override
    public void insertStore(StoreEntity storeEntity) {
        storeMapper.insertStore(storeEntity);
    }

    @Override
    public Integer deleteStoreByUnique(StoreEntity storeEntity) {
        return storeMapper.deleteStoreByUnique(storeEntity);
    }

    @Override
    public StoreEntity selectStoreByCluster(Long clusterId) {
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setClusterId(clusterId);
        return storeMapper.selectStoreByCluster(storeEntity);
    }

    @Override
    public Integer updateStoreByUnique(StoreEntity storeEntity) {
        return storeMapper.updateStoreByUnique(storeEntity);
    }
}
