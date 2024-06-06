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

package org.apache.eventmesh.dashboard.core.metadata.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConfigSyncFromClusterService extends AbstractMetadataHandler<ConfigMetadata, ConfigRemotingService, GetConfigRequest> {

    @Setter
    ConfigRemotingService configRemotingService;

    @Override
    public void addMetadata(ConfigMetadata meta) {

    }

    @Override
    public void deleteMetadata(ConfigMetadata meta) {

    }

    @Override
    public List<ConfigMetadata> getData() {
        // topic
        // group
        // runtime
        // broker

        return null;
    }

    @Override
    public List<ConfigMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }


    @Override
    public GlobalResult request(ConfigRemotingService configRemotingService, GetConfigRequest key) {
        return null;
    }
}
