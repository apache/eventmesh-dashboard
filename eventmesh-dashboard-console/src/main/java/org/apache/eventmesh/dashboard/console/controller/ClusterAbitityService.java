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

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.console.entity.base.BaseClusterIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;


@Component
public class ClusterAbitityService {


    @Autowired
    private ClusterService clusterService;


    @Value("${console.controller.data.merge:false}")
    @Getter
    private boolean merge = false;


    /**
     *
     *  可以对 topic， group， config ，sub 等做到强制性.offset 无法做到强制一致性
     *
     *
     */
    @Value("${console.data.forceConsistent:false}")
    private boolean forceConsistent = false;


    public boolean isCAP(ClusterIdDTO clusterIdDTO) {
        ClusterEntity clusterEntity = this.clusterService.queryClusterById(ClusterControllerMapper.INSTANCE.toClusterEntity(clusterIdDTO));
        return this.isCAP(clusterEntity);
    }

    public boolean isCAP(BaseClusterIdEntity baseClusterIdEntity) {
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(baseClusterIdEntity.getClusterType());
        return clusterFramework.isCAP();
    }
}
