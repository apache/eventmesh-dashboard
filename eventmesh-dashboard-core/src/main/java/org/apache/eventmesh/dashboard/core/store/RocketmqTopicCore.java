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

package org.apache.eventmesh.dashboard.core.store;


import org.apache.eventmesh.dashboard.common.properties.RocketmqProperties;
import org.apache.eventmesh.dashboard.common.dto.TopicProperties;
import org.apache.eventmesh.dashboard.common.util.RocketmqUtils;
import org.apache.eventmesh.dashboard.service.store.TopicCore;

import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RocketmqTopicCore implements TopicCore {

    private final RocketmqProperties rocketmqProperties;

    public RocketmqTopicCore(RocketmqProperties rocketmqProperties) {
        this.rocketmqProperties = rocketmqProperties;
    }

    @Override
    public List<TopicProperties> getTopics() {
        return RocketmqUtils.getTopics(rocketmqProperties.getNamesrvAddr(), rocketmqProperties.getRequestTimeoutMillis());
    }

    @Override
    public void createTopic(String topicName) {
        RocketmqUtils.createTopic(topicName, TopicFilterType.SINGLE_TAG.name(),
            PermName.PERM_READ | PermName.PERM_WRITE, rocketmqProperties.getNamesrvAddr(),
            rocketmqProperties.getReadQueueNums(), rocketmqProperties.getWriteQueueNums(),
            rocketmqProperties.getRequestTimeoutMillis());
    }

    @Override
    public void deleteTopic(String topicName) {
        RocketmqUtils.deleteTopic(topicName, rocketmqProperties.getNamesrvAddr(),
            rocketmqProperties.getRequestTimeoutMillis());
    }
}
