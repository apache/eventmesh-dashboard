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
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandler;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.entity.TopicEntity2MetadataConverter;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.springframework.stereotype.Service;

import lombok.Setter;

@Service
public class TopicMetadataHandlerToClusterImpl implements MetadataHandler<TopicEntity> {

    @Setter
    private TopicRemotingService topicRemotingService;

    private static final Converter<TopicEntity, TopicMetadata> converter = new TopicEntity2MetadataConverter();

    @Override
    public void addMetadata(TopicEntity meta) {
        TopicMetadata topicMetadata = converter.convert(meta);

        topicRemotingService.createTopic(new CreateTopicRequest(topicMetadata));
    }

    @Override
    public void deleteMetadata(TopicEntity meta) {
        TopicMetadata topicMetadata = converter.convert(meta);

        topicRemotingService.deleteTopic(new DeleteTopicRequest(topicMetadata));
    }
}
