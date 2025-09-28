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


package org.apache.eventmesh.dashboard.console.controller.deploy.pause;

import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterAndRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.ClusterAllMetadataDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PauseCluster implements UpdateHandler<ClusterEntity> {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterRelationshipService clusterRelationshipService;


    private List<RuntimeEntity> selfRuntimeList = new ArrayList<>();

    private List<RuntimeEntity> notSelfRuntimeList = new ArrayList<>();


    @Override
    public void init() {

    }

    /**
     * 关系解除，如果
     *
     * @param clusterEntity
     */
    @Override
    public void handler(ClusterEntity clusterEntity) {
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        if (clusterEntity.getClusterType().isMeta()) {
            // 检查  meta 集群 是否被关联，如何被关联，就禁止删除
            ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
            clusterRelationshipEntity.setClusterId(clusterEntity.getId());
            List<ClusterAndRelationshipEntity> clusterRelationshipEntityList =
                this.clusterRelationshipService.queryClusterAndRelationshipEntityListByClusterId(clusterRelationshipEntity);
            if (!clusterRelationshipEntityList.isEmpty()) {
                // 打印结果
                return;
            }
        }

        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterEntity.getId());
        ClusterAllMetadataDO clusterAllMetadata = this.runtimeService.queryAllByClusterId(runtimeEntity, true, true);

        clusterAllMetadata.getRuntimeEntityList().forEach(entity -> {
            // TODO
            if (entity.getTrusteeshipType().isSelf()) {
                entity.setDeployStatusType(DeployStatusType.PAUSE_FULL_WAIT);
            } else {
                entity.setStatus(1L);
            }
        });

        clusterAllMetadata.getClusterEntityList().forEach(entity -> {

        });

        if (clusterEntity.getClusterType().isMeta()) {
            // 得到 所有关联项目

            // 获得 所有关联项目的 存储 cluster

            //
        }

    }

}
