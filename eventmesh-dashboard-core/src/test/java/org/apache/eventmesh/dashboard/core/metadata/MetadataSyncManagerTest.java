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

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManageTest;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class MetadataSyncManagerTest {

    private Map<MetadataType, SyncMetadataCreateFactory> syncMetadataCreateFactoryMap;


    private Map<String, List<MetadataSyncWrapper>> metadataSyncConfigMap;

    private MetadataSyncManage metadataSyncManage = new MetadataSyncManage();

    private Remoting2Manage remotingManage = Remoting2Manage.getInstance();


    @Mock
    private MetadataSyncResultHandler defaultMetadataSyncResultHandler;


    private BaseSyncBase baseSyncBase = SDKManageTest.createJvm();

    @Before
    public void init() throws Exception {
        this.syncMetadataCreateFactoryMap =
            (Map<MetadataType, SyncMetadataCreateFactory>) FieldUtils.readDeclaredField(this.metadataSyncManage, "syncMetadataCreateFactoryMap",
                true);

        this.metadataSyncConfigMap =
            (Map<String, List<MetadataSyncWrapper>>) FieldUtils.readDeclaredField(this.metadataSyncManage, "metadataSyncConfigMap",
                true);
        metadataSyncManage.setMetadataSyncResultHandler(this.defaultMetadataSyncResultHandler);

        List<DataMetadataHandler<Object>> dataMetadataHandlerList = new ArrayList<>();
        DataMetadataHandler dataMetadataHandler = remotingManage.createDataMetadataHandler(AclRemotingService.class, baseSyncBase);

        dataMetadataHandlerList.add(dataMetadataHandler);
        metadataSyncManage.setDataMetadataHandlerList(dataMetadataHandlerList);

        Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap = new HashMap<>();

        DatabaseAndMetadataMapper databaseAndMetadataMapper =
            DatabaseAndMetadataMapper.builder().metaType(MetadataType.TOPIC).databaseHandlerClass(dataMetadataHandler.getClass())
                .metadataHandlerClass(TopicRemotingService.class).convertMetaData(new MockConvertMetaData()).build();
        databaseAndMetadataMapperMap.put(databaseAndMetadataMapper.getDatabaseHandlerClass(), databaseAndMetadataMapper);

        metadataSyncManage.init(100, 50000, databaseAndMetadataMapperMap);
    }


    @Test
    public void test() throws InterruptedException {
        baseSyncBase.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        baseSyncBase.setFirstToWhom(baseSyncBase.getFirstToWhom());
        metadataSyncManage.register(baseSyncBase);
        Thread.sleep(1000000L);
        System.out.println("test");
    }




    static class MockConvertMetaData implements ConvertMetaData<Object, Object> {

        @Override
        public Object toEntity(Object meta) {
            return meta;
        }

        @Override
        public Object toMetaData(Object entity) {
            return entity;
        }
    }

}
