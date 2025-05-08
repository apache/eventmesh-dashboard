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


package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateEtcdConfig;

import java.time.Duration;

import io.etcd.jetcd.Client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SDKMetadata(clusterType = {ClusterType.EVENTMESH_META_ETCD}, remotingType = RemotingType.EVENT_MESH_ETCD, sdkTypeEnum = {SDKTypeEnum.ADMIN,
    SDKTypeEnum.PING})
public class EtcdSDKOperation extends AbstractSDKOperation<Client, CreateEtcdConfig> {

    private static String[] getSplitEndpoints(CreateEtcdConfig etcdConfig) {
        return etcdConfig.getEtcdServerAddress().split(";");
    }

    @Override
    public Client createClient(CreateEtcdConfig clientConfig) throws Exception {
        Client client = Client.builder()
            .endpoints(clientConfig.getNetAddresses())
            .connectTimeout(Duration.ofSeconds(clientConfig.getConnectTime()))
            .build();
        return client;

    }

    @Override
    public void close(Client client) throws Exception {
        client.close();
    }
}
