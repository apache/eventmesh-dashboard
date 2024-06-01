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

package org.apache.eventmesh.dashboard.console.service.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.ResourceNumVO;

import java.util.List;

/**
 * cluster data service
 */
public interface ClusterService {

    GetClusterBaseMessageVO getClusterBaseMessage(Long clusterId);

    ResourceNumVO getResourceNumByCluster(Long clusterId);

    void batchInsert(List<ClusterEntity> clusterEntities);

    List<ClusterEntity> selectAll();

    List<ClusterEntity> selectNewlyIncreased(ClusterEntity clusterEntity);

    void addCluster(ClusterEntity cluster);

    List<ClusterEntity> selectAllCluster();

    ClusterEntity selectClusterById(ClusterEntity cluster);

    void updateClusterById(ClusterEntity cluster);

    void deactivate(ClusterEntity cluster);
}
