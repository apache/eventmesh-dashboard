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


package org.apache.eventmesh.dashboard.core.remoting;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManageTest;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.junit.Test;

public class Remoting2ManageTest {

    Remoting2Manage remoting2Manage = Remoting2Manage.getInstance();

    @Test
    public void test() {
        System.out.println(remoting2Manage);
    }


    @Test
    public void test_createDataMetadataHandler() {
        BaseSyncBase baseSyncBase = SDKManageTest.createJvm();
        DataMetadataHandler<BaseClusterIdBase> dataMetadataHandler =
            remoting2Manage.createDataMetadataHandler(AclRemotingService.class, baseSyncBase);
        dataMetadataHandler.getData();
    }


}
