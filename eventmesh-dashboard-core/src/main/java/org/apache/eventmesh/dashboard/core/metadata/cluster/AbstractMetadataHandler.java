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

package org.apache.eventmesh.dashboard.core.metadata.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManage;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

public abstract class AbstractMetadataHandler<T, S, RE> implements MetadataHandler<T>, RemotingManage.RemotingRequestWrapper<S, RE> {


    protected S request;
    @Setter
    private RemotingManage remotingManage;

    public void init() {
        this.request = (S) remotingManage.getProxyObject();
    }


    /**
     * 同步的时候，只同步runtime 的数据，还是会同步 storage 的数据。这个可以进行配置。
     *
     * @return
     */
    @Override
    public List<T> getData() {
        List<RemotingManage.RemotingWrapper> remotingWrapperList = new ArrayList<>();
        remotingWrapperList.addAll(
            remotingManage.getEventMeshClusterDO(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.SELF));
        remotingWrapperList.addAll(
            remotingManage.getStorageCluster(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.SELF));
        return remotingManage.request(this,
            remotingManage.getEventMeshClusterDO(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.SELF));
    }

}
