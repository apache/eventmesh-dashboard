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

import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.modle.dto.config.QueryConfigByInstanceId;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfigServiceImpl implements ConfigService {


    @Autowired
    ConfigMapper configMapper;


    @Override
    public List<ConfigEntity> queryByInstanceId(ConfigEntity configEntity) {
        return configMapper.queryByInstanceId(configEntity);
    }

    @Override
    public List<ConfigEntity> selectAll() {
        return configMapper.selectAll();
    }

    @Override
    public Integer updateConfigValueById(ConfigEntity configEntity) {
        return this.configMapper.updateConfigValueById(configEntity);
    }

    @Override
    public void batchInsert(List<ConfigEntity> configEntityList) {
        configMapper.batchInsert(configEntityList);
    }

    @Override
    public void copyConfig(Long sourceId, Long targetId) {
        configMapper.copyConfig(sourceId, targetId);
    }


    public Integer addConfig(ConfigEntity configEntity) {
        return configMapper.addConfig(configEntity);
    }


    public ConfigEntity setSearchCriteria(QueryConfigByInstanceId queryConfigByInstanceId, ConfigEntity configEntity) {
        if (queryConfigByInstanceId != null) {
            if (queryConfigByInstanceId.getConfigName() != null) {
                configEntity.setConfigName(queryConfigByInstanceId.getConfigName());
            }
            if (queryConfigByInstanceId.getIsModify() != null) {
                configEntity.setIsModify(queryConfigByInstanceId.getIsModify());
            }
            if (queryConfigByInstanceId.getAlreadyUpdate() != null) {
                configEntity.setAlreadyUpdate(queryConfigByInstanceId.getAlreadyUpdate());
            }
        }
        return configEntity;
    }




}