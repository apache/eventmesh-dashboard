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


package org.apache.eventmesh.dashboard.console.service.deploy.impl;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.deploy.PortMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.model.deploy.GetPortsDO;
import org.apache.eventmesh.dashboard.console.service.deploy.PortService;
import org.apache.eventmesh.dashboard.core.function.multinetwork.ClusterPort;
import org.apache.eventmesh.dashboard.core.function.multinetwork.PortMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSONObject;

@Service
@Transactional
public class PortServiceImpl implements PortService {

    @Autowired
    private PortMapper portMapper;

    @Autowired
    private ConfigMapper configMapper;

    @Override
    public List<String> getPorts(GetPortsDO getPortsDO) {
        // 节点从哪里开始
        // 如果是按照循序领取，同时要修改配置文件里面的
        PortEntity portEntity = new PortEntity();
        portEntity.setClusterId(getPortsDO.getClusterId());
        portEntity.setCurrentPort(getPortsDO.getPortNum());
        this.applyPorts(portEntity);
        List<String> ports = new ArrayList<>();
        for (int i = 1; i <= portEntity.getCurrentPort(); i++) {
            ports.add((getPortsDO.getPortNum() + i) + "");
        }
        return ports;
    }

    public Map<String, Integer> applyPorts(ClusterType clusterType, Long kubernetesId) {
        ClusterPort clusterPort = ClusterPort.valueOf(clusterType.name());
        int count = clusterPort.getPortMetadataList().size();
        PortEntity portEntity = new PortEntity();
        portEntity.setClusterId(kubernetesId);
        portEntity.setCurrentPort(count);
        PortEntity ports = this.applyPorts(new PortEntity());
        Map<String, Integer> portMap = new HashMap<>(count);
        List<ConfigEntity> configEntityList = new ArrayList<>();
        for (int i = 0; i < clusterPort.getPortMetadataList().size(); i++) {
            PortMetadata portMetadata = clusterPort.getPortMetadataList().get(i);
            int port = ports.getCurrentPort() + i;
            portMap.put(portMetadata.getConfigName(), port);
            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setConfigName(portMetadata.getConfigName());
            configEntity.setConfigValue(port + "");
            configEntityList.add(configEntity);

        }
        if (Objects.equals(ClusterType.STORAGE_KAFKA_BROKER, clusterType)) {
            List<ConfigEntity> queryData = this.configMapper.queryByRuntimeIdAndConfigName(configEntityList);
            queryData.forEach(configEntity -> {
                JSONObject jsonObject = JSONObject.parseObject(configEntity.getConfigValueRange());
                jsonObject.put("port", portMap.get(configEntity.getConfigName()));
                configEntity.setConfigValueRange(jsonObject.toJSONString());
            });
        }
        this.configMapper.updateValueByConfigList(configEntityList);
        return portMap;
    }

    private PortEntity applyPorts(PortEntity portEntity) {
        PortEntity newPort = this.portMapper.lockPort(portEntity);
        if (Objects.isNull(newPort)) {
            portEntity.setCurrentPort(30000);
            this.portMapper.insertPort(portEntity);
            newPort = this.portMapper.lockPort(portEntity);
        }
        this.portMapper.updatePort(portEntity);
        return newPort;
    }

    @Override
    public void releasePort(GetPortsDO getPortsDO) {

    }
}
