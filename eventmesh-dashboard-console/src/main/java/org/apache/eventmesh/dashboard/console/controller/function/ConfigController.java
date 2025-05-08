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


package org.apache.eventmesh.dashboard.console.controller.function;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.function.ConfigControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.dto.config.DetailConfigsVO;
import org.apache.eventmesh.dashboard.console.modle.dto.config.QueryConfigByInstanceId;
import org.apache.eventmesh.dashboard.console.modle.dto.config.UpdateConfigDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/user/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @PostMapping("/updateConfigs")
    public Integer updateConfigsByTypeAndId(@Validated @RequestBody UpdateConfigDTO updateConfigDTO) {
        return this.configService.updateConfigValueById(ConfigControllerMapper.INSTANCE.updateConfigsByTypeAndId(updateConfigDTO));
    }


    @PostMapping("/queryConfigByInstanceId")
    public List<DetailConfigsVO> queryConfigByInstanceId(@Validated @RequestBody QueryConfigByInstanceId data) {
        ConfigEntity configEntity = ConfigControllerMapper.INSTANCE.queryConfigByInstanceId(data);
        if (Objects.equals(data.getInstanceType(), "runtime")) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setId(data.getInstanceId());
            runtimeEntity = this.runtimeService.queryRuntimeEntityById(runtimeEntity);

            ConfigEntity clusterConfigEntity = new ConfigEntity();
            this.configService.queryByInstanceId(configEntity);

            ConfigEntity runtimeConfigEntity = new ConfigEntity();
            this.configService.queryByInstanceId(configEntity);
        } else {
            this.configService.queryByInstanceId(configEntity);
        }

        return null;
    }


}