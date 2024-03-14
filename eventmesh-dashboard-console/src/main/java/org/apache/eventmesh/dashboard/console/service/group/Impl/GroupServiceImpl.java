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

package org.apache.eventmesh.dashboard.console.service.group.Impl;

import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.group.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.groupmember.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.group.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.service.group.GroupService;
import org.apache.eventmesh.dashboard.console.service.groupmember.GroupMemberService;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    OprGroupMapper oprGroupMapper;

    @Autowired
    GroupMemberService groupMemberService;

    @Override
    public List<GroupEntity> selectAll() {
        return oprGroupMapper.selectAll();
    }

    @Override
    public void batchInsert(List<GroupEntity> groupEntities) {
        oprGroupMapper.batchInsert(groupEntities);
    }

    @EmLog(OprType = "search", OprTarget = "Group")
    @Override
    public List<GroupEntity> getGroupByClusterId(GroupEntity groupEntity) {
        return oprGroupMapper.selectGroup(groupEntity);

    }

    @EmLog(OprType = "add", OprTarget = "Group")
    @Override
    public GroupEntity addGroup(GroupEntity groupEntity) {
        oprGroupMapper.addGroup(groupEntity);
        return groupEntity;
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
        groupMemberService.addGroupMember(groupMemberEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupMemberEntity.getGroupName());
        groupEntity.setClusterId(groupMemberEntity.getClusterId());
        GroupEntity groupEntity1 = oprGroupMapper.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupMemberEntity.getId() + "" + "," + groupEntity1.getMembers());
        //Concatenate the members of the group
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() + 1);
        groupEntity1.setUpdateTime(new Timestamp(System.currentTimeMillis()));
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
        groupEntity1.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        oprGroupMapper.updateGroup(groupEntity1);
        return 1;
    }


}
