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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupMetadataHandlerToDbImpl implements MetadataHandler<GroupMetadata> {

    @Autowired
    GroupService groupService;

    @Override
    public void addMetadata(GroupMetadata meta) {
       /* meta.setMemberCount(0);
        GroupEntity groupEntity = new GroupEntity(meta);
        groupService.addGroup(groupEntity);*/
    }

    @Override
    public void addMetadata(List<GroupMetadata> metadata) {
        /*List<GroupEntity> entityList = metadata.stream().map(GroupEntity::new).collect(Collectors.toList());
        groupService.batchInsert(entityList);*/
    }

    @Override
    public void deleteMetadata(GroupMetadata meta) {

    }

    @Override
    public List<GroupMetadata> getData() {
        return null;
    }

    @Override
    public List<GroupMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }
}
