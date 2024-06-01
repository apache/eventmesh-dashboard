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

package org.apache.eventmesh.dashboard.console.function.optration;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicRequest;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.springframework.stereotype.Service;

import lombok.Setter;

@Service
public class TopicMetadataHandlerToClusterImpl implements MetadataHandler<TopicEntity> {

    @Setter
    private TopicRemotingService topicRemotingService;

    @Override
    public void addMetadata(TopicEntity meta) {
        TopicMetadata topicMetadata = new TopicMetadata();
        topicMetadata.setStoreAddress("");
        topicMetadata.setConnectionUrl("");
        topicMetadata.setTopicName(meta.getTopicName());
        topicMetadata.setRuntimeId(0L);
        topicMetadata.setStorageId(meta.getStorageId());
        topicMetadata.setRetentionMs(0L);
        topicMetadata.setType(meta.getType());
        topicMetadata.setDescription(meta.getDescription());
        topicMetadata.setRegistryAddress("");
        topicMetadata.setClusterId(0L);

        topicRemotingService.createTopic(new CreateTopicRequest(topicMetadata));
    }

    @Override
    public void deleteMetadata(TopicEntity meta) {

    }
}
