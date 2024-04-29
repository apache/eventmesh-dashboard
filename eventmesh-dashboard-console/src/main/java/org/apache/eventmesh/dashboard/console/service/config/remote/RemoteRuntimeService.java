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

package org.apache.eventmesh.dashboard.console.service.config.remote;


import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.config.ConfigMapper;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

@Service
public class RemoteRuntimeService {

    @Autowired
    private ConfigMapper configMapper;

    public void getRuntimeConfigByCluster(RuntimeEntity runtimeEntity) {
        RestTemplate restTemplate = new RestTemplate();
        String forEntity =
            restTemplate.getForObject("http://" + runtimeEntity.getHost() + ":" + runtimeEntity.getPort() + "/v2/configuration?format=properties", String.class, (Object) null);
        JSONObject jsonMap = JSON.parseObject(forEntity);
        Set<Entry<String, Object>> entries = jsonMap.entrySet();
        ArrayList<ConfigEntity> configEntities = new ArrayList<>();
        for (Entry<String, Object> entry : entries) {
            if (!entry.getKey().equals("data")) {
                continue;
            }
            JSONObject parse = JSON.parseObject(entry.getValue().toString());
            Set<Entry<String, Object>> configurationEntry = parse.entrySet();
            configurationEntry.forEach((configuration) -> {
                JSONObject parse1 = JSON.parseObject(configuration.getValue().toString());
                Set<Entry<String, Object>> configureEntry = parse1.entrySet();
                configureEntry.forEach((configure) -> {
                    ConfigEntity config = this.createRuntimeConfigEntity(runtimeEntity, configure.getKey(), configure.getValue().toString());
                    configEntities.add(config);
                });
            });
        }
        configMapper.batchInsert(configEntities);
    }

    private ConfigEntity createRuntimeConfigEntity(RuntimeEntity runtimeEntity, String configName, String configValue) {
        ConfigEntity config = new ConfigEntity();
        config.setConfigName(configName);
        config.setConfigValue(configValue);
        config.setBusinessType("runtime");
        config.setInstanceType(0);
        config.setClusterId(runtimeEntity.getClusterId());
        config.setIsDefault(0);
        config.setInstanceId(runtimeEntity.getId());
        return config;
    }
}
