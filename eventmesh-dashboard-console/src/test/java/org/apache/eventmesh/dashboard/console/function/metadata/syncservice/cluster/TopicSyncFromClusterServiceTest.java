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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.console.cache.RuntimeCache;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicSyncFromClusterServiceTest {

    private static TopicSyncFromClusterService topicSyncFromManagerService = new TopicSyncFromClusterService();

    private static TopicRemotingService topicRemotingService = Mockito.mock(TopicRemotingService.class);

    @BeforeAll
    public static void initMock() {
        Mockito.when(topicRemotingService.getAllTopics(Mockito.any())).thenAnswer(invocation -> {
            GetTopicsResponse getTopicsResponse = new GetTopicsResponse();
            getTopicsResponse.setTopicMetadataList(Arrays.asList(new TopicMetadata()));
            GetTopicsResult getTopicsResult = new GetTopicsResult();
            getTopicsResult.setGetTopicsResponseFuture(CompletableFuture.completedFuture(getTopicsResponse)
                .thenApply(response -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return response;
                }));
            return getTopicsResult;
        });
        topicSyncFromManagerService.setTopicRemotingService(topicRemotingService);
        RuntimeEntity runtimeEntity = new RuntimeEntity(1L, "runtime1", 2L, 1019, 1099, 1L, "null", 1, null, null, "null");
        RuntimeCache.getInstance().addRuntime(runtimeEntity);
        runtimeEntity.setHost("runtime2");
        RuntimeCache.getInstance().addRuntime(runtimeEntity);
        runtimeEntity.setHost("runtime3");
        RuntimeCache.getInstance().addRuntime(runtimeEntity);
        runtimeEntity.setHost("runtime4");
        RuntimeCache.getInstance().addRuntime(runtimeEntity);
        runtimeEntity.setHost("runtime5");
        RuntimeCache.getInstance().addRuntime(runtimeEntity);
    }

    @Test
    public void testGetData() {
        long start = System.nanoTime();
        topicSyncFromManagerService.getData();
        long end = System.nanoTime();
        assertTrue((end - start) < 2000_000_000L);
    }
}