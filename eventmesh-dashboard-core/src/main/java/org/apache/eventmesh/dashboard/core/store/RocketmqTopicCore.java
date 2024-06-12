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

package org.apache.eventmesh.dashboard.core.store;

import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.service.dto.RocketmqProperties;
import org.apache.eventmesh.dashboard.service.dto.TopicProperties;
import org.apache.eventmesh.dashboard.service.store.TopicCore;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RocketmqTopicCore implements TopicCore {

    private final RocketmqProperties rocketmqProperties;

    public RocketmqTopicCore(RocketmqProperties rocketmqProperties) {
        this.rocketmqProperties = rocketmqProperties;
    }

    private RemotingClient createRemotingClient(String brokerUrl) {
        CreateSDKConfig createSDKConfig = () -> brokerUrl;

        SDKManager.getInstance().createClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, createSDKConfig);
        return (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, brokerUrl);
    }

    @Override
    public Boolean createTopic(String topicName) {
        String namesrvAddr = rocketmqProperties.getNamesrvAddr();
        long requestTimeoutMillis = rocketmqProperties.getRequestTimeoutMillis();
        if (StringUtils.isEmpty(namesrvAddr)) {
            log.info("RocketmqTopicCore-createTopic failed, missing brokerUrl");
            return Boolean.FALSE;
        }

        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, namesrvAddr);
        if (remotingClient == null) {
            remotingClient = createRemotingClient(namesrvAddr);
        }
        try {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(topicName);
            requestHeader.setTopicFilterType(TopicFilterType.SINGLE_TAG.name());
            requestHeader.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);

            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            RemotingCommand response = remotingClient.invokeSync(namesrvAddr, request, requestTimeoutMillis);
            log.info("Rocketmq create topic result:" + response.toString());
            return response.getCode() == 0;
        } catch (Exception e) {
            log.error("RocketmqTopicCore-createTopic failed.", e);
        }
        return Boolean.FALSE;
    }

    @Override
    public List<TopicProperties> getTopics() {
        String namesrvAddr = rocketmqProperties.getNamesrvAddr();
        long requestTimeoutMillis = rocketmqProperties.getRequestTimeoutMillis();
        if (StringUtils.isEmpty(namesrvAddr)) {
            log.info("RocketmqTopicCore-getTopics failed, missing brokerUrl");
            return new ArrayList<>();
        }

        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, namesrvAddr);
        if (remotingClient == null) {
            remotingClient = createRemotingClient(namesrvAddr);
        }
        List<TopicConfig> topicConfigList = new ArrayList<>();
        try {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, (CommandCustomHeader) null);
            RemotingCommand response = remotingClient.invokeSync(namesrvAddr, request, requestTimeoutMillis);
            TopicConfigSerializeWrapper allTopicConfig = TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
            ConcurrentMap<String, TopicConfig> topicConfigTable = allTopicConfig.getTopicConfigTable();
            topicConfigList = new ArrayList<>(topicConfigTable.values());
        } catch (Exception e) {
            log.error("RocketmqTopicCore-createTopic failed.", e);
        }

        return topicConfig2TopicProperties(topicConfigList);
    }

    @Override
    public Boolean deleteTopic(String topicName) {
        String namesrvAddr = rocketmqProperties.getNamesrvAddr();
        long requestTimeoutMillis = rocketmqProperties.getRequestTimeoutMillis();
        if (StringUtils.isEmpty(namesrvAddr)) {
            log.info("RocketmqTopicCore-deleteTopic failed, missing brokerUrl");
            return Boolean.FALSE;
        }

        RemotingClient remotingClient = (RemotingClient) SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_ROCKETMQ_REMOTING, namesrvAddr);
        if (remotingClient == null) {
            remotingClient = createRemotingClient(namesrvAddr);
        }
        try {
            DeleteTopicRequestHeader deleteTopicRequestHeader = new DeleteTopicRequestHeader();
            deleteTopicRequestHeader.setTopic(topicName);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, null);
            RemotingCommand response = remotingClient.invokeSync(namesrvAddr, request, requestTimeoutMillis);

            log.info("Rocketmq delete topic result:" + response.toString());
            return response.getCode() == 0;
        } catch (Exception e) {
            log.error("RocketmqTopicCore-createTopic failed.", e);
        }
        return Boolean.FALSE;
    }

    public List<TopicProperties> topicConfig2TopicProperties(List<TopicConfig> topicConfigList) {
        ArrayList<TopicProperties> topicPropertiesList = new ArrayList<>();
        for (TopicConfig topicConfig : topicConfigList) {
            TopicProperties topicProperties = new TopicProperties();
            BeanUtils.copyProperties(topicConfig, topicProperties);
            topicPropertiesList.add(topicProperties);
        }
        return topicPropertiesList;
    }
}
