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
import org.apache.eventmesh.dashboard.console.modle.dto.config.ChangeConfigDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.config.GetConfigsListDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.config.UpdateConfigsLog;
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
    public void logUpdateRuntimeConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigDTO> changeConfigDTOList) {
        changeConfigDTOList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(null);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "store", OprType = "UpdateConfigs")
    public void logUpdateStoreConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigDTO> changeConfigDTOList) {
        changeConfigDTOList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(null);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "Connector", OprType = "UpdateConfigs")
    public void logUpdateConnectorConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigDTO> changeConfigDTOList) {
        changeConfigDTOList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(null);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }


    @EmLog(OprTarget = "Topic", OprType = "UpdateConfigs")
    public void logUpdateTopicConfigs(UpdateConfigsLog updateConfigsLog, List<ChangeConfigDTO> changeConfigDTOList) {
        changeConfigDTOList.forEach(n -> {
            ConfigEntity config = new ConfigEntity();
            config.setInstanceType(null);
            config.setInstanceId(updateConfigsLog.getInstanceId());
            config.setConfigName(n.getConfigName());
            config.setConfigValue(n.getConfigValue());
            config.setAlreadyUpdate(n.getAlreadyUpdate());
            configMapper.updateConfig(config);
        });
    }

    @Override
    public void updateConfigsByInstanceId(String name, Long clusterId, Integer instanceType, Long instanceId,
        List<ChangeConfigDTO> changeConfigDTOList) {
        ConcurrentHashMap<String, String> stringStringConcurrentHashMap = new ConcurrentHashMap<>();
        changeConfigDTOList.forEach(n -> {
            stringStringConcurrentHashMap.put(n.getConfigName(), n.getConfigValue());
        });
        UpdateConfigsLog updateConfigsLog =
            new UpdateConfigsLog(instanceId, clusterId, name, this.mapToProperties(stringStringConcurrentHashMap));
        ConfigServiceImpl service = (ConfigServiceImpl) AopContext.currentProxy();
        if (instanceType == 0) {
            service.logUpdateRuntimeConfigs(updateConfigsLog, changeConfigDTOList);
        } else if (instanceType == 1) {
            service.logUpdateStoreConfigs(updateConfigsLog, changeConfigDTOList);
        } else if (instanceType == 2) {
            service.logUpdateConnectorConfigs(updateConfigsLog, changeConfigDTOList);
        } else if (instanceType == 3) {
            service.logUpdateTopicConfigs(updateConfigsLog, changeConfigDTOList);
        }
    }


    @Override
    public List<ConfigEntity> selectAll() {
        return configMapper.selectAll();
    }

    @Override
    public void batchInsert(List<ConfigEntity> configEntityList) {
        configMapper.batchInsert(configEntityList);
    }

    @Override
    public void copyConfig(Long sourceId, Long targetId) {
        configMapper.copyConfig(sourceId, targetId);
    }

    @Override
    public void restoreConfig(Long sourceId, Long targetId) {

    }

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


    public Integer addConfig(ConfigEntity configEntity) {
        return configMapper.addConfig(configEntity);
    }

    @Override
    public Integer deleteConfig(ConfigEntity configEntity) {
        return configMapper.deleteConfig(configEntity);
    }

    @Override
    public List<ConfigEntity> selectByInstanceIdAndType(Long instanceId, Integer type) {
        ConfigEntity config = new ConfigEntity();
        config.setInstanceId(instanceId);
        config.setInstanceType(null);
        return configMapper.selectByInstanceId(config);
    }

    public ConfigEntity setSearchCriteria(GetConfigsListDTO getConfigsListDTO, ConfigEntity configEntity) {
        if (getConfigsListDTO != null) {
            if (getConfigsListDTO.getConfigName() != null) {
                configEntity.setConfigName(getConfigsListDTO.getConfigName());
            }
            if (getConfigsListDTO.getIsModify() != null) {
                configEntity.setIsModify(getConfigsListDTO.getIsModify());
            }
            if (getConfigsListDTO.getAlreadyUpdate() != null) {
                configEntity.setAlreadyUpdate(getConfigsListDTO.getAlreadyUpdate());
            }
        }
        return configEntity;
    }

    @Override
    public List<ConfigEntity> selectToFront(Long instanceId, Integer type, GetConfigsListDTO getConfigsListDTO) {
        ConfigEntity config = new ConfigEntity();
        config.setInstanceId(instanceId);
        config.setInstanceType(null);
        config = this.setSearchCriteria(getConfigsListDTO, config);
        return configMapper.getConfigsToFrontWithDynamic(config);
    }

    public void addDefaultConfigToCache() {
        List<ConfigEntity> configEntityList = configMapper.selectAllDefaultConfig();
        configEntityList.forEach(n -> {
            DefaultConfigKey defaultConfigKey = new DefaultConfigKey(n.getBusinessType(), n.getConfigName());
            defaultConfigCache.putIfAbsent(defaultConfigKey, n.getConfigValue());

        });
    }

    @Override
    public Map<String, String> selectDefaultConfig(String businessType, Long instanceId, Integer instanceType) {
        if (defaultConfigCache.size() == 0) {
            this.addDefaultConfigToCache();
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
