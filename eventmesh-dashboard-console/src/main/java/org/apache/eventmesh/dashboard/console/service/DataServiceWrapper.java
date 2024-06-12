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

package org.apache.eventmesh.dashboard.console.service;


import org.apache.eventmesh.dashboard.console.service.cluster.ClientDataService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ConnectorDataService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.console.service.message.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;
import org.apache.eventmesh.dashboard.console.service.store.StoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * DataServiceWrapper is a wrapper class for all data services. It is used to inject all data services in spring environment.
 */
@Component
@Getter
public class DataServiceWrapper {

    @Autowired
    private ClientDataService clientDataService;

    @Autowired
    private ClusterService clusterDataService;

    @Autowired
    private ConfigService configDataService;

    @Autowired
    private ConnectionDataService connectionDataService;

    @Autowired
    private ConnectorDataService connectorDataService;

    @Autowired
    private GroupService groupDataService;

    @Autowired
    private GroupMemberService groupMemberDataService;

    @Autowired
    private HealthDataService healthDataService;

    @Autowired
    private RuntimeService runtimeDataService;

    @Autowired
    private StoreService storeDataService;

    @Autowired
    private TopicService topicDataService;
}
