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

package org.apache.eventmesh.dashboard.console.service.config.Impl;

import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.config.ConfigMapper;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class ConfigServiceImpl implements ConfigService {


    @Autowired
    ConfigMapper configMapper;

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
    public Integer addConfig(ConfigEntity configEntity) {
        return configMapper.addConfig(configEntity);
    }

    @Override
    public Integer deleteConfig(ConfigEntity configEntity) {
        return configMapper.deleteConfig(configEntity);
    }

    @Override
    public List<ConfigEntity> selectByInstanceId(ConfigEntity configEntity) {
        return configMapper.selectByInstanceId(configEntity);
    }

    @Override
    public List<ConfigEntity> selectDefaultConfig(ConfigEntity configEntity) {
        return configMapper.selectDefaultConfig(configEntity);
    }

    @Override
    public void updateConfig(ConfigEntity configEntity) {
        configMapper.updateConfig(configEntity);
    }


}
