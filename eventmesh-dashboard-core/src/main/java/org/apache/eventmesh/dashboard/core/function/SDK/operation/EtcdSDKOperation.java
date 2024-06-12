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

import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateEtcdConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.common.exception.EtcdException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EtcdSDKOperation extends AbstractSDKOperation<KV> {

    private static String[] getSplitEndpoints(CreateEtcdConfig etcdConfig) {
        return etcdConfig.getEtcdServerAddress().split(";");
    }

    @Override
    public SimpleEntry<String, KV> createClient(CreateSDKConfig clientConfig) {
        final CreateEtcdConfig etcdConfig = (CreateEtcdConfig) clientConfig;
        KV kvClient = null;
        try {
            final Client client = Client.builder()
                .endpoints(getSplitEndpoints(etcdConfig))
                .connectTimeout(Duration.ofSeconds(etcdConfig.getConnectTime()))
                .build();
            kvClient = client.getKVClient();
        } catch (EtcdException e) {
            log.error("create etcd client failed", e);
        }
        return new SimpleEntry<>(clientConfig.getUniqueKey(), kvClient);
    }

    @Override
    public void close(Object client) {
        castClient(client).close();
    }
}
