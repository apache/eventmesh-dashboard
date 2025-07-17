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


import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.GroupControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.RuntimeIdDTO;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
@RequestMapping("/user/group")
public class GroupController {


    @Autowired
    private GroupService groupService;

    @PostMapping("queryGroupListByClusterId")
    public List<GroupEntity> queryGroupListByClusterId(@RequestBody @Validated RuntimeIdDTO runtimeIdDTO) {
        return groupService.getGroupByClusterId(GroupControllerMapper.INSTANCE.queryGroupListByClusterId(runtimeIdDTO));
    }

    @PostMapping("queryGroupListByTopicId")
    public List<GroupEntity> queryGroupListByTopicId(@RequestBody @Validated IdDTO idDTO) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(idDTO.getId());
        return groupService.queryGroupListByTopicId(topicEntity);
    }

    @PostMapping("deleteGroupById")
    public Integer deleteGroupById(@RequestBody @Validated IdDTO idDTO) {
        return groupService.deleteGroup(GroupControllerMapper.INSTANCE.deleteGroupById(idDTO));
    }

}
