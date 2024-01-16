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

package org.apache.eventmesh.dashboard.core.service.store;

import org.apache.eventmesh.dashboard.core.config.AdminProperties;
import org.apache.eventmesh.dashboard.core.model.TopicProperties;
import org.apache.eventmesh.dashboard.core.service.TopicService;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * TODO implement methods from storage-plugin.admin
 */

@Slf4j
@Service
public class RocketmqTopicService implements TopicService {

    AdminProperties adminProperties;

    public RocketmqTopicService(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    public List<TopicProperties> getTopic() {
        return null;
    }

    @Override
    public void createTopic(String topicName) {

    }

    @Override
    public void deleteTopic(String topicName) {

    }
}
