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

import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.OprGroupMemberDao;
import org.apache.eventmesh.dashboard.console.mapper.TopicDao;
import org.apache.eventmesh.dashboard.console.service.TopicService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TopicServiceImpl implements TopicService {

    @Autowired
    TopicDao topicDao;

    @Autowired
    OprGroupMemberDao oprGroupMemberDao;


    @Override
    public List<TopicEntity> getTopicList(TopicEntity topicEntity) {
        return topicDao.getTopicList(topicEntity);
    }

    @Override
    public Integer addTopic_plus(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("active");
        oprGroupMemberDao.updateMemberByTopic(groupMemberEntity);
        return topicDao.addTopic(topicEntity);
    }

    @Override
    public Integer updateTopic(TopicEntity topicEntity) {
        return topicDao.updateTopic(topicEntity);
    }

    @Override
    public Integer deleteTopic(Long id) {
        return topicDao.deleteTopic(id);
    }

    @Override
    public TopicEntity selectTopicById(TopicEntity topicEntity) {
        return topicDao.selectTopicById(topicEntity);
    }

    @Override
    public TopicEntity selectTopicByUnique(TopicEntity topicEntity) {
        return topicDao.selectTopicByUnique(topicEntity);
    }

    @Override
    public Integer deleteTopic_plus(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("empty");
        oprGroupMemberDao.updateMemberByTopic(groupMemberEntity);
        return topicDao.deleteTopic(topicEntity.getId());
    }
}