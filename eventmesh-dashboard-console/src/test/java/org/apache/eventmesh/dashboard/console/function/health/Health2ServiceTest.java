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


package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmCapConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Health2ServiceTest {

    @Mock
    private HealthDataService dataService;
    private Health2Service health2Service = new Health2Service();

    public static BaseSyncBase createJvm() {
        SDKTypeEnum sdkTypeEnum = SDKTypeEnum.ADMIN;
        BaseSyncBase baseSyncBase = new RuntimeMetadata();
        baseSyncBase.setId(1L);
        baseSyncBase.setClusterType(ClusterType.STORAGE_JVM_BROKER);
        CreateJvmConfig config = new CreateJvmConfig();
        config.setKey(baseSyncBase.getId().toString());
        SDKManage.getInstance().createClient(sdkTypeEnum, baseSyncBase, config, baseSyncBase.getClusterType());
        return baseSyncBase;
    }

    public static BaseSyncBase createJvmCap() {
        SDKTypeEnum sdkTypeEnum = SDKTypeEnum.ADMIN;
        BaseSyncBase baseSyncBase = new RuntimeMetadata();
        baseSyncBase.setId(1L);
        baseSyncBase.setClusterType(ClusterType.STORAGE_JVM_CAP_BROKER);
        CreateJvmCapConfig config = new CreateJvmCapConfig();
        config.setKey(baseSyncBase.getId().toString());
        SDKManage.getInstance().createClient(sdkTypeEnum, baseSyncBase, config, baseSyncBase.getClusterType());
        return baseSyncBase;
    }

    @Before
    public void init() {
        health2Service.setDataService(dataService);
    }

    @Test
    public void test() {
        BaseSyncBase baseSyncBase = createJvm();
        health2Service.register(baseSyncBase);
        health2Service.executeAll();
    }

}
