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

import org.apache.eventmesh.dashboard.common.dto.Result;
import org.apache.eventmesh.dashboard.console.dto.CreateTopicRequest;
import org.apache.eventmesh.dashboard.console.dto.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.service.dto.TopicProperties;
import org.apache.eventmesh.dashboard.service.store.TopicCore;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/topic")
public class TopicController {

    /**
     * TODO expose implement by FunctionManager
     */
    TopicCore topicCore;

    /**
     * TODO Is OPTIONS method and @CrossOrigin necessary?
     */
    @CrossOrigin
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Object> preflight() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "*")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Max-Age", "86400")
            .build();
    }

    @CrossOrigin
    @GetMapping
    public Result<List<TopicProperties>> getList() {
        List<TopicProperties> topicList = topicCore.getTopics();
        return Result.success(topicList);
    }

    @CrossOrigin
    @PostMapping
    public Result<Object> create(@RequestBody CreateTopicRequest createTopicRequest) {
        String topicName = createTopicRequest.getName();
        topicCore.createTopic(topicName);
        return Result.success();
    }

    @CrossOrigin
    @DeleteMapping
    public Result<Object> delete(@RequestBody DeleteTopicRequest deleteTopicRequest) {
        String topicName = deleteTopicRequest.getName();
        topicCore.deleteTopic(topicName);
        return Result.success();
    }
}