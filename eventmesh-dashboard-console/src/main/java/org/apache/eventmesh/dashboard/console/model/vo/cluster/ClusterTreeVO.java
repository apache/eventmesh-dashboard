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

import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClusterTreeVO extends ClusterIdDTO {

    private ClusterType nodeType;

    private Long id;

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

    /**
     * 不想加字段，这个字段作为 port 的索引，目前只支持范围获得
     */
    private Integer runtimeIndex;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTimestamp;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTimestamp;


    private LocalDateTime startTimestamp;


    private String jmxProperties;

    private String config;

    private String description;

    private String authType;


    /**
     * 如何解除共享
     */
    private ClusterOwnType clusterOwnType;


    private String host;

    private Integer port;

    private Integer jmxPort;

    private Integer adminPort;

    private String rack;

    private String endpointMap;

    private String createScriptContent;

    private Long kubernetesClusterId;


    private List<ClusterTreeVO> children;

    private List<ClusterTreeVO> runtime;

    private List<Map<String, String>> portInfo;

}
