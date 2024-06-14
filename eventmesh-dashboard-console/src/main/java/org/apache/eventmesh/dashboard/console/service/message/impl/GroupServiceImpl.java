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

package org.apache.eventmesh.dashboard.console.service.message.impl;

import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.service.message.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private OprGroupMapper oprGroupMapper;

    @Autowired
    private GroupMemberService groupMemberService;

    @Override
    public List<GroupEntity> selectAll() {
        return oprGroupMapper.selectAll();
    }

    @Override
    public Integer batchInsert(List<GroupEntity> groupEntities) {
        return oprGroupMapper.batchInsert(groupEntities);
    }

    @EmLog(OprType = "search", OprTarget = "Group")
    @Override
    public List<GroupEntity> selectGroupByClusterId(GroupEntity groupEntity) {
        return oprGroupMapper.selectGroup(groupEntity);

    }

    @EmLog(OprType = "add", OprTarget = "Group")
    @Override
    public void insertGroup(GroupEntity groupEntity) {
        oprGroupMapper.insertGroup(groupEntity);
    }

    @Override
    public void updateGroup(GroupEntity groupEntity) {
        oprGroupMapper.updateGroup(groupEntity);
    }

    @Override
    public Integer deleteGroup(GroupEntity groupEntity) {
        return oprGroupMapper.deleteGroup(groupEntity);
    }

    @Override
    public GroupEntity selectGroup(GroupEntity groupEntity) {
        return oprGroupMapper.selectGroupById(groupEntity);
    }

    @Override
    public Integer insertMemberToGroup(GroupMemberEntity groupMemberEntity) {
        groupMemberService.insertGroupMember(groupMemberEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupMemberEntity.getGroupName());
        groupEntity.setClusterId(groupMemberEntity.getClusterId());
        GroupEntity groupEntity1 = oprGroupMapper.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupMemberEntity.getId() + "" + "," + groupEntity1.getMembers());
        //Concatenate the members of the group
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() + 1);
        groupEntity1.setUpdateTime(LocalDateTime.now());
        oprGroupMapper.updateGroup(groupEntity1);
        return 1;
        //Modify the group member information
    }

    @Override
    public Integer deleteMemberFromGroup(GroupMemberEntity groupMemberEntity) {
        groupMemberService.deleteGroupMember(groupMemberEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupMemberEntity.getGroupName());
        groupEntity.setClusterId(groupMemberEntity.getClusterId());
        GroupEntity groupEntity1 = oprGroupMapper.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupEntity1.getMembers().replaceAll(groupMemberEntity.getId() + "" + ",", ""));
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() - 1);
        groupEntity1.setUpdateTime(LocalDateTime.now());
        oprGroupMapper.updateGroup(groupEntity1);
        return 1;
    }


}
