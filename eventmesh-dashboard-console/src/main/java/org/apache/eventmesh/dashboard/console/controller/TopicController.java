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

package org.apache.eventmesh.dashboard.console.controller;


import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.GetTopicListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;
import org.apache.eventmesh.dashboard.console.service.topic.TopicService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TopicController {

    @Autowired
    private TopicService topicService;


    @PostMapping("/cluster/topic/topicList")
    public List<TopicEntity> getTopicList(@Validated @RequestBody GetTopicListDTO getTopicListDTO) {
        return topicService.getTopicListToFront(getTopicListDTO.getClusterId(), getTopicListDTO);

    }


    @GetMapping("/cluster/topic/deleteTopic")
    public String deleteTopic(TopicEntity topicEntity) {
        try {
            topicService.deleteTopic(topicEntity);
        } catch (Exception e) {
            return e.getMessage();
        }
        return "success";
    }


    @GetMapping("/cluster/topic/showCreateTopic")
    public CreateTopicDTO showCreateTopicMessage() {
        return new CreateTopicDTO();
    }

    @PostMapping("/cluster/topic/createTopic")
    public String createTopic(@Validated @RequestBody CreateTopicDTO createTopicDTO) {
        try {
            topicService.createTopic(createTopicDTO);
        } catch (Exception e) {
            return e.getMessage();
        }
        return "success";
    }

    @GetMapping("/cluster/topic/getTopicDetailGroups")
    public List<TopicDetailGroupVO> getTopicDetailGroups(Long topicId) {
        return topicService.getTopicDetailGroups(topicId);
    }

}
