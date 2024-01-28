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

import org.apache.eventmesh.dashboard.console.entity.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.OprGroupDao;
import org.apache.eventmesh.dashboard.console.service.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.GroupService;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    OprGroupDao oprGroupDao;

    @Autowired
    GroupMemberService groupMemberService;

    @Override
    public List<GroupEntity> getGroupList(GroupEntity groupEntity) {
        return oprGroupDao.getGroupList(groupEntity);

    }

    @Override
    public Integer addGroup(GroupEntity groupEntity) {
        return oprGroupDao.addGroup(groupEntity);
    }

    @Override
    public Integer updateGroup(GroupEntity groupEntity) {
        return oprGroupDao.updateGroup(groupEntity);
    }

    @Override
    public Integer deleteGroup(Long id) {
        return oprGroupDao.deleteGroup(id);
    }

    @Override
    public GroupEntity selectGroup(GroupEntity groupEntity) {
        return oprGroupDao.selectGroupById(groupEntity);
    }

    @Override
    public Integer insertMemberToGroup_plus(GroupMemberEntity groupMemberEntity) {
        groupMemberService.addGroupMember(groupMemberEntity);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupMemberEntity.getGroupName());
        groupEntity.setClusterId(groupMemberEntity.getClusterId());
        GroupEntity groupEntity1 = oprGroupDao.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupMemberEntity.getId() + "" + "," + groupEntity1.getMembers());
        //Concatenate the members of the group
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() + 1);
        groupEntity1.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return oprGroupDao.updateGroup(groupEntity1);
        //Modify the group member information
    }

    @Override
    public Integer deleteMemberFromGroup_plus(GroupMemberEntity groupMemberEntity) {
        groupMemberService.deleteGroupMember(groupMemberEntity.getId());
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupMemberEntity.getGroupName());
        groupEntity.setClusterId(groupMemberEntity.getClusterId());
        GroupEntity groupEntity1 = oprGroupDao.selectGroupByUnique(groupEntity);
        //^Obtain the group to which the member belongs
        groupEntity1.setMembers(groupEntity1.getMembers().replaceAll(groupMemberEntity.getId() + "" + ",", ""));
        groupEntity1.setMemberCount(groupEntity1.getMemberCount() - 1);
        groupEntity1.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return oprGroupDao.updateGroup(groupEntity1);
    }


}
