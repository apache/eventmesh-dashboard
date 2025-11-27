/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.core.remoting.runtime;

import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;



public class RuntimeTopicRemotingService extends AbstractRuntimeRemotingService implements TopicRemotingService {

    @Override
    public CreateTopicResult createTopic(CreateTopic2Request createTopicRequest) {

        CreateTopicResult createTopicResult = new CreateTopicResult();



        return null;
    }

    @Override
    public DeleteTopicResult deleteTopic(DeleteTopicRequest deleteTopicRequest) {
        DeleteTopicResult deleteTopicResult = new DeleteTopicResult();
        //this.defaultMQAdminExt.deleteTopic(deleteTopicRequest.getTopicMetadata().getTopicName(), null);
        return null;
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopics2Request getTopicsRequest) throws Exception {

        GetTopicsResult result = new GetTopicsResult();

        return result;
    }
}
