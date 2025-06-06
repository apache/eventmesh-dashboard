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


package org.apache.eventmesh.dashboard.common.model.base;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseSyncBase extends BaseClusterIdBase {


    private ClusterTrusteeshipType trusteeshipType;

    private FirstToWhom firstToWhom;

    private FirstToWhom firstSyncState;

    private ReplicationType replicationType;

    private SyncErrorType syncErrorType;

    private String config;

    /**
     * 上线时间
     */
    private LocalDateTime onlineTimestamp;

    /**
     * 下线时间
     */
    private LocalDateTime offlineTimestamp;


    private LocalDateTime startTimestamp;

    public boolean isCluster() {
        return true;
    }

}
