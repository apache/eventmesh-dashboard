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

import org.apache.eventmesh.dashboard.console.entity.function.ConfigTemplateEntity;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigTemplateMapper;
import org.apache.eventmesh.dashboard.console.service.function.ConfigTemplateService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfigTemplateServiceImpl implements ConfigTemplateService {

    @Autowired
    private ConfigTemplateMapper configTemplateMapper;


    @Override
    public void insertConfigTemplate(ConfigTemplateEntity configTemplateEntity) {
        this.configTemplateMapper.insertConfigTemplate(configTemplateEntity);
    }

    @Override
    public List<ConfigTemplateEntity> queryConfigTemplateByClusterType(ConfigTemplateEntity configTemplateEntity) {
        return this.configTemplateMapper.queryConfigTemplateByClusterType(configTemplateEntity);
    }
}
