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

package org.apache.eventmesh.dashboard.console.function.report.collect.padding;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.cap.CapQueueIndex;
import org.apache.eventmesh.dashboard.console.function.report.model.base.cap.CapSubscribeId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.cap.CapTopicId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.not.QueueIndex;
import org.apache.eventmesh.dashboard.console.function.report.model.base.not.SubscribeId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.not.TopicId;


public class TopicIdPadding extends AbstractPadding<ClusterId, TopicMetadata> {


    {
        this.setIdFunction((data) -> {
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(data.getClusterType());
            if (clusterFramework.isCAP()) {
                return data.getClusterId();
            }
            return data.getRuntimeId();
        });
        this.setKeyFunction(TopicMetadata::getName);
    }

    @Override
    public Class<?>[] clazz() {
        return new Class[] {TopicId.class, SubscribeId.class, CapTopicId.class, CapSubscribeId.class, CapQueueIndex.class};
    }

    @Override
    public void padding(ClusterId data) {
        if (data instanceof TopicId topicId) {
            topicId.setTopicId(this.getData(topicId.getRuntimeId(), topicId.getTopicName()).getId());
        } else if (data instanceof SubscribeId subscribeId) {
            TopicMetadata topicMetadata = this.getData(subscribeId.getRuntimeId(), subscribeId.getTopicName());
            subscribeId.setTopicId(topicMetadata.getId());
        } else if (data instanceof CapTopicId capTopicId) {
            TopicMetadata topicMetadata = this.getData(capTopicId.getClustersId(), capTopicId.getTopicName());
            capTopicId.setTopicId(topicMetadata.getId());
        } else if (data instanceof CapSubscribeId capSubscribeId) {
            TopicMetadata topicMetadata = this.getData(capSubscribeId.getClustersId(), capSubscribeId.getTopicName());
            capSubscribeId.setTopicId(topicMetadata.getId());
        }
        if (data instanceof CapQueueIndex capQueueIndex) {
            TopicMetadata topicMetadata = this.getData(capQueueIndex.getClustersId(), capQueueIndex.getTopicName());
            capQueueIndex.setTopicId(topicMetadata.getId());
        } else if (data instanceof QueueIndex queueIndex) {
            TopicMetadata topicMetadata = this.getData(queueIndex.getClustersId(), queueIndex.getTopicName());
            queueIndex.setTopicId(topicMetadata.getId());
        }
    }
}
