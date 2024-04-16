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
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;
import org.apache.eventmesh.dashboard.console.service.config.instanceoperation.RuntimeConfigService;
import org.apache.eventmesh.dashboard.console.service.runtime.RuntimeService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Synchronous DB To Instance
 */

@Service
public class SyncRuntimeConfigTask {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RuntimeConfigService runtimeConfigService;

    @Autowired
    private ConfigService configService;

    public void synchronousRuntimeConfig(Long clusterId) {
        List<RuntimeEntity> runtimeEntityList = runtimeService.getRuntimeByClusterId(clusterId);
        for (RuntimeEntity runtimeEntity : runtimeEntityList) {

            ConcurrentHashMap<String, String> runtimeConfigMapFromInstance = this.configListToMap(
                runtimeConfigService.getRuntimeConfigFromInstance(clusterId, runtimeEntity.getHost()));

            ConfigEntity configEntity = this.getConfigEntityBelongInstance(clusterId, runtimeEntity.getId());

            ConcurrentHashMap<String, String> runtimeConfigMapFromDb =
                this.configListToMap(configService.selectByInstanceIdAndType(configEntity.getInstanceId(), configEntity.getInstanceType()));

            ConcurrentHashMap<String, String> updateConfigMap = new ConcurrentHashMap<>();

            runtimeConfigMapFromInstance.entrySet().forEach(n -> {
                if (runtimeConfigMapFromDb.remove(n.getKey(), n.getValue())) {
                    runtimeConfigMapFromInstance.remove(n.getKey());
                }
                if (runtimeConfigMapFromDb.get(n.getKey()) != null) {
                    updateConfigMap.put(n.getKey(), runtimeConfigMapFromDb.get(n.getKey()));
                    runtimeConfigMapFromInstance.remove(n.getKey());
                    runtimeConfigMapFromDb.remove(n.getKey());
                }
            });
            //add  runtimeConfigMapFromDb

            //update  updateConfigMap

            //delete runtimeConfigMapFromInstance
        }
    }

    private ConcurrentHashMap<String, String> configListToMap(List<ConfigEntity> configEntityList) {
        ConcurrentHashMap<String, String> runtimeConfigMap = new ConcurrentHashMap<>();
        configEntityList.forEach(n -> {
                runtimeConfigMap.put(n.getConfigName(), n.getConfigValue());
            }
        );
        return runtimeConfigMap;
    }


    private ConfigEntity getConfigEntityBelongInstance(Long clusterId, Long id) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setClusterId(clusterId);
        configEntity.setInstanceId(id);
        configEntity.setInstanceType(0);
        return configEntity;
    }
}
