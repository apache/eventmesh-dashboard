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


package org.apache.eventmesh.dashboard.core.metadata.result;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.common.enums.SyncType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;

import lombok.Data;

/**
 * cluster or runtime 每次同步行为记录
 */
@Data
public class MetadataSyncResult {

    private String key;

    private BaseSyncBase baseSyncBase;

    private MetadataType metadataType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private SyncErrorType syncErrorType;

    /**
     * 同步结果
     */
    private FirstToWhom firstToWhom = FirstToWhom.NOT;

    private SyncType syncType;

    private boolean success = false;


}
