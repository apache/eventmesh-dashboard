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

package org.apache.eventmesh.dashboard.console.function.SDK.operation;

import org.apache.eventmesh.dashboard.console.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.console.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.console.function.SDK.wrapper.NacosSDKWrapper;

import java.util.AbstractMap.SimpleEntry;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;

public class NacosSDKOperation extends AbstractSDKOperation<NacosSDKWrapper> {

    private final NacosConfigSDKOperation nacosConfigClientCreateOperation = new NacosConfigSDKOperation();
    private final NacosNamingSDKOperation nacosNamingClientCreateOperation = new NacosNamingSDKOperation();

    @Override
    public SimpleEntry<String, NacosSDKWrapper> createClient(CreateSDKConfig createClientConfig) {
        SimpleEntry<String, ConfigService> configSimpleEntry = nacosConfigClientCreateOperation.createClient(createClientConfig);
        SimpleEntry namingSimpleEntry = nacosNamingClientCreateOperation.createClient(createClientConfig);
        if (configSimpleEntry.getKey() != namingSimpleEntry.getKey()) {
            throw new RuntimeException("Nacos config and naming server address not match");
        }
        NacosSDKWrapper nacosClient = new NacosSDKWrapper(
            (ConfigService) configSimpleEntry.getValue(), (NamingService) namingSimpleEntry.getValue()
        );
        return new SimpleEntry<>(configSimpleEntry.getKey(), nacosClient);
    }

    @Override
    public void close(Object client) {
        try {
            castClient(client).shutdown();
        } catch (Exception e) {
            throw new RuntimeException("Nacos client close failed", e);
        }
    }
}
