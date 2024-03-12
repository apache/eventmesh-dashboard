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

import org.apache.eventmesh.dashboard.console.entity.groupmember.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
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
    public void batchInsert(List<TopicEntity> topicEntities) {
        topicMapper.batchInsert(topicEntities);
    }

    @Override
    public List<TopicEntity> selectAll() {
        return topicMapper.selectAll();
    }

    @Override
    public Integer selectTopicNumByCluster(Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        return topicMapper.selectTopicNumByCluster(topicEntity);
    }

    @Override
    public List<TopicEntity> getTopicList(TopicEntity topicEntity) {
        return topicMapper.getTopicList(topicEntity);
    }

    @Override
    public void addTopic(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("active");
        oprGroupMemberMapper.updateMemberByTopic(groupMemberEntity);
        topicMapper.addTopic(topicEntity);
    }

    @Override
    public void updateTopic(TopicEntity topicEntity) {
        topicMapper.updateTopic(topicEntity);
    }

    @Override
    public void deleteTopicById(TopicEntity topicEntity) {
        topicMapper.deleteTopic(topicEntity);
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
    public void deleteTopic(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("empty");
        oprGroupMemberMapper.updateMemberByTopic(groupMemberEntity);
        topicMapper.deleteTopic(topicEntity);
    }

    @Override
    public List<TopicEntity> selectTopiByCluster(Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        return topicMapper.selectTopicByCluster(topicEntity);
    }


}
