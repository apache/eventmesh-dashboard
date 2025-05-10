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


package org.apache.eventmesh.dashboard.core.function.SDK.operation.kubernetes;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKubernetesConfig;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;


@SDKMetadata(clusterType = ClusterType.KUBERNETES_RUNTIME, remotingType = RemotingType.KUBERNETES, sdkTypeEnum = {
    SDKTypeEnum.ADMIN, SDKTypeEnum.PING})
public class KubernetesClientOperation extends AbstractSDKOperation<KubernetesClient, CreateKubernetesConfig> {

    @Override
    public KubernetesClient createClient(CreateKubernetesConfig clientConfig) throws Exception {
        KubernetesClientBuilder kubernetesClientBuilder = new KubernetesClientBuilder();
        kubernetesClientBuilder.withConfig(this.getBaseSyncBase().getConfig());
        return kubernetesClientBuilder.build();
    }

    @Override
    public void close(KubernetesClient client) throws Exception {

    }
}
