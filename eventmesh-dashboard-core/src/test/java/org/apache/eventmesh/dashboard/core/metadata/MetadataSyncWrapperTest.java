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


package org.apache.eventmesh.dashboard.core.metadata;


import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManageTest;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataSyncWrapperTest {

    @Mock
    private MetadataSyncResultHandler metadataSyncResultHandler;

    @Mock
    private DataMetadataHandler<BaseClusterIdBase> dataBasesHandler;

    @Mock
    private DataMetadataHandler<BaseClusterIdBase> clusterService;


    private final BaseSyncBase baseSyncBase = SDKManageTest.createJvm();


    private final MetadataSyncWrapper metadataSyncWrapper = new MetadataSyncWrapper();


    @Before
    public void init() throws InstantiationException, IllegalAccessException {

        MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
        metadataSyncConfig.setBaseSyncBase(baseSyncBase);
        metadataSyncConfig.setMetadataType(MetadataType.TOPIC);
        metadataSyncConfig.setMetadataSyncResult(new MetadataSyncResult());
        metadataSyncConfig.setDataBasesHandler(dataBasesHandler);
        metadataSyncConfig.setClusterService(clusterService);

        metadataSyncWrapper.setMetadataSyncConfig(metadataSyncConfig);
        metadataSyncWrapper.setMetadataSyncResultHandler(metadataSyncResultHandler);

        metadataSyncWrapper.createDifference();
    }

    @Test
    public void test_readOnly() {

    }

    @Test
    public void test_firstToWhom() {

    }

    @Test
    public void test_check() {

    }

    @Test
    public void test_isSyncClusterData() {

    }

    @Test
    public void test_isSyncDatabasesData() {

    }
}
