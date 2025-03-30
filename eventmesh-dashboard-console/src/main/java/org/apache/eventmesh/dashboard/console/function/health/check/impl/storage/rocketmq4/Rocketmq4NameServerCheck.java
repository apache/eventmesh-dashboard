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

import org.apache.eventmesh.dashboard.common.constant.health.HealthCheckTypeConstant;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@HealthCheckType(type = HealthCheckTypeConstant.HEALTH_CHECK_TYPE_STORAGE, subType = HealthCheckTypeConstant.HEALTH_CHECK_SUBTYPE_ROCKETMQ)
public class Rocketmq4NameServerCheck extends AbstractHealthCheckService<DefaultRemotingClient> {


    @Override
    public void doCheck(HealthCheckCallback callback) throws Exception {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_NAMESRV_CONFIG, null);
        getClient().invokeAsync(request, 3000,
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
    }

}
