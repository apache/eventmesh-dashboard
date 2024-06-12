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
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.remoting.protocol.body.TopicConfigSerializeWrapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;


public class RocketMQTopicRemotingService extends AbstractRocketMQRemotingService implements TopicRemotingService {

    @Resource
    @Override
    @RemotingAction(support = false, substitution = RemotingType.STORAGE)
    public CreateTopicResult createTopic(CreateTopicRequest createTopicRequest) {
        CreateTopicResult createTopicResult = new CreateTopicResult();
        TopicConfig topicConfig = this.toDataOjbect(createTopicRequest.getTopicMetadata().getTopicConfig(), TopicConfig.class);
        return this.cluster(createTopicResult, (master, result) -> {
            this.defaultMQAdminExt.createAndUpdateTopicConfig(master, topicConfig);
            return null;
        });
    }

    @Override
    public DeleteTopicResult deleteTopic(DeleteTopicRequest deleteTopicRequest) {
        DeleteTopicResult deleteTopicResult = new DeleteTopicResult();
        return this.clusterName(deleteTopicResult, (master, result) -> {
            this.defaultMQAdminExt.deleteTopic(deleteTopicRequest.getTopicMetadata().getTopicName(), master);
            return null;
        });
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopicsRequest getTopicsRequest) {
        GetTopicsResult getTopicsResult = new GetTopicsResult();
        GetTopicsResponse getTopicsResponse = new GetTopicsResponse();
        List<TopicMetadata> list = new ArrayList<>();
        getTopicsResult.setData(getTopicsResponse);
        return this.cluster(getTopicsResult, (master, result) -> {
            TopicConfigSerializeWrapper topicConfigSerializeWrapper = this.defaultMQAdminExt.getAllTopicConfig(master, 3000);
            if (!topicConfigSerializeWrapper.getTopicConfigTable().isEmpty()) {
                topicConfigSerializeWrapper.getTopicConfigTable().forEach((k, v) -> {
                    TopicMetadata topicMetadata = new TopicMetadata();
                    //topicMetadata.setClusterId();
                });
            }
            return null;
        });
    }
}
