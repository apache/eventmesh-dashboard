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

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.remoting.RemotingServiceHandler;
import org.apache.eventmesh.dashboard.core.remoting.jvm.AbstractJvmService;

import java.util.HashMap;
import java.util.Map;

import lombok.Setter;


public class MockJvmRemotingServiceHandler implements RemotingServiceHandler {

    @Setter
    private Map<MetadataType, SyncMetadataCreateFactory> syncMetadataCreateFactoryMap = new HashMap<>();


    @Override
    public void serviceInit(Object object) {
        if (object instanceof AbstractJvmService<?> service) {
            service.setSyncMetadataCreateFactoryMap(syncMetadataCreateFactoryMap);
        }
    }
}
