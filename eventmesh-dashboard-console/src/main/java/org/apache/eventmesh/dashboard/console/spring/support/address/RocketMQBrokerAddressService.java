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

package org.apache.eventmesh.dashboard.console.spring.support.address;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;

import java.util.List;
import java.util.Map;

public class RocketMQBrokerAddressService extends AbstractAddressService {


    @Override
    public List<ClusterType> clusterType() {
        return List.of(
            ClusterType.STORAGE_ROCKETMQ_CLUSTER,
            ClusterType.STORAGE_ROCKETMQ_BROKER,
            ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE,
            ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT);
    }

    @Override
    public AddressServiceResult createClientAddress(AddressServiceIPDO data) {
        // TODO 挂上域名
        Map<Long, List<RuntimeEntity>> runtimeEntityByClusterType = data.getRuntimeEntityByClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        StringBuilder hostAddress = new StringBuilder();
        StringBuilder podAddress = new StringBuilder();
        runtimeEntityByClusterType.forEach((key, value) -> {
            value.forEach(runtimeEntity -> {
                hostAddress.append(runtimeEntity.getHost()).append(":").append(runtimeEntity.getPort()).append(';');
                podAddress.append(runtimeEntity.getPodHost()).append(":").append(runtimeEntity.getPort()).append(';');
            });
        });
        AddressServiceResult addressServiceResult = this.createAddressServiceResult();
        addressServiceResult.setCheckSuccess(true);

        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setConfigName("namesrvAddr");
        configEntity.setDescription("用户调用地址：");
        configEntity.setConfigValue(hostAddress.toString());

        addressServiceResult.addConfigEntity(configEntity);

        configEntity = new ConfigEntity();
        configEntity.setDescription("内部地址：");
        configEntity.setConfigValue(podAddress.toString());
        addressServiceResult.addConfigEntity(configEntity);

        return addressServiceResult;
    }

    @Override
    public AddressServiceResult createCapAddress(AddressServiceIPDO data) {
        return null;
    }

    @Override
    public AddressServiceResult createRegisterAddress(AddressServiceIPDO data) {
        List<RuntimeEntity> runtimeEntityList = data.getRuntimeEntityList(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        AddressServiceResult addressServiceResult = this.createAddressServiceResult(runtimeEntityList, false);
        if (!addressServiceResult.isCheckSuccess()) {
            return addressServiceResult;
        }
        return this.createClientAddress(data);
    }

    @Override
    public AddressServiceResult createDependAddress(AddressServiceIPDO data) {
        return null;
    }
}
