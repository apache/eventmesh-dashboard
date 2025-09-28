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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.util.Objects;

public abstract class BaseClusterIdBase extends BaseOrganizationBase {

    private Long clusterId;

    private ClusterType clusterType;

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public boolean isDelete() {
        return this.getStatus() == 0;
    }

    public boolean isUpdate() {
        return !this.isDelete() && !this.isInsert();
    }

    public boolean isInsert() {
        return Objects.equals(this.getUpdateTime() , this.getCreateTime());
    }

}
