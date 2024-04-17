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


import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.group.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.groupmember.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.health.CheckResultCache;
import org.apache.eventmesh.dashboard.console.mapper.config.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.group.OprGroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.groupmember.OprGroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.health.HealthCheckResultMapper;
import org.apache.eventmesh.dashboard.console.mapper.runtime.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.storage.StoreMapper;
import org.apache.eventmesh.dashboard.console.mapper.topic.TopicMapper;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.GetTopicListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;

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
    public List<TopicDetailGroupVO> getTopicDetailGroups(Long topicId) {
        TopicEntity topicEntity = this.selectTopicById(topicId);
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(topicEntity.getClusterId());
        groupMemberEntity.setTopicName(topicEntity.getTopicName());
        List<String> groupNamelist = oprGroupMemberMapper.selectGroupNameByTopicName(groupMemberEntity);
        ArrayList<TopicDetailGroupVO> topicDetailGroupVOList = new ArrayList<>();
        groupNamelist.forEach(n -> {
            TopicDetailGroupVO topicDetailGroupVO = new TopicDetailGroupVO();
            topicDetailGroupVO.setGroupName(n);
            groupMemberEntity.setGroupName(n);
            List<String> list = oprGroupMemberMapper.selectTopicsByGroupNameAndClusterId(groupMemberEntity);
            topicDetailGroupVO.setTopics(list);
            GroupEntity groupEntity = new GroupEntity();
            groupEntity.setClusterId(topicEntity.getClusterId());
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
    public void createTopic(CreateTopicDTO createTopicDTO) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setType(0);
        topicEntity.setClusterId(createTopicDTO.getClusterId());
        topicEntity.setTopicName(createTopicDTO.getName());
        topicEntity.setDescription(createTopicDTO.getDescription());
        topicEntity.setRetentionMs(createTopicDTO.getSaveTime());
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setClusterId(topicEntity.getClusterId());
        topicEntity.setStorageId(String.valueOf(storeMapper.selectStoreByCluster(storeEntity).getId()));
        topicEntity.setCreateProgress(1);
        topicMapper.addTopic(topicEntity);
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
    public TopicEntity selectTopicById(Long topicId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicId);
        return topicMapper.selectTopicById(topicEntity);
    }

    @Override
    public TopicEntity selectTopicByUnique(TopicEntity topicEntity) {
        return topicMapper.selectTopicByUnique(topicEntity);
    }


    @Override
    public void deleteTopic(TopicEntity topicEntity) {
        topicMapper.deleteTopic(topicEntity);
    }

    @Override
    public List<TopicEntity> selectTopiByCluster(Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        return topicMapper.selectTopicByCluster(topicEntity);
    }


    public TopicEntity setSearchCriteria(GetTopicListDTO getTopicListDTO, TopicEntity topicEntity) {
        topicEntity.setTopicName(getTopicListDTO.getTopicName());
        return topicEntity;
    }

    @Override
    public List<TopicEntity> getTopicListToFront(Long clusterId, GetTopicListDTO getTopicListDTO) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        topicEntity = this.setSearchCriteria(getTopicListDTO, topicEntity);
        List<TopicEntity> topicEntityList = topicMapper.getTopicsToFrontByClusterId(topicEntity);
        topicEntityList.forEach(n -> {
            n.setStatus(CheckResultCache.getLastHealthyCheckResult("topic", n.getId()));
        });
        return topicEntityList;
    }


}
