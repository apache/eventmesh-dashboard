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


import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.model.DO.runtime.QueryRuntimeByBigExpandClusterDO;
import org.apache.eventmesh.dashboard.console.model.deploy.ClusterAllMetadataDO;

import java.util.List;

/**
 * Runtime data service
 */
public interface RuntimeService {

    RuntimeEntity queryRuntimeEntityById(RuntimeEntity runtimeEntity);

    List<RuntimeEntity> queryRuntimeToFrontByClusterId(RuntimeEntity runtimeEntity);

    List<RuntimeEntity> queryRuntimeToFrontByClusterIdList(List<ClusterEntity> clusterEntityList);

    List<RuntimeEntity> queryRuntimeByBigExpandCluster(QueryRuntimeByBigExpandClusterDO data);

    List<RuntimeEntity> queryMetaRuntimeByStorageClusterId(QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO);

    ClusterAllMetadataDO queryAllByClusterId(RuntimeEntity runtimeEntity, boolean isRuntime, boolean isRelationship);

    List<RuntimeEntity> selectAll();

    List<RuntimeEntity> queryByUpdateTime(RuntimeEntity runtimeEntity);

    void updateAddressByRuntimeId(RuntimeEntity runtimeEntity);

    Integer batchUpdate(List<RuntimeEntity> runtimeEntities);

    void batchUpdateDeployStatusType(List<RuntimeEntity> runtimeEntities);

    void batchUpdateDeployStatusType(List<RuntimeEntity> runtimeEntities, DeployStatusType deployStatusType);

    void insertRuntimeByClusterData(RuntimeEntity runtimeEntity);

    void insertRuntime(RuntimeEntity runtimeEntity);

    void batchInsert(List<RuntimeEntity> runtimeEntities);

    void updateRuntimeByCluster(RuntimeEntity runtimeEntity);

    void deleteRuntimeByCluster(RuntimeEntity runtimeEntity);

    void deactivate(RuntimeEntity runtimeEntity);
}
