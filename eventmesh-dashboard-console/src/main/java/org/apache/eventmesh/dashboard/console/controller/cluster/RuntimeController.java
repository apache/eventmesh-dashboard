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


package org.apache.eventmesh.dashboard.console.controller.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.RuntimeControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.runtime.QueryRuntimeListByClusterIdFormDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.runtime.QueryRuntimeListByOrganizationIdAndFormDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user/runtime")
public class RuntimeController {

    @Autowired
    private RuntimeService runtimeService;

    @PostMapping("/queryRuntimeListByClusterId")
    public List<RuntimeEntity> queryRuntimeListByClusterId(@Validated @RequestBody ClusterIdDTO clusterIdDTO) {
        List<RuntimeEntity> runtimeEntityList =
            runtimeService.queryRuntimeToFrontByClusterId(RuntimeControllerMapper.INSTANCE.queryRuntimeListByClusterId(clusterIdDTO));
        return runtimeEntityList;
    }

    @PostMapping("/queryRuntimeListByClusterIdForm")
    public List<RuntimeEntity> queryRuntimeListByClusterIdForm(@Validated @RequestBody QueryRuntimeListByClusterIdFormDTO dto) {
        return this.runtimeService.queryRuntimeListByClusterIdForm(RuntimeControllerMapper.INSTANCE.queryRuntimeListByClusterIdForm(dto));
    }


    @PostMapping("/queryRuntimeListByOrganizationIdAndForm")
    public List<RuntimeEntity> queryRuntimeListByOrganizationIdAndForm(@Validated @RequestBody
    QueryRuntimeListByOrganizationIdAndFormDTO dto) {
        return this.runtimeService.queryRuntimeListByClusterIdForm(RuntimeControllerMapper.INSTANCE.queryRuntimeListByOrganizationIdAndForm(dto));
    }


    @PostMapping("/queryRuntimeById")
    public RuntimeEntity queryRuntimeById(@Validated @RequestBody IdDTO idDTO) {
        return this.runtimeService.queryRuntimeEntityById(RuntimeControllerMapper.INSTANCE.queryRuntimeListById(idDTO));
    }


}
