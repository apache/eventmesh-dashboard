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

package org.apache.eventmesh.dashboard.console.function.metadata.syncservice.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsRequest;
import org.apache.eventmesh.dashboard.console.cache.RuntimeCache;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.springframework.stereotype.Service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TopicSyncFromClusterService implements SyncDataService<TopicMetadata> {

    @Setter
    TopicRemotingService topicRemotingService;

    public List<TopicMetadata> getData() {
        ConcurrentLinkedDeque<TopicMetadata> topicList = new ConcurrentLinkedDeque<>();
        Collection<RuntimeEntity> runtimeList = RuntimeCache.getInstance().getRuntimeList();

        CountDownLatch countDownLatch = new CountDownLatch(runtimeList.size());
        ForkJoinPool taskThreadPool = new ForkJoinPool(runtimeList.size());
        try {
            taskThreadPool.submit(() ->
                runtimeList.parallelStream().forEach(runtimeEntity -> {
                    GetTopicsRequest getTopicsRequest = new GetTopicsRequest();
                    getTopicsRequest.setRuntimeHost(runtimeEntity.getHost());
                    getTopicsRequest.setRuntimePort(runtimeEntity.getPort());
                    topicRemotingService.getAllTopics(getTopicsRequest).getGetTopicsResponseFuture()
                        .whenComplete((result, ex) -> {
                            if (Objects.isNull(result)) {
                                log.error("Error occurred while getting topics", ex);
                            }
                            result.getTopicMetadataList().forEach(topic -> {
                                topic.setRuntimeId(runtimeEntity.getId());
                                topicList.add(topic);
                            });
                            countDownLatch.countDown();
                        });
                })
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error occurred while executing parallel stream", e);
        } finally {
            taskThreadPool.shutdown();
        }

        try {
            countDownLatch.await();
            return new ArrayList<>(topicList);
        } catch (InterruptedException e) {
            log.error("sync topic data from runtime failed", e);
            return new ArrayList<>();
        }
    }
}
