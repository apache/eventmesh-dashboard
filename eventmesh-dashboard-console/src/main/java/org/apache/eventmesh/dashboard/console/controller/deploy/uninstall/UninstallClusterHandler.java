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


package org.apache.eventmesh.dashboard.console.controller.deploy.uninstall;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.domain.ClusterAndRuntimeDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.modle.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UninstallClusterHandler implements UpdateHandler<ClusterEntity> {

    @Autowired
    private ClusterAndRuntimeDomain clusterAndRuntimeDomain;

    @Override
    public void init() {

    }

    @Override
    public void handler(ClusterEntity clusterEntity) {

        // 查询所有 cluster 标记 状态
        ClusterAndRuntimeOfRelationshipDO clusterAndRuntimeOfRelationshipDO = clusterAndRuntimeDomain.getAllClusterAndRuntimeByCluster(clusterEntity);
        // 查询 所有 runtime 标记 ing
    }
}
