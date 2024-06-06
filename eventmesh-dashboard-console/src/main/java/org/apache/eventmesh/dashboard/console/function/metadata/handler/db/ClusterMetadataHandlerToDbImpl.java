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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClusterMetadataHandlerToDbImpl implements MetadataHandler<ClusterMetadata> {

    @Autowired
    private ClusterService clusterService;

    @Override
    public void addMetadata(ClusterMetadata meta) {
        //clusterService.addCluster(new ClusterEntity(meta));
    }

    @Override
    public void addMetadata(List<ClusterMetadata> metadataList) {
      /*  List<ClusterEntity> entityList = metadataList.stream()
            .map(ClusterEntity::new)
            .collect(Collectors.toList());
        clusterService.batchInsert(entityList);*/
    }

    @Override
    public void deleteMetadata(ClusterMetadata meta) {
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
