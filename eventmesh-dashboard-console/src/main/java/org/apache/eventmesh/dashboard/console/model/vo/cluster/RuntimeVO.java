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


package org.apache.eventmesh.dashboard.console.model.vo.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RuntimeVO {

    private Long id;

    private LocalDateTime createTime;

    private Long createUserId;

    private LocalDateTime updateTime;

    private Long updateUserId;

    private Long status;

    private Integer isDelete;

    private Long organizationId;

    private ClusterType clusterType;

    private String name;

    private String version;

    private ClusterTrusteeshipType trusteeshipType;

    private FirstToWhom firstToWhom;

    private FirstToWhom firstSyncState;

    private ReplicationType replicationType;

    private SyncErrorType syncErrorType;

    private DeployStatusType deployStatusType;

    private DeployStatusType createMethod;

    private Long resourcesConfigId;

    private Long deployScriptId;

    private String deployScriptName;

    private String deployScriptVersion;

    private Integer runtimeIndex;

    private LocalDateTime onlineTimestamp;

    private LocalDateTime offlineTimestamp;

    private LocalDateTime startTimestamp;

    private Long clusterId;

    private String host;

    private String podHost;

    private Integer port;

    private Integer jmxPort;

    private Integer adminPort;

    private String rack;

    private String endpointMap;

    private String createScriptContent;

    private Long kubernetesClusterId;

    private String authType;

    private Long healthStatus;
}
