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
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.service.message.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;

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
    public List<GroupEntity> queryGroupListByTopicId(TopicEntity topicEntity) {
        return this.groupMapper.queryGroupListByTopicId(topicEntity);
    }

    @Override
    public void batchInsert(List<GroupEntity> groupEntities) {
        groupMapper.batchInsert(groupEntities);
    }

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
    public Integer deleteGroup(GroupEntity groupEntity) {
        return groupMapper.deleteGroup(groupEntity);
    }

}
