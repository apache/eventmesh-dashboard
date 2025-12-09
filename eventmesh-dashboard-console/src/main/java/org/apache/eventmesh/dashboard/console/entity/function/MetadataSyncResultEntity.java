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

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/function/MetadataSyncResultEntity.java

package org.apache.eventmesh.dashboard.console.entity.function;
========

package org.apache.eventmesh.dashboard.console.entity;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.io.Serializable;
import java.time.LocalDateTime;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/BaseEntity.java

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/function/MetadataSyncResultEntity.java
@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataSyncResultEntity extends BaseRuntimeIdEntity {
========
/**
 * Base Entity provide some basic fields that every Eventmesh Dashboard Entity would have
 * <p>
 * 12 broker -> 12 queue ， 11 queue ，  1broker 没有 队列。 副本，随机出现在一个 broker
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name = "BaseEntity", description = "Base entity")
@Deprecated
public class BaseEntity implements Serializable {
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/BaseEntity.java


<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/function/MetadataSyncResultEntity.java
    private Long syncId;

    private MetadataType metadataType;

    private String errorType;

    private ClusterTrusteeshipType clusterTrusteeshipType;

    private boolean isFast = false;

    private SyncErrorType syncErrorType;

    private String resultData;

========
    /**
     * 集群id，不是 eventmesh集群id。
     */
    protected Long clusterId;

    protected ClusterType clusterType;

    protected LocalDateTime createTime;

    protected LocalDateTime updateTime;

    private Integer status;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/entity/BaseEntity.java
}
