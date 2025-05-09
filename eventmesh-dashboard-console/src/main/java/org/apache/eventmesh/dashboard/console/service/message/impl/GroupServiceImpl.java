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
import org.apache.eventmesh.dashboard.console.entity.message.SubscriptionEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.service.message.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberService groupMemberService;

    @Override
    public List<GroupEntity> selectAll() {
        return groupMapper.selectAll();
    }

    @Override
    public void batchInsert(List<GroupEntity> groupEntities) {
        groupMapper.batchInsert(groupEntities);
    }

    @EmLog(OprType = "search", OprTarget = "Group")
    @Override
    public List<GroupEntity> getGroupByClusterId(GroupEntity groupEntity) {
        return groupMapper.selectGroup(groupEntity);

    }

    @EmLog(OprType = "add", OprTarget = "Group")
    @Override
    public void addGroup(GroupEntity groupEntity) {
        groupMapper.addGroup(groupEntity);
    }

    @Override
    public void updateGroup(GroupEntity groupEntity) {
        groupMapper.updateGroup(groupEntity);
    }

    @Override
    public Integer deleteGroup(GroupEntity groupEntity) {
        return groupMapper.deleteGroup(groupEntity);
    }

    @Override
    public GroupEntity selectGroup(GroupEntity groupEntity) {
        return groupMapper.selectGroupById(groupEntity);
    }

    @Override
    public Integer insertMemberToGroup(SubscriptionEntity subscriptionEntity) {
        groupMemberService.addGroupMember(subscriptionEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(subscriptionEntity.getGroupName());
        groupEntity.setClusterId(subscriptionEntity.getClusterId());
        GroupEntity groupEntity1 = groupMapper.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(subscriptionEntity.getId() + "," + groupEntity1.getMembers());
        //Concatenate the members of the group
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() + 1);
        groupEntity1.setUpdateTime(LocalDateTime.now());
        groupMapper.updateGroup(groupEntity1);
        return 1;
        //Modify the group member information
    }

    @Override
    public Integer deleteMemberFromGroup(SubscriptionEntity subscriptionEntity) {
        groupMemberService.deleteGroupMember(subscriptionEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(subscriptionEntity.getGroupName());
        groupEntity.setClusterId(subscriptionEntity.getClusterId());
        GroupEntity groupEntity1 = groupMapper.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupEntity1.getMembers().replaceAll(subscriptionEntity.getId() + ",", ""));
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() - 1);
        groupEntity1.setUpdateTime(LocalDateTime.now());
        groupMapper.updateGroup(groupEntity1);
        return 1;
    }


}
