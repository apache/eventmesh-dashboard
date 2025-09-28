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


package org.apache.eventmesh.dashboard.console.controller.deploy.relationship;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

public class RelationshipHandler implements UpdateHandler<ClusterRelationshipEntity> {

    private ClusterService clusterService;

    private ClusterRelationshipService clusterRelationshipService;

    private RuntimeService runtimeService;


    @Override
    public void init() {

    }

    /**
     *  是否 允许
     *
     */
    @Override
    public void handler(ClusterRelationshipEntity clusterRelationshipEntity) {
        ClusterEntity queryMainClusterEntity = new ClusterEntity();
        queryMainClusterEntity.setId(clusterRelationshipEntity.getClusterId());
        ClusterEntity mainClusterEntity = this.clusterService.queryClusterById(queryMainClusterEntity);

        ClusterEntity queryRelationshipClusterEntity = new ClusterEntity();
        queryRelationshipClusterEntity.setId(clusterRelationshipEntity.getRelationshipId());
        ClusterEntity relationshipClusterEntity = this.clusterService.queryClusterById(queryRelationshipClusterEntity);

        //  绑定 meta 集群， 那么下面所有的 runtime 集群，需要更新更新
        //  绑定 runtime 集群， 这个 runtime 集群 需要获得绑定集群的 meta ，然后重启
        //  绑定 存储集群
        clusterRelationshipService.addClusterRelationshipEntry(clusterRelationshipEntity);
        //  如果  relationship runtime 集群 不是 托管类型 不需要处理

        // main cluster  与 relationship cluster 都不是 self 类型就不需要管
        if(mainClusterEntity.getTrusteeshipType().isReverse() && relationshipClusterEntity.getTrusteeshipType().isReverse()){
            return;
        }

        if(mainClusterEntity.getClusterType().isEventCluster() && relationshipClusterEntity.getClusterType().isStorageCluster()){
            // 还要判断 main 集群 是否 支持 API 操作 meta cluster
            if(mainClusterEntity.getTrusteeshipType().isReverse()){
                return;
            }

            // 查询 relationship 里面的 所有的 meta cluster 的 runtime list
            // 查询 main cluster 里面 所有 runtime cluster 的 runtime list
            // 查询 main cluster runtime list 的所有 meta 配置 ，追加配置
            // 如果 main cluster 支持远程操作配置，等待 sync 模块同步
            // 如果 main cluster 不支持...... 同时是self 模式，操作 k8s 更新 runtime list
        }

        //  如果 storage runtime cluster 不支持 meta 配置操作，不需要处理

        /*
            如果 main cluster 需要 relationship cluster 的 meta 集群 地址，则 runtime 集群需要重启

        */
        if (clusterRelationshipEntity.getRelationshipType().isMeta()) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterRelationshipEntity.getClusterId());
            //runtimeService.queryOnlyRuntimeByClusterId(runtimeEntity);
        } else if(clusterRelationshipEntity.getRelationshipType().isRuntime()){
            // 查询 main cluster 里面 是否存在 meta cluster ， 如果存在则需要更新


            // 如果 没有 meta cluster 不需要查询 runtime 列表
        }
    }
}
