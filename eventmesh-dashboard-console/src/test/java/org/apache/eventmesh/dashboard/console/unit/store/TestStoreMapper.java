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

package org.apache.eventmesh.dashboard.console.unit.store;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
import org.apache.eventmesh.dashboard.console.mapper.storage.StoreMapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestStoreMapper {

    @Autowired
    private StoreMapper storeMapper;

    private StoreEntity storeEntity;

    private StoreEntity storeEntity1;

    @Before
    public void init() {
        storeEntity = new StoreEntity();
        storeEntity1 = new StoreEntity();
    }

    @Test
    public void testAddStore() {
        storeMapper.addStore(storeEntity);
        storeMapper.addStore(storeEntity1);
        StoreEntity storeEntities = storeMapper.selectStoreByCluster(storeEntity);

        storeEntities.setUpdateTime(null);
        storeEntities.setCreateTime(null);


    }

    @Test
    public void testDeleteStoreByUnique() {
        storeMapper.addStore(storeEntity);
        storeMapper.deleteStoreByUnique(storeEntity);
        StoreEntity storeEntities = storeMapper.selectStoreByCluster(storeEntity);
        Assert.assertNull(storeEntities);
    }

    @Test
    public void testUpdateStoreByUnique() {
        storeMapper.addStore(storeEntity);
        storeEntity.setStatus((short) 5);
        storeMapper.updateStoreByUnique(storeEntity);
        StoreEntity storeEntity1 = storeMapper.selectStoreByCluster(storeEntity);
        Assert.assertEquals(storeEntity1.getStatus(), storeEntity.getStatus());
    }
}
