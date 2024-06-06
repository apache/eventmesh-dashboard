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

package org.apache.eventmesh.dashboard.console.function.metadata.syncservice;

import org.apache.eventmesh.dashboard.console.spring.support.FunctionManagerLoader;
import org.apache.eventmesh.dashboard.core.metadata.cluster.AclSyncFromClusterService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.ConfigSyncFromClusterService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.GroupSyncFromClusterService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.InstanceUserFromClusterService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.RuntimeSyncFromClusterService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.TopicSyncFromClusterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * SyncDataServiceWrapper is a wrapper class for all sync services. It is used to inject all sync services.
 *
 * @see FunctionManagerLoader
 */
@Getter
@Component
public class SyncDataServiceWrapper {

    @Autowired
    private AclSyncFromClusterService aclSyncFromClusterService;

    @Autowired
    private ConfigSyncFromClusterService configSyncFromClusterService;

    @Autowired
    private GroupSyncFromClusterService groupSyncFromClusterService;

    @Autowired
    private RuntimeSyncFromClusterService runtimeSyncFromClusterService;

    @Autowired
    private InstanceUserFromClusterService instanceUserFromClusterService;

    @Autowired
    private TopicSyncFromClusterService topicSyncFromClusterService;
}
