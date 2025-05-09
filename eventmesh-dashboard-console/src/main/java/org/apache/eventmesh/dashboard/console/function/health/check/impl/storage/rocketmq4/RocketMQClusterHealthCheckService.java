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

import org.apache.eventmesh.dashboard.common.constant.health.HealthConstant;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractTopicHealthCheckService;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetTopicConfigRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.statictopic.TopicConfigAndQueueMapping;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RocketMQClusterHealthCheckService extends AbstractTopicHealthCheckService<DefaultRemotingClient> {


    private volatile boolean checkTopic = false;


    public void doCheck(HealthCheckCallback callback) throws Exception {
        if (!RocketMQClusterHealthCheckService.this.checkTopic) {
            this.getTopicConfig();
            return;
        }
        try {
            if (this.getBaseSyncBase().getReplicationType().isSlave()) {
                this.sendMessage(callback);
            } else {
                this.consume(callback);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(HealthCheckCallback callback) throws RemotingException, InterruptedException, MQClientException {
        if (this.getBaseSyncBase().getReplicationType().isSlave()) {
            return;
        }
        SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
        requestHeader.setProducerGroup(HealthConstant.ROCKETMQ_CHECK_PRODUCER_GROUP);
        requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        requestHeader.setDefaultTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        requestHeader.setDefaultTopicQueueNums(1);
        requestHeader.setQueueId(1);
        //requestHeader.setSysFlag(1);
        requestHeader.setBornTimestamp(System.currentTimeMillis());
        //requestHeader.setFlag(msg.getFlag());
        requestHeader.setReconsumeTimes(0);
        requestHeader.setUnitMode(false);
        requestHeader.setBatch(false);
        requestHeader.setBrokerName("127.0.0.1:9876");
        Message message = new Message();
        message.setTags(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        message.setBody(this.messageContext());
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, requestHeader);
        request.setBody(message.getBody());
        this.getClient().invokeAsync(
            request,
            1000,
            new InvokeCallback() {

                @Override
                public void operationComplete(ResponseFuture responseFuture) {

                }

                public void operationSucceed(RemotingCommand response) {
                    if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
                        try {
                            consume(callback);
                        } catch (Exception e) {
                            callback.onFail(new RuntimeException(e));
                        }

                    } else {
                        callback.onFail(new RuntimeException(""));
                    }
                }

                public void operationFail(Throwable throwable) {
                    callback.onFail(new RuntimeException(throwable));
                }
            });

    }

    private void consume(HealthCheckCallback callback) throws Exception {
        PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
        requestHeader.setConsumerGroup(HealthConstant.GROUP);
        requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        this.getClient().invokeAsync(null, 1000, new InvokeCallback() {
            @Override
            public void operationComplete(ResponseFuture responseFuture) {

            }

            @Override
            public void operationSucceed(final RemotingCommand response) {
                AtomicBoolean isSuccess = new AtomicBoolean(false);
                if (isSuccess.get()) {
                    return;
                }
                try {
                    consume(callback);
                } catch (Exception ex) {
                    callback.onFail(new RuntimeException(ex));
                }
            }

            @Override
            public void operationFail(final Throwable throwable) {
                if (!isEndCheck()) {
                    try {
                        consume(callback);
                    } catch (Exception ex) {
                        callback.onFail(new RuntimeException(ex));
                    }
                } else {
                    callback.onFail(new RuntimeException(throwable));
                }
            }
        });
    }

    @Override
    public void init() throws Exception {
        this.getTopicConfig();
    }

    private void getTopicConfig() throws InterruptedException,
        RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        GetTopicConfigRequestHeader header = new GetTopicConfigRequestHeader();
        header.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        header.setLo(true);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_CONFIG, header);
        RemotingCommand response = this.getClient().invokeSync(request, 3000);
        assert response != null;
        if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
            TopicConfigAndQueueMapping topicConfigAndQueueMapping =
                RemotingSerializable.decode(response.getBody(), TopicConfigAndQueueMapping.class);
            ConcurrentMap<Integer, Integer> queue = topicConfigAndQueueMapping.getMappingDetail().getCurrIdMap();
            if (queue.size() == 1) {
                queue.forEach((k, value) -> {
                    this.setOffset(value);
                    this.setQueue(k);
                });
                return;
            }
            RocketMQClusterHealthCheckService.this.checkTopic = true;
            // 删除 topic
        }
        if (this.getBaseSyncBase().getReplicationType().isSlave()) {
            return;
        }
        this.createTopic();
    }

    private void createTopic() throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
        requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
        requestHeader.setTopicFilterType(TopicFilterType.SINGLE_TAG.name());
        requestHeader.setReadQueueNums(1);
        requestHeader.setWriteQueueNums(1);
        requestHeader.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
        RemotingCommand response =
            this.getClient().invokeSync(request, 1000);
        if (!Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
            RocketMQClusterHealthCheckService.this.checkTopic = true;
        }
    }


}
