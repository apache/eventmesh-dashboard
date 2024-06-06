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

package org.apache.eventmesh.dashboard.core.metadata.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsRequest;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TopicSyncFromClusterService extends AbstractMetadataHandler<TopicMetadata, TopicRemotingService, GetTopicsRequest> {

    @Override
    public void addMetadata(TopicMetadata meta) {

    }

    @Override
    public void deleteMetadata(TopicMetadata meta) {

    }


    @Override
    public List<TopicMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }


    @Override
    public GlobalResult request(TopicRemotingService topicRemotingService, GetTopicsRequest key) {
        return null;
    }
}
