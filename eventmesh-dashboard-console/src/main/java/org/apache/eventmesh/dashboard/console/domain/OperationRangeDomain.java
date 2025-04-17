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


package org.apache.eventmesh.dashboard.console.domain;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseSyncEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.ClusterAllMetadataDO;
import org.apache.eventmesh.dashboard.console.modle.dto.operation.OperationBaseDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 可以得到返回的数据为两种： 1. 写入数据的 2. 直接请求其他组件
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OperationRangeDomain {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;


    private OperationBaseDTO operationBaseDTO;

    private OperationRangeDomainDataHandler rangeDomainDataHandler;

    private final Set<ClusterType> clusterTypeSet = new HashSet<>();

    private ClusterType rangeType;

    private boolean currentType;


    public OperationRangeDomain operationBaseDTO(OperationBaseDTO operationBaseDTO) {
        this.operationBaseDTO = operationBaseDTO;
        return this;
    }


    public OperationRangeDomain operationRangeDomainDataHandler(OperationRangeDomainDataHandler operationRangeDomainDataHandler) {
        this.rangeDomainDataHandler = operationRangeDomainDataHandler;
        return this;
    }

    public OperationRangeDomain clusterType(ClusterType clusterType) {
        this.clusterTypeSet.add(clusterType);
        return this;
    }

    public OperationRangeDomain rangeType(ClusterType rangeType) {
        this.rangeType = rangeType;
        return this;
    }

    public <T> T hander() {
        List<T> operationList = new ArrayList<>();
        if (Objects.equals(operationBaseDTO.getRangeType(), MetadataType.RUNTIME)) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity = runtimeService.queryRuntimeEntityById(runtimeEntity);
            // 直接写入
            operationList.add((T) this.rangeDomainDataHandler.handler(runtimeEntity));
            return (T) operationList;
        }
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType());
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        ClusterAllMetadataDO clusterAllMetadataDO =
            runtimeService.queryAllByClusterId(runtimeEntity, true, false);
        clusterAllMetadataDO.getClusterEntityList().forEach((value) -> {
            ClusterFramework clusterFramework1 = ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType());
            if (!clusterFramework1.isCAP()) {
                return;
            }

            if (!clusterTypeSet.isEmpty() && !clusterTypeSet.contains(value.getClusterType())) {
                return;
            }
            if (!Objects.equals(this.rangeType, value.getClusterType().getAssemblyNodeType())) {
                return;
            }

            operationList.add((T) this.rangeDomainDataHandler.handler(value));
        });
        clusterAllMetadataDO.getRuntimeEntityList().forEach((value) -> {
            ClusterFramework clusterFramework1 = ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType());
            if (clusterFramework1.isCAP()) {
                return;
            }
            if (!clusterTypeSet.isEmpty() && !clusterTypeSet.contains(value.getClusterType())) {
                return;
            }
            if (!Objects.equals(this.rangeType, value.getClusterType().getAssemblyNodeType())) {
                return;
            }
            operationList.add((T) this.rangeDomainDataHandler.handler(value));
        });

        return (T) operationList;
    }


    public static interface OperationRangeDomainDataHandler {


        Object handler(BaseSyncEntity baseEntity);
    }

}
