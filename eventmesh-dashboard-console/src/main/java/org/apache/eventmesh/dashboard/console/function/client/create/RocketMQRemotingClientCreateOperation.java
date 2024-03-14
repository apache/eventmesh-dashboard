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

package org.apache.eventmesh.dashboard.console.function.client.create;

import org.apache.eventmesh.dashboard.console.function.client.AbstractClientOperation;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateClientConfig;

import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;

import javafx.util.Pair;

public class RocketMQRemotingClientCreateOperation extends AbstractClientOperation<RemotingClient> {

    @Override
    public Pair<String, RemotingClient> createClient(CreateClientConfig clientConfig) {
        NettyClientConfig config = new NettyClientConfig();
        config.setUseTLS(false);
        RemotingClient remotingClient = new NettyRemotingClient(config);
        remotingClient.start();
        return new Pair<>(clientConfig.getUniqueKey(), remotingClient);
    }

    @Override
    public void close(Object client) {
        ((RemotingClient) client).shutdown();
    }
}
