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

package org.apache.eventmesh.dashboard.console.service.message;

import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;

import java.util.List;

/**
 * Service about Topic
 */
public interface TopicService {

    List<TopicDetailGroupVO> selectTopicDetailGroups(Long topicId);

    void createTopic(TopicEntity topicEntity);

    void batchInsert(List<TopicEntity> topicEntities);

    List<TopicEntity> selectAll();

    void insertTopic(TopicEntity topicEntity);

    Integer updateTopic(TopicEntity topicEntity);

    void deleteTopicById(TopicEntity topicEntity);

    TopicEntity selectTopicById(TopicEntity topicEntity);

    Integer deleteTopic(TopicEntity topicEntity);

    List<TopicEntity> selectTopiByCluster(TopicEntity topicEntity);

    List<TopicEntity> selectTopicListToFront(TopicEntity topicEntity);
}
