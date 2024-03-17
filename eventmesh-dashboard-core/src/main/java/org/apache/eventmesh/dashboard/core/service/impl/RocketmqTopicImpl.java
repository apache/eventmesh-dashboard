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

package org.apache.eventmesh.dashboard.core.service.impl;

import org.apache.eventmesh.dashboard.common.dto.TopicProperties;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.service.mq.RocketmqTopicService;

import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.common.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RocketmqTopicImpl implements RocketmqTopicService {

    private static final String BROKER_URL = "175.27.155.139:10911";

    @Override
    public Boolean createTopic(String topicName, String topicFilterTypeName, int perm, String nameServerAddr,
        int readQueueNums, int writeQueueNums, long requestTimeoutMillis) {
        nameServerAddr = BROKER_URL;
        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, nameServerAddr);
        try {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(topicName);
            requestHeader.setTopicFilterType(topicFilterTypeName);
            requestHeader.setReadQueueNums(readQueueNums);
            requestHeader.setWriteQueueNums(writeQueueNums);
            requestHeader.setPerm(perm);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            RemotingCommand response = remotingClient.invokeSync(nameServerAddr, request, requestTimeoutMillis);
            log.info("rocketmq create topic result:" + response.toString());
            return response.getCode() == 0;
        } catch (Exception e) {
            log.error("Rocketmq create topic failed when examining topic stats.", e);
        }
        return Boolean.FALSE;
    }

    @Override
    public List<TopicProperties> getTopics(String nameServerAddr, long requestTimeoutMillis) {
        nameServerAddr = BROKER_URL;
        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, nameServerAddr);
        List<TopicConfig> topicConfigList = new ArrayList<>();
        try {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, (CommandCustomHeader) null);
            RemotingCommand response = remotingClient.invokeSync(nameServerAddr, request, requestTimeoutMillis);
            TopicConfigSerializeWrapper allTopicConfig = TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
            ConcurrentMap<String, TopicConfig> topicConfigTable = allTopicConfig.getTopicConfigTable();
            topicConfigList = new ArrayList<>(topicConfigTable.values());
        } catch (Exception e) {
            log.error("Rocketmq get topics failed when examining topic stats.", e);
        }

        return topicConfig2TopicProperties(topicConfigList);
    }

    @Override
    public Boolean deleteTopic(String topicName, String nameServerAddr, long requestTimeoutMillis) {
        nameServerAddr = BROKER_URL;
        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, nameServerAddr);
        try {
            DeleteTopicRequestHeader deleteTopicRequestHeader = new DeleteTopicRequestHeader();
            deleteTopicRequestHeader.setTopic(topicName);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_NAMESRV, null);
            RemotingCommand response = remotingClient.invokeSync(nameServerAddr, request, requestTimeoutMillis);
            log.info("rocketmq delete topic result:" + response.toString());
            return response.getCode() == 0;
        } catch (Exception e) {
            log.error("Rocketmq delete topic failed when examining topic stats.", e);
        }
        return Boolean.FALSE;
    }

    private List<TopicProperties> topicConfig2TopicProperties(List<TopicConfig> topicConfigList) {
        ArrayList<TopicProperties> topicPropertiesList = new ArrayList<>();
        for (TopicConfig topicConfig : topicConfigList) {
            TopicProperties topicProperties = new TopicProperties();
            BeanUtils.copyProperties(topicConfig, topicProperties);
            topicPropertiesList.add(topicProperties);
        }
        return topicPropertiesList;
    }

}
