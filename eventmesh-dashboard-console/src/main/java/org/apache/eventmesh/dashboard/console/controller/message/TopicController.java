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

package org.apache.eventmesh.dashboard.console.controller.message;


import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.message.TopicControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.GetTopicListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("topic")
public class TopicController {

    @Autowired
    private TopicService topicService;


    @PostMapping("/queryTopicListByClusterId")
    public List<TopicEntity> queryTopicListByClusterId(@Validated @RequestBody GetTopicListDTO getTopicListDTO) {
        return topicService.selectTopicListToFront(TopicControllerMapper.INSTANCE.queryTopicListByClusterId(getTopicListDTO));
    }

    @PostMapping("queryTopicListById ")
    public TopicEntity queryTopicById(@Validated @RequestBody IdDTO idDTO) {
        return topicService.selectTopicById(TopicControllerMapper.INSTANCE.queryTopicListById(idDTO));
    }

    @GetMapping("deleteTopic")
    public Integer deleteTopic(@Validated @RequestBody IdDTO idDTO) {
        return topicService.deleteTopic(TopicControllerMapper.INSTANCE.deleteTopic(idDTO));
    }

    @PostMapping("createTopic")
    public void createTopic(@Validated @RequestBody CreateTopicDTO createTopicDTO) {
        topicService.createTopic(TopicControllerMapper.INSTANCE.createTopic(createTopicDTO));
    }

    /**
     * TODO delete
     *
     * @param topicId
     * @return
     */
    @GetMapping("/cluster/topic/getTopicDetailGroups")
    public List<TopicDetailGroupVO> getTopicDetailGroups(Long topicId) {
        return topicService.selectTopicDetailGroups(topicId);
    }

}
