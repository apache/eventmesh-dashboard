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

import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.message.ConfigControllerMapper;
import org.apache.eventmesh.dashboard.console.model.dto.config.UpdateConfigDTO;
import org.apache.eventmesh.dashboard.console.model.function.config.QueryByInstanceIdDTO;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(("/user/config"))
public class ConfigController {

    private static final ConfigControllerMapper INSTANCE = ConfigControllerMapper.INSTANCE;

    @Autowired
    private ConfigService configService;

    @PostMapping("/cluster/config/updateConfigs")
    public String updateConfigsByTypeAndId(@Validated @RequestBody UpdateConfigDTO updateConfigDTO) {
        return "success";
    }


    @PostMapping("/queryByInstanceId")
    public List<ConfigEntity> queryByInstanceId(@Validated @RequestBody QueryByInstanceIdDTO queryByInstanceIdDTO) {
        return configService.queryByInstanceId(INSTANCE.queryByInstanceId(queryByInstanceIdDTO));
    }


}
