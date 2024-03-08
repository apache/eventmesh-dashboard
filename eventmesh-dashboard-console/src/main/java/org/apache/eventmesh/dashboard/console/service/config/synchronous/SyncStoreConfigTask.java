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
import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;
import org.apache.eventmesh.dashboard.console.service.config.instanceoperation.StoreConfigService;
import org.apache.eventmesh.dashboard.console.service.store.StoreService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Synchronous DB To Instance
 */

@Service
public class SyncStoreConfigTask {

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreConfigService storeConfigService;

    @Autowired
    private ConfigService configService;

    public void synchronousStoreConfig(Long clusterId) {
        List<StoreEntity> storeEntityList = storeService.selectStoreByCluster(clusterId);
        for (StoreEntity storeEntity : storeEntityList) {

            ConcurrentHashMap<String, String> storeConfigMapFromInstance = this.configListToMap(
                storeConfigService.getStorageConfigFromInstance(clusterId, storeEntity.getHost()));

            ConfigEntity configEntity = this.getConfigEntityBelongInstance(clusterId, storeEntity.getId());

            ConcurrentHashMap<String, String> storeConfigMapFromDb = this.configListToMap(configService.selectByInstanceId(configEntity));

            ConcurrentHashMap<String, String> updateConfigMap = new ConcurrentHashMap<>();

            storeConfigMapFromInstance.entrySet().forEach(n -> {
                if (storeConfigMapFromDb.remove(n.getKey(), n.getValue())) {
                    storeConfigMapFromInstance.remove(n.getKey());
                }
                if (storeConfigMapFromDb.get(n.getKey()) != null) {
                    updateConfigMap.put(n.getKey(), storeConfigMapFromDb.get(n.getKey()));
                    storeConfigMapFromInstance.remove(n.getKey());
                    storeConfigMapFromDb.remove(n.getKey());
                }
            });
            //add  storeConfigMapFromDb

            //update  updateConfigMap

            //delete storeConfigMapFromInstance
        }
    }

    private ConcurrentHashMap<String, String> configListToMap(List<ConfigEntity> configEntityList) {
        ConcurrentHashMap<String, String> storeConfigMap = new ConcurrentHashMap<>();
        configEntityList.forEach(n -> {
                storeConfigMap.put(n.getConfigName(), n.getConfigValue());
            }
        );
        return storeConfigMap;
    }


    private ConfigEntity getConfigEntityBelongInstance(Long clusterId, Long id) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setClusterId(clusterId);
        configEntity.setInstanceId(id);
        configEntity.setInstanceType(1);
        return configEntity;
    }
}
