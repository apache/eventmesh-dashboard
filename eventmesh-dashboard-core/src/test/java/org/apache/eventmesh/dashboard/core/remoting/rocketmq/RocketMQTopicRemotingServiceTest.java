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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.rocketmq.common.TopicFilterType;

import org.junit.Before;
import org.junit.Test;

public class RocketMQTopicRemotingServiceTest {


    private RocketMQTopicRemotingService rocketMQTopicRemotingService;


    @Before
    public void init() {
        RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
        runtimeMetadata.setId(5L);
        runtimeMetadata.setHost("127.0.0.1");
        runtimeMetadata.setPort(20911);
        runtimeMetadata.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);

        AbstractSimpleCreateSDKConfig config =
            ConfigManage.getInstance().getSimpleCreateSdkConfig(runtimeMetadata.getClusterType(), SDKTypeEnum.ADMIN);
        config.setKey("11");
        NetAddress netAddress = new NetAddress();
        netAddress.setAddress(runtimeMetadata.getHost());
        netAddress.setPort(runtimeMetadata.getPort());
        config.setNetAddress(netAddress);

        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtimeMetadata, config, runtimeMetadata.getClusterType());
        rocketMQTopicRemotingService = Remoting2Manage.getInstance().createRemotingService(TopicRemotingService.class, runtimeMetadata);
    }

    @Test
    public void test_createTopic() throws Exception {
        TopicMetadata topicMetadata = new TopicMetadata();
        topicMetadata.setTopicName("test_topic");
        topicMetadata.setWriteQueueNum(10);
        topicMetadata.setReadQueueNum(12);
        topicMetadata.setOrder(0);
        topicMetadata.setTopicFilterType(TopicFilterType.SINGLE_TAG.name());
        CreateTopic2Request createTopicRequest = new CreateTopic2Request();
        createTopicRequest.setMetaData(topicMetadata);
        rocketMQTopicRemotingService.createTopic(createTopicRequest);

        GetTopics2Request getTopicsRequest = new GetTopics2Request();
        getTopicsRequest.setMetaData(topicMetadata);
        GetTopicsResult getTopicsResult = rocketMQTopicRemotingService.getAllTopics(getTopicsRequest);
        getTopicsResult.getData();
    }

}
