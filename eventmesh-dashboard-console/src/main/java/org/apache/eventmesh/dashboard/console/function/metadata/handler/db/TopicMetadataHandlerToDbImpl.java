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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.console.entity.StoreEntity;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;
import org.apache.eventmesh.dashboard.console.service.store.StoreService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TopicMetadataHandlerToDbImpl implements MetadataHandler<TopicMetadata> {

    @Autowired
    private TopicService topicService;

    @Autowired
    private StoreService storeService;

    @Override
    public void addMetadata(TopicMetadata meta) {
        if (Objects.nonNull(meta.getConnectionUrl())) {
            URI uri = URI.create(meta.getConnectionUrl());
            if (Objects.nonNull(uri.getHost()) && uri.getPort() != -1) {
                StoreEntity store = storeService.selectByHostPort(uri.getHost(), uri.getPort());
                if (Objects.nonNull(store)) {
                    meta.setStorageId(store.getId());
                }
            }
        }

        //topicService.addTopic(new TopicEntity(meta));
    }

    @Override
    public void deleteMetadata(TopicMetadata meta) {
        topicService.deleteTopic(null);
    }

    @Override
    public List<TopicMetadata> getData() {
        return null;
    }

    @Override
    public List<TopicMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }
}
