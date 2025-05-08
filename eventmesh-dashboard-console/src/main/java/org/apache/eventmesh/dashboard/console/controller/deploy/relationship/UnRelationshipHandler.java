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
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

public class UnRelationshipHandler implements UpdateHandler<ClusterRelationshipEntity> {

    private ClusterService clusterService;

    private ClusterRelationshipService clusterRelationshipService;

    private RuntimeService runtimeService;

    @Override
    public void init() {

    }

    @Override
    public void handler(ClusterRelationshipEntity clusterRelationshipEntity) {
        // 取消 runtime ，什么都不用管
        // 接触 meta ，关联 集群下 runtime 都需要管理
    }

    @Override
    public List<ClusterMetadata> getData() {
        return null;
    }

    @Override
    public List<ClusterMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }
}
