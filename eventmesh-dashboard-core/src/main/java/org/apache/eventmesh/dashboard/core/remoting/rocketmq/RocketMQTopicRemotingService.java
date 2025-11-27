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

import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingAction;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.attribute.AttributeParser;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;


public class RocketMQTopicRemotingService extends AbstractRocketMQRemotingService implements TopicRemotingService {

    private static final TypeReference<Map<String, String>> ATTRIBUTES_TYPE_REFERENCE = new TypeReference<Map<String, String>>() {
    };


    @Override
    @RemotingAction(support = false, substitution = RemotingType.STORAGE)
    public CreateTopicResult createTopic(CreateTopic2Request createTopicRequest) throws Exception {
        TopicMetadata topicMetadata = createTopicRequest.getMetaData();

        CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
        requestHeader.setTopic(topicMetadata.getTopicName());
        requestHeader.setDefaultTopic(topicMetadata.getTopicName());
        requestHeader.setReadQueueNums(topicMetadata.getReadQueueNum());
        requestHeader.setWriteQueueNums(topicMetadata.getWriteQueueNum());
        // 下面的参数是否定义到 数据库表里面
        requestHeader.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);

        TopicFilterType topicFilterType = Objects.isNull(topicMetadata.getTopicFilterType()) ? TopicFilterType.SINGLE_TAG
            : TopicFilterType.valueOf(topicMetadata.getTopicFilterType());
        requestHeader.setTopicFilterType(topicFilterType.name());
        requestHeader.setTopicSysFlag(0);
        requestHeader.setOrder(!Objects.isNull(topicMetadata.getOrder()) && topicMetadata.getOrder() == 1);
        if(Objects.nonNull(topicMetadata.getTopicConfig())){
            Map<String, String> configMap = JSON.parseObject(topicMetadata.getTopicConfig(), ATTRIBUTES_TYPE_REFERENCE);
            requestHeader.setAttributes(AttributeParser.parseToString(configMap));
        }
        return this.invokeSync(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader, new CreateTopicResult());
    }

    @Override
    public DeleteTopicResult deleteTopic(DeleteTopicRequest deleteTopicRequest) throws Exception {
        TopicMetadata topicMetadata = deleteTopicRequest.getMetaData();
        DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
        requestHeader.setTopic(topicMetadata.getTopicName());
        return this.invokeSync(RequestCode.DELETE_TOPIC_IN_BROKER, requestHeader, new CreateTopicResult());
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopics2Request getTopicsRequest) throws Exception {
        RocketMQFunction<TopicConfigSerializeWrapper> handlerFunction = o -> {
            List<TopicMetadata> topicMetadataList = new ArrayList<>();
            o.getTopicConfigTable().forEach((key, value) -> {
                TopicMetadata topicMetadata = new TopicMetadata();
                topicMetadata.setTopicName(value.getTopicName());
                topicMetadata.setReadQueueNum(value.getReadQueueNums());
                topicMetadata.setWriteQueueNum(value.getWriteQueueNums());
                topicMetadata.setTopicFilterType(value.getTopicFilterType().name());
                topicMetadata.setOrder(value.isOrder()?1:0);
                topicMetadata.setTopicConfig(JSON.toJSONString(value.getAttributes()));
                topicMetadataList.add(topicMetadata);
            });
            return topicMetadataList;
        };
        RequestHandler requestHandler = RequestHandler.builder().code(RequestCode.GET_ALL_TOPIC_CONFIG).resultClass(GetTopicsResult.class)
            .resultDataClass(TopicConfigSerializeWrapper.class).handlerFunction(handlerFunction).build();
        return this.invokeSync(requestHandler);
    }
}
