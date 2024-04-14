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

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeRequest;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataService;
import org.apache.eventmesh.dashboard.console.service.registry.RegistryDataService;
import org.apache.eventmesh.dashboard.core.meta.runtime.NacosRuntimeCore;
import org.apache.eventmesh.dashboard.service.remoting.MetaRemotingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RuntimeSyncFromClusterService implements SyncDataService<RuntimeMetadata> {

    private final MetaRemotingService metaRemotingService = new NacosRuntimeCore();

    @Autowired
    private RegistryDataService registryDataService;

    @Override
    public List<RuntimeMetadata> getData() {
        List<GetRuntimeRequest> requestList = new ArrayList<>();
        ConcurrentLinkedDeque<RuntimeMetadata> runtimeMetadata = new ConcurrentLinkedDeque<>();
        List<MetaEntity> metaEntityList = registryDataService.selectAll();
        registryDataService.selectAll().forEach(
            metaEntity -> {
                GetRuntimeRequest request = new GetRuntimeRequest();
                request.setRegistryAddress(metaEntity.getHost() + ":" + metaEntity.getPort());
                requestList.add(request);
            }
        );
        if (requestList.isEmpty()) {
            return new ArrayList<>();
        }
        CountDownLatch countDownLatch = new CountDownLatch(requestList.size());
        ForkJoinPool taskThreadPool = new ForkJoinPool(requestList.size());

        try {
            taskThreadPool.submit(() ->
                requestList.parallelStream().forEach(request -> {
                    metaRemotingService.getRuntime(request).getFuture()
                        .whenComplete((result, ex) -> {
                            if (Objects.isNull(result)) {
                                log.error("Error occurred while getting topics", ex);
                            }
                            runtimeMetadata.addAll(result.getRuntimeMetadataList());
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
            return new ArrayList<>(runtimeMetadata);
        } catch (InterruptedException e) {
            log.error("sync topic data from runtime failed", e);
            return new ArrayList<>();
        }
    }
}
