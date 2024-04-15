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

package org.apache.eventmesh.dashboard.core.meta.runtime;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeResult;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateNacosConfig;
import org.apache.eventmesh.dashboard.service.remoting.MetaRemotingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingService;

public class NacosRuntimeCore implements MetaRemotingService {

    @Override
    public GetRuntimeResult getRuntime(GetRuntimeRequest getRuntimeRequest) {
        CreateNacosConfig createNacosConfig = new CreateNacosConfig();
        createNacosConfig.setServerAddress(getRuntimeRequest.getRegistryAddress());
        NacosNamingService nacosNamingService = (NacosNamingService) SDKManager.getInstance()
            .createClient(SDKTypeEnum.META_NACOS_NAMING, createNacosConfig).getValue();
        GetRuntimeResult getRuntimeResult = new GetRuntimeResult();

        CompletableFuture<GetRuntimeResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, RuntimeMetadata> runtimeMetadataMap = new HashMap<>();
                //If service name or group name is changed, please modify the following code
                List<String> protocols = Arrays.asList("GRPC", "HTTP", "TCP");

                for (String protocol : protocols) {
                    List<Instance> instances =
                        nacosNamingService.getAllInstances("EVENTMESH-runtime-" + protocol, protocol + "-GROUP");
                    instances.forEach(instance -> {
                        if (!runtimeMetadataMap.containsKey(instance.getIp())) {
                            RuntimeMetadata runtimeMetadata = RuntimeMetadata.builder()
                                .host(instance.getIp())
                                .port(instance.getPort())
                                .rack(instance.getClusterName())
                                .storageClusterId(0L)
                                .clusterName(Objects.isNull(instance.getClusterName()) ? instance.getClusterName() : "NORMAL")
                                .registryAddress(getRuntimeRequest.getRegistryAddress())
                                .jmxPort(0)
                                .endpointMap("")
                                .build();
                            runtimeMetadata.setRegistryAddress(getRuntimeRequest.getRegistryAddress());
                            runtimeMetadataMap.put(instance.getIp(), runtimeMetadata);
                        }
                    });
                }

                return new GetRuntimeResponse(new ArrayList<>(runtimeMetadataMap.values()));
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        });

        getRuntimeResult.setFuture(future);
        return getRuntimeResult;
    }
}
