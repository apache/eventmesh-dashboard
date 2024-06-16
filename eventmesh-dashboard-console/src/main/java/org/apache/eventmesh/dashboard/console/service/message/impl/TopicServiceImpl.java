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
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.OprGroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.mapper.storage.StoreMapper;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TopicServiceImpl implements TopicService {

    @Autowired
    TopicMapper topicMapper;

    @Autowired
    OprGroupMemberMapper oprGroupMemberMapper;

    @Autowired
    HealthCheckResultMapper healthCheckResultMapper;

    @Autowired
    ConfigMapper configMapper;

    @Autowired
    RuntimeMapper runtimeMapper;

    @Autowired
    StoreMapper storeMapper;

    @Autowired
    OprGroupMapper groupMapper;


    @Override
    public List<TopicDetailGroupVO> selectTopicDetailGroups(Long topicId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicId);
        topicEntity = this.selectTopicById(topicEntity);
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(topicEntity.getClusterId());
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        List<String> groupNamelist = oprGroupMemberMapper.selectGroupNameByTopicName(groupMemberEntity);
        ArrayList<TopicDetailGroupVO> topicDetailGroupVOList = new ArrayList<>();
        TopicEntity finalTopicEntity = topicEntity;
        groupNamelist.forEach(n -> {
            TopicDetailGroupVO topicDetailGroupVO = new TopicDetailGroupVO();
            topicDetailGroupVO.setGroupName(n);
            groupMemberEntity.setGroupName(n);
            List<String> list = oprGroupMemberMapper.selectTopicsByGroupNameAndClusterId(groupMemberEntity);
            topicDetailGroupVO.setTopics(list);
            GroupEntity groupEntity = new GroupEntity();
            groupEntity.setClusterId(finalTopicEntity.getClusterId());
            groupEntity.setName(n);
            GroupEntity group = groupMapper.selectGroupByNameAndClusterId(groupEntity);
            topicDetailGroupVO.setMemberNum(group.getMemberCount());
            topicDetailGroupVO.setState(group.getState());
            topicDetailGroupVOList.add(topicDetailGroupVO);
        });
        return topicDetailGroupVOList;
    }

    @EmLog(OprType = "add", OprTarget = "topic")
    @Override
    public void createTopic(TopicEntity topicEntity) {
        topicEntity.setCreateProgress(1);
        topicMapper.insertTopic(topicEntity);
    }

    @Override
    public void batchInsert(List<TopicEntity> topicEntities) {
        topicMapper.batchInsert(topicEntities);
    }

    @Override
    public List<TopicEntity> selectAll() {
        return topicMapper.selectAll();
    }


    @Override
    public void insertTopic(TopicEntity topicEntity) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        groupMemberEntity.setState("active");
        oprGroupMemberMapper.updateMemberByTopic(groupMemberEntity);
        topicMapper.insertTopic(topicEntity);
    }

    @Override
    public Integer updateTopic(TopicEntity topicEntity) {
        return topicMapper.updateTopic(topicEntity);
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
    public Integer deleteTopic(TopicEntity topicEntity) {
        return topicMapper.deleteTopic(topicEntity);
    }

    @Override
    public List<TopicEntity> selectTopiByCluster(TopicEntity topicEntity) {
        return topicMapper.selectTopicByCluster(topicEntity);
    }

    @Override
    public List<TopicEntity> selectTopicListToFront(TopicEntity topicEntity) {
        List<TopicEntity> topicEntityList = topicMapper.queryTopicsToFrontByClusterId(topicEntity);
        topicEntityList.forEach(n -> {
            n.setStatus(CheckResultCache.getINSTANCE().getLastHealthyCheckResult("topic", n.getId()));
        });
        return topicEntityList;
    }


}
