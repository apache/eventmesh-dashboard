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


<<<<<<<< HEAD:eventmesh-dashboard-core/src/main/java/org/apache/eventmesh/dashboard/core/remoting/jvm/runtime/JvmConfigRemotingService.java
package org.apache.eventmesh.dashboard.core.remoting.jvm.runtime;
========
package org.apache.eventmesh.dashboard.console.service.connector.Impl;


import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/connector/Impl/ResourcesConfigServiceImpl.java


<<<<<<<< HEAD:eventmesh-dashboard-core/src/main/java/org/apache/eventmesh/dashboard/core/remoting/jvm/runtime/JvmConfigRemotingService.java
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.AddConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopics2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;


public class JvmConfigRemotingService extends AbstractJvmRemotingService implements ConfigRemotingService {

    @Override
    public BaseGlobalResult addConfig(AddConfigRequest addConfigRequest) {
========
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourcesConfigServiceImpl implements ResourcesConfigService {



    @Override
    public void insertResources(ResourcesConfigEntity resourcesConfigEntity) {

    }

    @Override
    public ResourcesConfigEntity queryResourcesById(ResourcesConfigEntity resourcesConfigEntity) {
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/connector/Impl/ResourcesConfigServiceImpl.java
        return null;
    }

    @Override
<<<<<<<< HEAD:eventmesh-dashboard-core/src/main/java/org/apache/eventmesh/dashboard/core/remoting/jvm/runtime/JvmConfigRemotingService.java
    public GetTopicsResult getConfig(GetConfigRequest getConfigRequest) {
        return null;
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopics2Request getTopicsRequest) {
        return null;
========
    public List<ResourcesConfigEntity> queryResourcesByOrganizationId(ResourcesConfigEntity resourcesConfigEntity) {
        return List.of();
    }

    @Override
    public void copyResources(ResourcesConfigEntity resourcesConfigEntity) {

>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/connector/Impl/ResourcesConfigServiceImpl.java
    }
}
