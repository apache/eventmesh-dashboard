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

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigResponse;
import org.apache.eventmesh.dashboard.console.cache.ClusterCache;
import org.apache.eventmesh.dashboard.console.cache.RuntimeCache;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataService;
import org.apache.eventmesh.dashboard.console.service.store.StoreService;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConfigSyncFromClusterService implements SyncDataService<ConfigMetadata> {

    @Autowired
    private StoreService storeDataService;

    @Setter
    ConfigRemotingService configRemotingService;

    @Override
    public List<ConfigMetadata> getData() {
        List<CompletableFuture<GetConfigResponse>> futures = new ArrayList<>();
        futures.add(getConfigsFromRegistry());
        futures.add(getConfigsFromRuntime());
        futures.add(getConfigsFromKafka());
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<ConfigMetadata>> allConfigMetadataFuture = allFutures.thenApply(v ->
            futures.stream()
                .map(future -> {
                    try {
                        return future.get().getConfigMetadataList();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList())
        );

        return allConfigMetadataFuture.join();
    }

    private CompletableFuture<GetConfigResponse> getConfigsFromRegistry() {
        GetConfigRequest registryRequest = new GetConfigRequest();
        List<String> registryList = new ArrayList<>();
        ClusterCache.getINSTANCE().getClusters().forEach(clusterEntity -> registryList.add(clusterEntity.getRegistryAddress()));
        registryRequest.setRegistryAddressList(registryList);
        return configRemotingService.getConfigsFromRegistry(registryRequest).getGetConfigResponseFuture();
    }

    private CompletableFuture<GetConfigResponse> getConfigsFromRuntime() {
        GetConfigRequest runtimeRequest = new GetConfigRequest();
        List<String> runtimeAddressList = new ArrayList<>();
        RuntimeCache.getInstance().getRuntimeList().forEach(runtimeEntity -> {
            runtimeAddressList.add(runtimeEntity.getHost() + ":" + runtimeEntity.getPort());
        });
        runtimeRequest.setRuntimeAddressList(runtimeAddressList);
        return configRemotingService.getConfigsFromRuntime(runtimeRequest).getGetConfigResponseFuture();
    }

    private CompletableFuture<GetConfigResponse> getConfigsFromKafka() {
        GetConfigRequest brokerRequest = new GetConfigRequest();
        return null;
    }
}
