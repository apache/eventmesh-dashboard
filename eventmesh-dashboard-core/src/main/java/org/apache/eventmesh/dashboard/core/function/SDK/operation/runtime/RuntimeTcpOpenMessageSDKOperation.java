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

package org.apache.eventmesh.dashboard.core.function.SDK.operation.runtime;

import static org.apache.eventmesh.dashboard.core.function.SDK.util.RuntimeSDKOperationUtils.buildEventMeshTCPClientConfig;
import static org.apache.eventmesh.dashboard.core.function.SDK.util.RuntimeSDKOperationUtils.buildUserAgent;

import org.apache.eventmesh.client.tcp.conf.EventMeshTCPClientConfig;
import org.apache.eventmesh.client.tcp.impl.openmessage.OpenMessageTCPClient;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRuntimeConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.util.AbstractMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeTcpOpenMessageSDKOperation extends AbstractSDKOperation<OpenMessageTCPClient> {

    @Override
    public AbstractMap.SimpleEntry<String, OpenMessageTCPClient> createClient(CreateSDKConfig clientConfig) {
        final CreateRuntimeConfig runtimeConfig = (CreateRuntimeConfig) clientConfig;
        OpenMessageTCPClient openMessageTCPClient = null;
        try {
            UserAgent userAgent = buildUserAgent(runtimeConfig.getUserAgent());
            final EventMeshTCPClientConfig eventMeshTCPClientConfig = buildEventMeshTCPClientConfig(
                runtimeConfig.getRuntimeServerAddress(), userAgent);
            openMessageTCPClient = new OpenMessageTCPClient(eventMeshTCPClientConfig);
            openMessageTCPClient.init();
        } catch (Exception e) {
            log.error("create runtime eventmesh OpenMessage client failed", e);
        }
        return new AbstractMap.SimpleEntry<>(clientConfig.getUniqueKey(), openMessageTCPClient);
    }

    @Override
    public void close(Object client) {
        castClient(client).close();
    }
}
