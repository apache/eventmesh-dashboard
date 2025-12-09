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

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/deploy/create/CreateClusterByCopyDTO.java
package org.apache.eventmesh.dashboard.console.model.deploy.create;

import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;

import java.util.Map;
========

package org.apache.eventmesh.dashboard.console.modle;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/OperateDTO.java

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 */
@Data
<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/deploy/create/CreateClusterByCopyDTO.java
@EqualsAndHashCode(callSuper = false)
public class CreateClusterByCopyDTO extends ClusterIdDTO {

    private String clusterName;

    private Long kubernetesClusterId;

    private Map<Long, Long> clusterAndKubernetesIdMap;
========
@EqualsAndHashCode(callSuper = true)
public class OperateDTO extends IdDTO {


    private OperationRangeType operationRangeType;

    private ClusterType clusterType;

>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/OperateDTO.java
}
