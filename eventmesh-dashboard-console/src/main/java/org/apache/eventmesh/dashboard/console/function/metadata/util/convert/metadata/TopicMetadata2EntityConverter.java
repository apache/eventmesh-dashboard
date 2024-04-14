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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

public class TopicMetadata2EntityConverter implements Converter<TopicMetadata, TopicEntity> {

    @Override
    public TopicEntity convert(TopicMetadata source) {
        return TopicEntity.builder()
            .clusterId(source.getClusterId())
            .topicName(source.getTopicName())
            .storageId(source.getStorageId())
            .retentionMs(source.getRetentionMs())
            .type(source.getType())
            .description(source.getDescription())
            .status(1)
            .clusterId(source.getClusterId())
            .createProgress(1)
            .build();
    }

    @Override
    public String getUnique(TopicMetadata source) {
        return source.getTopicName();
    }
}
