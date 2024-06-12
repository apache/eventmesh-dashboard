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

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManager;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupSyncFromClusterService extends AbstractMetadataHandler<GroupMetadata, GroupRemotingService, GetGroupsRequest> {

    @Autowired
    private RemotingManager remotingManager;

    @Override
    public void addMetadata(GroupMetadata meta) {

    }

    @Override
    public void deleteMetadata(GroupMetadata meta) {

    }

    @Override
    public List<GroupMetadata> getData() {
        List<GroupMetadata> eventMeshGroupList = this.remotingManager.request(this, remotingManager.getEventMeshClusterDO());
        //List<GroupMetadata> rocketMQMeshGroupList = this.remotingManager.request(this,remotingManager.getRocketMQClusterDO() );
        //eventMeshGroupList.addAll(rocketMQMeshGroupList);
        return eventMeshGroupList;
    }

    @Override
    public List<GroupMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }

    @Override
    public GlobalResult request(GroupRemotingService groupRemotingService, GetGroupsRequest key) {
        return null;
    }
}
