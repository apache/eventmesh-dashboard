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

package org.apache.eventmesh.dashboard.console.service.Impl;

import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.OprGroupMemberDao;
import org.apache.eventmesh.dashboard.console.service.GroupMemberService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberServiceImp implements GroupMemberService {

    @Autowired
    OprGroupMemberDao oprGroupMemberDao;

    @Override
    @EmLog(OprType = "View", OprTarget = "GroupMember")
    public List<GroupMemberEntity> getGroupMember(GroupMemberEntity groupMemberEntity) {
        return oprGroupMemberDao.getGroupList(groupMemberEntity);
    }

    @Override
    @EmLog(OprType = "add", OprTarget = "GroupMember")
    public Integer addGroupMember(GroupMemberEntity groupMemberEntity) {
        return oprGroupMemberDao.addGroupMember(groupMemberEntity);
    }

    @Override
    public Integer updateGroupMember(GroupMemberEntity groupMemberEntity) {
        return oprGroupMemberDao.updateGroupMember(groupMemberEntity);
    }

    @Override
    public Integer deleteGroupMember(Long id) {
        return oprGroupMemberDao.deleteGroupMember(id);
    }

    @Override
    public GroupMemberEntity selectGroupMemberById(GroupMemberEntity groupMemberEntity) {
        return oprGroupMemberDao.selectGroupMemberById(groupMemberEntity);
    }

    @Override
    public List<GroupMemberEntity> selectGroupMemberByGroup(GroupEntity groupEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setGroupName(groupEntity.getName());
        groupMemberEntity.setClusterId(groupEntity.getClusterId());
        //Obtain a member who meets the conditions of a group
        return oprGroupMemberDao.selectAllMemberByUnique(groupMemberEntity);
    }

    @Override
    public List<GroupMemberEntity> selectAllMemberByTopic(GroupMemberEntity groupMemberEntity) {
        List<GroupMemberEntity> groupMemberEntities = oprGroupMemberDao.selectAllMemberByTopic(groupMemberEntity);
        return groupMemberEntities;
    }

}
