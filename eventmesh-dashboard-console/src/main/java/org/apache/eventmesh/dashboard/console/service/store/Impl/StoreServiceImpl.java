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

import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
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
    public List<StoreEntity> selectAll() {
        return storeMapper.selectAll();
    }

    @Override
    public void batchInsert(List<StoreEntity> storeEntities) {
        storeMapper.batchInsert(storeEntities);
    }

    @Override
    public void addStore(StoreEntity storeEntity) {
        storeMapper.addStore(storeEntity);
    }

    @Override
    public void deleteStoreByUnique(StoreEntity storeEntity) {
        storeMapper.deleteStoreByUnique(storeEntity);
    }

    @Override
    public List<StoreEntity> selectStoreByCluster(Long clusterId) {
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setClusterId(clusterId);
        return storeMapper.selectStoreByCluster(storeEntity);
    }

    @Override
    public void updateStoreByUnique(StoreEntity storeEntity) {
        storeMapper.updateStoreByUnique(storeEntity);
    }
}
