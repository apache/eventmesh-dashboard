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
import org.apache.eventmesh.dashboard.console.domain.ClusterAndRuntimeDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryTreeByChildClusterIdDO;
import org.apache.eventmesh.dashboard.console.model.dto.agent.AgentStartActionDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressManage;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressService;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressServiceIPDO;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressServiceResult;
import org.apache.eventmesh.dashboard.console.utils.data.controller.agent.AgentActionControllerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    @Autowired
    private ClusterAndRuntimeDomain clusterAndRuntimeDomain;

    @Autowired
    private AddressManage addressManage;

    /**
     * TODO
     *      启动时，无法确认其他服务是否启动
     *      只有在不停地 check 时，可以确认依赖服务已经启动
     */
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

        AgentStartActionVO agentStartActionVO = new AgentStartActionVO();
        agentStartActionVO.setClusterType(clusterEntity.getClusterType().toString());
        ClusterType clusterType = clusterEntity.getClusterType();
        ClusterSyncMetadataEnum.getClusterFramework(clusterType);
        if (!clusterType.isMeta()) {
            return agentStartActionVO;
        }
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
        if (!clusterFramework.isAP()) {
            return agentStartActionVO;
        }
        agentStartActionVO.setCheck(true);
        agentStartActionVO.setConfigMap(this.queryConfig(runtimeEntity, clusterEntity));
        return agentStartActionVO;

    }


    @PostMapping("agentCheckRuntime")
    public AgentCheckRuntimeVO agentCheckRuntime(@RequestBody @Valid AgentStartActionDTO data) {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(data.getClusterId());
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);

        ClusterType clusterType = clusterEntity.getClusterType();

        AddressService addressService = addressManage.getAddressService(clusterType);
        RuntimeEntity remoteRuntimeEntity = new RuntimeEntity();
        remoteRuntimeEntity.setClusterId(clusterEntity.getId());

        AddressServiceResult addressServiceResult = new AddressServiceResult();
        if (clusterType.isEventMethRuntime()) {
            //
            this.clusterAndRuntimeDomain.getAllClusterAndRuntimeByCluster(null);
        } else {
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
            if ((clusterFramework.isCAP() && clusterType.isMeta())) {
                List<RuntimeEntity> runtimeEntities = this.runtimeService.queryRuntimeToFrontByClusterId(remoteRuntimeEntity);
                addressServiceResult = addressService.createCapAddress(AgentActionControllerUtils.byRuntimeList(runtimeEntities));
            } else if (clusterType.isMetaAndRuntime()) {
                List<RuntimeEntity> runtimeEntities = this.runtimeService.queryRuntimeToFrontByClusterId(remoteRuntimeEntity);
                addressServiceResult = addressService.createCapAddress(AgentActionControllerUtils.byRuntimeList(runtimeEntities));
            } else if (clusterType.isRuntime()) {
                // 需要 得到 集群的依赖组件检查依赖 runtime 是否运行
                // 检查依赖 runtime 是否运行
                QueryTreeByChildClusterIdDO queryTreeByChildClusterIdDO = new QueryTreeByChildClusterIdDO();
                queryTreeByChildClusterIdDO.setClusterEntity(clusterEntity);
                queryTreeByChildClusterIdDO.setRootClusterTypeList(Set.of(clusterType.getHigher()));
                queryTreeByChildClusterIdDO.setOnlyClusterTypeList(new HashSet<>(clusterType.getHigher().getMetaClusterType()));

                ClusterAndRuntimeOfRelationshipDO clusterAndRuntimeOfRelationshipDo =
                    this.clusterAndRuntimeDomain.queryTreeByChildClusterId(queryTreeByChildClusterIdDO);
                AddressServiceIPDO addressServiceDo =
                    AgentActionControllerUtils.byClusterAndRuntimeOfRelationshipDO(clusterAndRuntimeOfRelationshipDo);
                addressServiceResult = addressService.createRegisterAddress(addressServiceDo);
            }
        }
        AgentCheckRuntimeVO agentCheckRuntimeVO = new AgentCheckRuntimeVO();
        if (!addressServiceResult.isCheckSuccess()) {
            agentCheckRuntimeVO.setSuccess(false);
        } else {
            ClusterEntity tmpClusterEntity = clusterEntity;
            // TODO 这个有一个问题，这些配置地址，是写入 cluster ，还是 写入 runtime, 写入 cluster
            List<ConfigEntity> configEntityList = addressServiceResult.getConfigEntities().stream().filter((value) -> {
                if (Objects.isNull(value.getConfigName())) {
                    return false;
                } else {
                    value.setInstanceId(tmpClusterEntity.getId());
                    value.setInstanceType(MetadataType.CLUSTER);
                    return true;
                }
            }).toList();
            this.configService.updateValueByConfigList(configEntityList);
            agentCheckRuntimeVO.setConfigMap(this.queryConfig(remoteRuntimeEntity, clusterEntity));
        }
        return agentCheckRuntimeVO;
    }


    private Map<String, String> queryConfig(RuntimeEntity runtimeEntity, ClusterEntity clusterEntity) {
        List<ConfigEntity> queryConfigConfigList = new ArrayList<>(2);

        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setInstanceId(clusterEntity.getId());
        configEntity.setInstanceType(MetadataType.CLUSTER);

        queryConfigConfigList.add(configEntity);

        configEntity = new ConfigEntity();
        configEntity.setInstanceId(runtimeEntity.getId());
        configEntity.setInstanceType(MetadataType.RUNTIME);

        queryConfigConfigList.add(configEntity);

        Map<String, String> configMap = new HashMap<>();
        queryConfigConfigList.forEach(value -> {
            List<ConfigEntity> configEntityList = this.configService.queryByInstanceId(value);
            configEntityList.forEach(config -> {
                configMap.put(config.getConfigName(), config.getConfigValue());
            });
        });

        return configMap;
    }

}
