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


<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/connector/ResourcesConfigService.java
package org.apache.eventmesh.dashboard.console.service.connector;

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
========
package org.apache.eventmesh.dashboard.console.service.function;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.MetadataSyncResultEntity;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/function/MetadataSyncResultService.java

import java.util.List;


/**
 *
 */
<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/connector/ResourcesConfigService.java
public interface ResourcesConfigService {


    List<ResourcesConfigEntity> queryByRuntimeList(List<RuntimeEntity> runtimeEntityList);


    void insertResources(ResourcesConfigEntity resourcesConfigEntity);


    ResourcesConfigEntity queryResourcesById(ResourcesConfigEntity resourcesConfigEntity);


    List<ResourcesConfigEntity> queryResourcesByOrganizationId(ResourcesConfigEntity resourcesConfigEntity);

    void copyResources(ResourcesConfigEntity resourcesConfigEntity);
========
public interface MetadataSyncResultService {


    void bachMetadataSyncResult(List<MetadataSyncResultEntity> healthCheckResultEntityList, List<RuntimeEntity> runtimeList,
        List<ClusterEntity> clusterEntityList);


    List<MetadataSyncResultEntity> queueHealthCheckResultEntityList(MetadataSyncResultEntity healthCheckResultEntity);
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/function/MetadataSyncResultService.java
}
