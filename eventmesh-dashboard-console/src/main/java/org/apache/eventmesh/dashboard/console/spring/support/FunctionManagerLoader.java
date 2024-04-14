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

package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.console.config.FunctionManagerConfigs;
import org.apache.eventmesh.dashboard.console.function.health.HealthService;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.syncservice.SyncDataServiceWrapper;
import org.apache.eventmesh.dashboard.console.service.DataServiceWrapper;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
public class FunctionManagerLoader {

    private FunctionManager functionManager;

    private FunctionManagerProperties properties;

    @Autowired
    private DataServiceWrapper dataServiceContainer;
    @Autowired
    private SyncDataServiceWrapper syncDataServiceWrapper;
    @Autowired
    private MetadataHandlerWrapper metadataHandlerWrapper;

    @Autowired
    private FunctionManagerConfigs functionManagerConfigs;

    @Bean
    public HealthService getHealthService() {
        return functionManager.getHealthService();
    }


    @PostConstruct
    void initManager() {
        functionManager = new FunctionManager();
        properties = new FunctionManagerProperties();
        properties.setDataServiceContainer(
            dataServiceContainer);
        properties.setSyncDataServiceWrapper(
            syncDataServiceWrapper);
        properties.setMetadataHandlerWrapper(metadataHandlerWrapper);

        functionManager.setProperties(properties);
        functionManager.setConfigs(functionManagerConfigs);
        functionManager.initFunctionManager();
    }

}
