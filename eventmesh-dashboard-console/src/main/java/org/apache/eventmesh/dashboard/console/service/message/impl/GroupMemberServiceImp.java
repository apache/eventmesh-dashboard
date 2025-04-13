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
import org.apache.eventmesh.dashboard.console.mapper.message.OprGroupMemberMapper;
import org.apache.eventmesh.dashboard.console.service.message.GroupMemberService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberServiceImp implements GroupMemberService {

    @Autowired
    OprGroupMemberMapper oprGroupMemberMapper;

    @Override
    public List<SubscriptionEntity> selectAll() {
        return oprGroupMemberMapper.selectAll();
    }

    @Override
    public void batchInsert(List<SubscriptionEntity> groupMemberEntities) {
        oprGroupMemberMapper.batchInsert(groupMemberEntities);
    }

    @Override
    @EmLog(OprType = "View", OprTarget = "GroupMember")
    public List<SubscriptionEntity> getGroupMemberByClusterId(SubscriptionEntity subscriptionEntity) {
        return oprGroupMemberMapper.getGroupByClusterId(subscriptionEntity);
    }

    @Override
    @EmLog(OprType = "add", OprTarget = "GroupMember")
    public void addGroupMember(SubscriptionEntity subscriptionEntity) {
        oprGroupMemberMapper.addGroupMember(subscriptionEntity);
    }

    @Override
    public void updateGroupMember(SubscriptionEntity subscriptionEntity) {
        oprGroupMemberMapper.updateGroupMember(subscriptionEntity);
    }

    @Override
    public SubscriptionEntity deleteGroupMember(SubscriptionEntity subscriptionEntity) {
        return oprGroupMemberMapper.deleteGroupMember(subscriptionEntity);
    }

    @Override
    public SubscriptionEntity selectGroupMemberById(SubscriptionEntity subscriptionEntity) {
        return oprGroupMemberMapper.selectGroupMemberById(subscriptionEntity);
    }

    @Override
    public List<SubscriptionEntity> selectGroupMemberByGroup(GroupEntity groupEntity) {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setGroupName(groupEntity.getName());
        subscriptionEntity.setClusterId(groupEntity.getClusterId());
        //Obtain a member who meets the conditions of a group
        return oprGroupMemberMapper.selectMember(subscriptionEntity);
    }

    @Override
    public List<SubscriptionEntity> selectAllMemberByTopic(SubscriptionEntity subscriptionEntity) {
        List<SubscriptionEntity> groupMemberEntities = oprGroupMemberMapper.selectMember(subscriptionEntity);
        return groupMemberEntities;
    }

}
