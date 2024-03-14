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

import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;

import org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant;
import org.apache.eventmesh.dashboard.console.constant.health.HealthConstant;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@HealthCheckType(type = HealthCheckTypeConstant.HEALTH_CHECK_TYPE_STORAGE, subType = HealthCheckTypeConstant.HEALTH_CHECK_SUBTYPE_ROCKETMQ_TOPIC)
public class Rocketmq4TopicCheck extends AbstractHealthCheckService {

    private RemotingClient remotingClient;

    private DefaultMQPushConsumer consumer;

    private DefaultMQProducer producer;

    private Long startTime;

    private BlockingQueue<Message> consumedMessages = new LinkedBlockingQueue<>();

    public Rocketmq4TopicCheck(HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    @Override
    public void doCheck(HealthCheckCallback callback) {
        startTime = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        log.debug("RocketmqTopicCheck start, uuid:{}", uuid);
        try {
            Message msg = new Message(HealthConstant.ROCKETMQ_CHECK_TOPIC, "eventmesh-dashboard-rocketmq-topic-check", uuid
                .getBytes(
                    RemotingHelper.DEFAULT_CHARSET));
            synchronized (this) {
                producer.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        if (!sendResult.getSendStatus().equals(SEND_OK)) {
                            log.error("send message failed, sendResult:{}", sendResult);
                            callback.onFail(new Exception("send message failed for reason:" + sendResult.getSendStatus().toString()));
                            return;
                        }
                        consume(callback, uuid);
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("send message failed", e);
                        callback.onFail((Exception) e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("RocketmqTopicCheck failed when producing message.", e);
            callback.onFail(e);
        }

    }

    private synchronized void consume(HealthCheckCallback callback, String uuid) {
        try {
            while (System.currentTimeMillis() - startTime < getConfig().getRequestTimeoutMillis()) {
                Message message = consumedMessages.poll(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
                if (message != null) {
                    log.debug("RocketmqTopicCheck consume message:{}", new String(message.getBody()));
                    if (Arrays.equals(message.getBody(), uuid.getBytes())) {
                        callback.onSuccess();
                        return;
                    }
                }
            }
            callback.onFail(new TimeoutException("consume message timeout"));
        } catch (Exception e) {
            log.error("RocketmqTopicCheck failed when consuming message.", e);
            callback.onFail(e);
        }
    }

    @Override
    public void init() {
        NettyClientConfig config = new NettyClientConfig();
        config.setUseTLS(false);
        remotingClient = new NettyRemotingClient(config);
        remotingClient.start();

        //TODO there are many functions that can be reused, they should be collected in a util module
        //this function that create topics can be reused
        try {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            requestHeader.setTopicFilterType(TopicFilterType.SINGLE_TAG.name());
            requestHeader.setReadQueueNums(8);
            requestHeader.setWriteQueueNums(8);
            requestHeader.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            Object result = remotingClient.invokeSync(getConfig().getRocketmqConfig().getBrokerUrl(), request, getConfig().getRequestTimeoutMillis());
            log.info(result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopicCheck init failed when examining topic stats.", e);
            return;
        }

        try {
            producer = new DefaultMQProducer(HealthConstant.ROCKETMQ_CHECK_PRODUCER_GROUP);
            producer.setNamesrvAddr(getConfig().getRocketmqConfig().getNameServerUrl());
            producer.setCompressMsgBodyOverHowmuch(16);
            producer.start();

            consumer = new DefaultMQPushConsumer(HealthConstant.ROCKETMQ_CHECK_CONSUMER_GROUP);
            consumer.setMessageModel(MessageModel.CLUSTERING);
            consumer.setNamesrvAddr(getConfig().getRocketmqConfig().getNameServerUrl());
            consumer.subscribe(HealthConstant.ROCKETMQ_CHECK_TOPIC, "*");
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    consumedMessages.addAll(list);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            consumer.start();

        } catch (Exception e) {
            log.error("RocketmqCheck initialization failed when creating Rocketmq4 clients.", e);
        }


    }

    @Override
    public void destroy() {
        producer.shutdown();
        consumer.shutdown();
    }
}

