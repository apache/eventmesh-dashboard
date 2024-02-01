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

package org.apache.eventmesh.dashboard.console.service.topic;

import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.groupmember.OprGroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.topic.TopicMapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TopicServiceImpl implements TopicService {

    @Autowired
    TopicMapper topicMapper;

    @Autowired
    OprGroupMemberMapper oprGroupMemberMapper;


    @Override
    public List<TopicEntity> getTopicList(TopicEntity topicEntity) {
        return topicMapper.getTopicListByClusterId(topicEntity);
    }

    @Override
    public TopicEntity addTopic_plus(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("active");
        oprGroupMemberMapper.updateMemberByTopic(groupMemberEntity);
        return topicMapper.addTopic(topicEntity);
    }

    @Override
    public TopicEntity updateTopic(TopicEntity topicEntity) {
        return topicMapper.updateTopic(topicEntity);
    }

    @Override
    public TopicEntity deleteTopic(TopicEntity topicEntity) {
        return topicMapper.deleteTopic(topicEntity);
    }

    @Override
    public TopicEntity selectTopicById(TopicEntity topicEntity) {
        return topicMapper.selectTopicById(topicEntity);
    }

    @Override
    public TopicEntity selectTopicByUnique(TopicEntity topicEntity) {
        return topicMapper.selectTopicByUnique(topicEntity);
    }

    @Override
    public TopicEntity deleteTopic_plus(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("empty");
        oprGroupMemberMapper.updateMemberByTopic(groupMemberEntity);
        return topicMapper.deleteTopic(topicEntity);
    }
}
