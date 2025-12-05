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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMapper;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.AbstractRemotingService;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.util.Objects;
import java.util.function.Function;

import com.alibaba.fastjson.JSON;

import lombok.Builder;
import lombok.Data;

/**
 * rocketmq 其他不同的是。 以nameservier 为主。那么可以多集群。一个eventmesh 可以操作多个集群
 */
@RemotingServiceMapper(clusterType = {
    ClusterType.STORAGE_ROCKETMQ,
    ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE,
    ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT})
public abstract class AbstractRocketMQRemotingService extends AbstractRemotingService<DefaultRemotingClient> {


    @SuppressWarnings("unchecked")
    protected <T> T buildResult(RemotingCommand response, GlobalResult<?> result) {
        if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
            result.setCode(200);
        } else {
            result.setCode(10000 + response.getCode());
            result.setMessage(response.getRemark());
        }
        return (T) result;
    }


    @SuppressWarnings("unchecked")
    protected <T> T invokeSync(int code, CommandCustomHeader customHeader, GlobalResult<?> result)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(code, customHeader));
        return (T) this.buildResult(response, result);
    }

    protected RemotingCommand invokeSync(RemotingCommand request)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        return this.getClient().invokeSync(request, 3000);
    }

    @SuppressWarnings("unchecked")
    protected <T> T invokeSync(RequestHandler requestHandler)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, InstantiationException,
        IllegalAccessException {
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(requestHandler.getCode(), requestHandler.getCustomHeader()));
        GlobalResult<Object> result = (GlobalResult<Object>) requestHandler.getResultClass().newInstance();
        if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
            result.setCode(200);
            if (Objects.nonNull(requestHandler.getResultDataClass())) {
                Object object = JSON.parseObject(response.getBody(), requestHandler.getResultDataClass());
                RocketMQFunction<Object> function = (RocketMQFunction<Object>) requestHandler.getHandlerFunction();
                Object resultData = function.apply(object);
                result.setData(resultData);
            }
        } else {
            result.setCode(10000 + response.getCode());
            result.setMessage(response.getRemark());
        }
        return (T) result;
    }

    /**
     *
     */
    public interface RocketMQFunction<T> extends Function<T, Object> {

    }

    @Data
    @Builder
    public static class RequestHandler {


        private int code;

        private CommandCustomHeader customHeader;

        private Class<?> resultClass;

        private Class<?> resultDataClass;

        private RocketMQFunction<?> handlerFunction;


    }


}
