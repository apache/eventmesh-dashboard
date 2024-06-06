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

package org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4;

import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRocketmqConfig;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Rocketmq4BrokerCheck extends AbstractHealthCheckService {

    private CreateRocketmqConfig config;


    public Rocketmq4BrokerCheck(HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    @Override
    public void doCheck(HealthCheckCallback callback) {
        try {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_RUNTIME_INFO, null);
            RemotingClient client = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, config.getUniqueKey());
            client.invokeAsync(getConfig().getRocketmqConfig().getBrokerUrl(), request, getConfig().getRequestTimeoutMillis(),
                new InvokeCallback() {
                    @Override
                    public void operationComplete(ResponseFuture responseFuture) {
                        if (responseFuture.isSendRequestOK()) {
                            callback.onSuccess();
                        } else {
                            callback.onFail(new RuntimeException("RocketmqNameServerCheck failed caused by " + responseFuture.getCause()));
                        }
                    }

                });
        } catch (Exception e) {
            log.error("RocketmqCheck failed.", e);
            callback.onFail(e);
        }
    }

    @Override
    public void init() {
        setBrokerUrl();

        config = new CreateRocketmqConfig();
        config.setBrokerUrl(getConfig().getRocketmqConfig().getBrokerUrl());
        SDKManager.getInstance().createClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, config);
    }

    private void setBrokerUrl() {
        if (Objects.nonNull(getConfig().getRocketmqConfig()) && Objects.nonNull(getConfig().getRocketmqConfig().getBrokerUrl())) {
            return;
        }
        if (Objects.nonNull(getConfig().getConnectUrl()) && !getConfig().getConnectUrl().isEmpty()) {
            getConfig().getRocketmqConfig().setBrokerUrl(getConfig().getConnectUrl());
            return;
        }
        if (Objects.nonNull(getConfig().getHost()) && Objects.nonNull(getConfig().getPort())) {
            getConfig().getRocketmqConfig().setBrokerUrl(getConfig().getHost() + ":" + getConfig().getPort());
            return;
        }
        throw new RuntimeException("RocketmqNameServerCheck init failed, brokerUrl is empty");
    }

    @Override
    public void destroy() {
        SDKManager.getInstance().deleteClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, config.getUniqueKey());
    }
}
