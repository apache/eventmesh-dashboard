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

package org.apache.eventmesh.dashboard.console.controller.agent;


import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.model.DO.runtime.QueryRuntimeByBigExpandClusterDO;
import org.apache.eventmesh.dashboard.console.model.dto.agent.AgentStartActionDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamp.decoration.core.result.DecorationResultException;


@RestController
@RequestMapping("/agent/")
public class AgentActionController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @PostMapping("agentStartAction")
    public AgentStartActionVO agentStartAction(@RequestBody @Valid AgentStartActionDTO data) {

        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(data.getClusterId());

        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        if (Objects.isNull(clusterEntity)) {
            DecorationResultException.throwDecorationResultException(41001, "cluster not exist");
        }
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setId(data.getRuntimeId());
        runtimeEntity = this.runtimeService.queryRuntimeEntityById(runtimeEntity);
        if (Objects.isNull(runtimeEntity)) {
            DecorationResultException.throwDecorationResultException(41002, "runtime not exist");
        }

        RuntimeEntity updateRuntimeEntity = new RuntimeEntity();
        updateRuntimeEntity.setId(runtimeEntity.getId());
        updateRuntimeEntity.setHost(data.getNodeAddress());
        updateRuntimeEntity.setPodHost(data.getLocalAddress());
        this.runtimeService.updateAddressByRuntimeId(updateRuntimeEntity);
        // 识别架构，修改 需要 修改 config 相关配置 , 端口

        // TODO 识别 架构方式，修改 cap 架构配置 ，

        // 依赖组件，在 agentCheckRuntime ，

        // 识别架构, 获得

        AgentStartActionVO agentStartActionVO = new AgentStartActionVO();
        agentStartActionVO.setClusterType(clusterEntity.getClusterType().toString());
        agentStartActionVO.setCheck(clusterEntity.getClusterType().isMeta());

        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setInstanceId(clusterEntity.getId());
        configEntity.setInstanceType(MetadataType.CLUSTER);
        List<ConfigEntity> configEntityList = this.configService.queryByInstanceId(configEntity);
        Map<String, String> configMap = new HashMap<String, String>();
        configEntityList.forEach(config -> {
            configMap.put(config.getConfigName(), config.getConfigValue());
        });

        configEntity = new ConfigEntity();
        configEntity.setInstanceId(runtimeEntity.getId());
        configEntity.setInstanceType(MetadataType.RUNTIME);
        this.configService.queryByInstanceId(configEntity);
        configEntityList.forEach(config -> {
            configMap.put(config.getConfigName(), config.getConfigValue());
        });

        agentStartActionVO.setConfigMap(configMap);

        return agentStartActionVO;

    }


    @PostMapping("agentCheckRuntime")
    public AgentCheckRuntimeVO agentCheckRuntime(@RequestBody @Valid AgentStartActionDTO data) {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(data.getClusterId());
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);

        AgentCheckRuntimeVO agentCheckRuntimeVO = new AgentCheckRuntimeVO();

        QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO =
            QueryRuntimeByBigExpandClusterDO.builder().followClusterId(clusterEntity.getId())
                .queryClusterTypeList(clusterEntity.getClusterType().getMetaClusterType()).build();

        List<RuntimeEntity> runtimeEntityList = this.runtimeService.queryMetaRuntimeByStorageClusterId(queryRuntimeByBigExpandClusterDO);

        ClusterType clusterType = clusterEntity.getClusterType();
        if (clusterType.isEventMethRuntime()) {
            queryRuntimeByBigExpandClusterDO = QueryRuntimeByBigExpandClusterDO.builder().followClusterId(clusterEntity.getId())
                .storageMetaClusterTypeList(ClusterType.getStorageMetaRuntimeCluster()).build();
            //如果是 eventmesh 集群，name需要查询 存储集群的 runtime 是否启动
            // 如果识别 meta 的可用度
            runtimeEntityList = this.runtimeService.queryRuntimeByBigExpandCluster(queryRuntimeByBigExpandClusterDO);
        } else {
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
            if (clusterFramework.isCAP() && clusterType.isMeta()) {
                //
            } else if (clusterType.isMetaAndRuntime()) {
                //
            } else if (clusterType.isRuntime()) {
                //
            }

        }

        return agentCheckRuntimeVO;
    }
}
