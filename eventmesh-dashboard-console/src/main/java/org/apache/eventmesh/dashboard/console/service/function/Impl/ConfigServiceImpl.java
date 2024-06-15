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

package org.apache.eventmesh.dashboard.console.service.function.Impl;

import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.DefaultConfigKey;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.modle.config.ChangeConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.config.UpdateConfigsLog;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class ConfigServiceImpl implements ConfigService {


    @Autowired
    ConfigMapper configMapper;

    private Map<DefaultConfigKey, String> defaultConfigCache = new HashMap<>();


    @EmLog(OprTarget = "Runtime", OprType = "UpdateConfigs")
    public void logUpdateRuntimeConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigEntity> changeConfigEntityList) {
        changeConfigEntityList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(0);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "store", OprType = "UpdateConfigs")
    public void logUpdateStoreConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigEntity> changeConfigEntityList) {
        changeConfigEntityList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(1);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "Connector", OprType = "UpdateConfigs")
    public void logUpdateConnectorConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigEntity> changeConfigEntityList) {
        changeConfigEntityList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(2);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "Topic", OprType = "UpdateConfigs")
    public void logUpdateTopicConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigEntity> changeConfigEntityList) {
        changeConfigEntityList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(3);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }

    @Override
    public void updateConfigsByInstanceId(String name, Long clusterId, Integer instanceType, Long instanceId,
        List<ChangeConfigEntity> changeConfigEntityList) {
        ConcurrentHashMap<String, String> stringStringConcurrentHashMap = new ConcurrentHashMap<>();
        changeConfigEntityList.forEach(n -> {
            stringStringConcurrentHashMap.put(n.getConfigName(), n.getConfigValue());
        });
        UpdateConfigsLog updateConfigsLog = new UpdateConfigsLog();
        updateConfigsLog.setInstanceId(instanceId);
        updateConfigsLog.setClusterId(clusterId);
        updateConfigsLog.setName(name);
        updateConfigsLog.setConfigProperties(this.mapToProperties(stringStringConcurrentHashMap));
        ConfigServiceImpl service = (ConfigServiceImpl) AopContext.currentProxy();
        if (instanceType == 0) {
            service.logUpdateRuntimeConfigs(updateConfigsLog, changeConfigEntityList);
        } else if (instanceType == 1) {
            service.logUpdateStoreConfigs(updateConfigsLog, changeConfigEntityList);
        } else if (instanceType == 2) {
            service.logUpdateConnectorConfigs(updateConfigsLog, changeConfigEntityList);
        } else if (instanceType == 3) {
            service.logUpdateTopicConfigs(updateConfigsLog, changeConfigEntityList);
        }
    }


    @Override
    public List<ConfigEntity> selectAll() {
        return configMapper.selectAll();
    }

    @Override
    public Integer batchInsert(List<ConfigEntity> configEntityList) {
        return configMapper.batchInsert(configEntityList);
    }

    @Override
    public String mapToYaml(Map<String, String> stringMap) {
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(stringMap);
    }

    @Override
    public String mapToProperties(Map<String, String> stringMap) {
        Properties properties = new Properties();
        stringMap.forEach((key, value) -> {
            properties.setProperty(key, value);
        });
        return properties.toString().replace(",", ",\n");
    }

    @Override
    public Map<String, String> propertiesToMap(String configProperties) {
        ConcurrentHashMap<String, String> stringStringConcurrentHashMap = new ConcurrentHashMap<>();
        String replace = configProperties.replace("{", "");
        String replace1 = replace.replace("}", "");
        String[] split = replace1.split(",");
        Arrays.stream(split).forEach(n -> {
            String[] split1 = n.split("=");
            stringStringConcurrentHashMap.put(split1[0].replace("\n ", ""), split1[1]);
        });
        return stringStringConcurrentHashMap;
    }

    @Override
    public void insertConfig(ConfigEntity configEntity) {
        configMapper.insertConfig(configEntity);
    }

    @Override
    public Integer deleteConfig(ConfigEntity configEntity) {
        return configMapper.deleteConfig(configEntity);
    }

    @Override
    public List<ConfigEntity> selectByInstanceIdAndType(Long instanceId, Integer type) {
        ConfigEntity config = new ConfigEntity();
        config.setInstanceId(instanceId);
        config.setInstanceType(type);
        return configMapper.selectByInstanceId(config);
    }


    @Override
    public List<ConfigEntity> selectToFront(ConfigEntity config) {
        return configMapper.selectConfigsToFrontWithDynamic(config);
    }

    public void insertDefaultConfigToCache() {
        List<ConfigEntity> configEntityList = configMapper.selectAllDefaultConfig();
        configEntityList.forEach(n -> {
            DefaultConfigKey defaultConfigKey = new DefaultConfigKey();
            defaultConfigKey.setConfigName(n.getConfigName());
            defaultConfigKey.setBusinessType(n.getBusinessType());
            defaultConfigCache.putIfAbsent(defaultConfigKey, n.getConfigValue());

        });
    }

    @Override
    public Map<String, String> selectDefaultConfig(String businessType) {
        if (defaultConfigCache.size() == 0) {
            this.insertDefaultConfigToCache();
        }
        ConcurrentHashMap<String, String> stringStringConcurrentHashMap = new ConcurrentHashMap<>();
        defaultConfigCache.forEach((k, v) -> {
            if (k.getBusinessType().equals(businessType)) {
                stringStringConcurrentHashMap.put(k.getConfigName(), v);
            }
        });
        return stringStringConcurrentHashMap;
    }

}
