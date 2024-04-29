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

package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.dto.config.DetailConfigsVO;
import org.apache.eventmesh.dashboard.console.modle.dto.config.GetConfigsListDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.config.UpdateConfigDTO;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @PostMapping("/cluster/config/updateConfigs")
    public String updateConfigsByTypeAndId(@Validated @RequestBody UpdateConfigDTO updateConfigDTO) {
        try {
            configService.updateConfigsByInstanceId(updateConfigDTO.getUsername(), updateConfigDTO.getClusterId(), updateConfigDTO.getInstanceType(),
                updateConfigDTO.getInstanceId(), updateConfigDTO.getChangeConfigDTOS());
        } catch (Exception e) {
            return e.getMessage();
        }
        return "success";
    }


    @PostMapping("/cluster/config/getInstanceDetailConfigs")
    public List<DetailConfigsVO> getInstanceDetailConfigs(@Validated @RequestBody GetConfigsListDTO getConfigsListDTO) {
        List<ConfigEntity> configEntityList = configService.selectToFront(getConfigsListDTO.getInstanceId(),
            getConfigsListDTO.getInstanceType(), getConfigsListDTO);
        Map<String, ConfigEntity> stringStringConcurrentHashMap = configService.selectDefaultConfig(getConfigsListDTO.getBusinessType(),
            getConfigsListDTO.getInstanceType());
        ArrayList<DetailConfigsVO> showDetailConfigsVOS = new ArrayList<>();
        System.out.println(stringStringConcurrentHashMap.get("eventMesh.server.global.scheduler"));
        configEntityList.forEach(n -> {
            DetailConfigsVO showDetailConfigsVO = new DetailConfigsVO();
            ConfigEntity defaultConfig = stringStringConcurrentHashMap.get(n.getConfigName());
            if (defaultConfig != null) {
                showDetailConfigsVO.setDefaultValue(defaultConfig.getConfigValue());
                showDetailConfigsVO.setIsModify(defaultConfig.getIsModify());
            }
            showDetailConfigsVO.setConfigName(n.getConfigName());
            showDetailConfigsVO.setConfigValue(n.getConfigValue());
            showDetailConfigsVO.setAlreadyUpdate(n.getAlreadyUpdate());
            showDetailConfigsVOS.add(showDetailConfigsVO);
        });
        return showDetailConfigsVOS;
    }


}
