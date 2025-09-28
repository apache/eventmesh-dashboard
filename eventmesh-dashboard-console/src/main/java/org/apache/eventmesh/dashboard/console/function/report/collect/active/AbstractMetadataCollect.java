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

package org.apache.eventmesh.dashboard.console.function.report.collect.active;

import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.active.AbstractCollect.AbstractRuntimeCollect;

import java.util.List;

public abstract class AbstractMetadataCollect<C> extends AbstractRuntimeCollect<C> {


    private List<TopicMetadata> topicMetadataList;

    private List<GroupMetadata> groupMetadataList;


    public synchronized List<TopicMetadata> getTopicMetadataList() {
        return topicMetadataList;
    }

    public synchronized void setTopicMetadataList(List<TopicMetadata> topicMetadataList) {
        this.topicMetadataList = topicMetadataList;
    }

    public synchronized List<GroupMetadata> getGroupMetadataList() {
        return groupMetadataList;
    }

    public synchronized void setGroupMetadataList(List<GroupMetadata> groupMetadataList) {
        this.groupMetadataList = groupMetadataList;
    }
}