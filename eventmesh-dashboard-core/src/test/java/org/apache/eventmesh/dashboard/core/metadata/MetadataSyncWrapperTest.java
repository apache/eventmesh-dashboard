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
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManageTest;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.core.metadata.difference.BothNotCacheDifference;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataSyncWrapperTest {

    @Mock
    private MetadataSyncResultHandler metadataSyncResultHandler;

    @Mock
    private DataMetadataHandler<BaseClusterIdBase> dataBasesHandler;

    @Mock
    private DataMetadataHandler<BaseClusterIdBase> clusterService;


    private BaseSyncBase baseSyncBase = SDKManageTest.createJvm();


    private MetadataSyncWrapper metadataSyncWrapper = new MetadataSyncWrapper();


    private MockData mockData = new MockData();

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

        mockData.source = dataBasesHandler;
        mockData.target = clusterService;

    }

    @Test
    public void test_readOnly() {

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

    @Test
    public void test_firstToWhom() {
        baseSyncBase.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        baseSyncBase.setFirstToWhom(FirstToWhom.RUNTIME);
        metadataSyncWrapper.getMetadataSyncConfig().setClusterServiceType(ClusterTrusteeshipType.SELF);
        metadataSyncWrapper.createDifference();

        mockData.mock_firstToWhom();

    }

    @Test
    public void test_databaseFlow_self() {
        baseSyncBase.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        metadataSyncWrapper.getMetadataSyncConfig().setClusterServiceType(ClusterTrusteeshipType.SELF);
        metadataSyncWrapper.createDifference();

        mockData.mock_init();
        mockData.mock_sync();
        mockData.mock_sync();
        mockData.mock_check();

    }


    @Test
    public void test_runtimeFlow() {
        baseSyncBase.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
        metadataSyncWrapper.getMetadataSyncConfig().setClusterServiceType(ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
        metadataSyncWrapper.createDifference();
        mockData.mock_init();
        mockData.mock_sync();
        mockData.mock_sync();
        mockData.mock_check();
    }

    /**
     * 求差
     */
    @SuppressWarnings("unchecked")
    class MockData {

        private DataMetadataHandler<BaseClusterIdBase> source;

        private DataMetadataHandler<BaseClusterIdBase> target;

        private List<BaseClusterIdBase> sourceAllList = new ArrayList<>();

        private List<BaseClusterIdBase> targetAllList = new ArrayList<>();

        private Long addIndex = 0L;

        public void firstSource() {

        }

        public void firstTarget() {

        }


        public void mock_init() {
            List<BaseClusterIdBase> runtimeMetadataList = this.createRuntimeMetadata(5);
            List<BaseClusterIdBase> runtimeMetadataList2 = this.createRuntimeMetadata(10);
            List<BaseClusterIdBase> runtimeMetadataList3 = this.createRuntimeMetadata(3);

            List<BaseClusterIdBase> sourceList = new ArrayList<>();
            sourceList.addAll(runtimeMetadataList);
            sourceList.addAll(runtimeMetadataList2);
            this.sourceAddList(sourceList);
            Mockito.when(source.getData()).thenReturn(sourceList);

            List<BaseClusterIdBase> targetList = new ArrayList<>();
            targetList.addAll(runtimeMetadataList2);
            targetList.addAll(runtimeMetadataList3);
            Mockito.when(target.getData()).thenReturn(targetList);
            Mockito.doAnswer((invocation) -> {
                Collection<BaseClusterIdBase> all = (Collection<BaseClusterIdBase>) invocation.getArgument(0, Collection.class);
                List<BaseClusterIdBase> add = (List<BaseClusterIdBase>) invocation.getArgument(1, List.class);
                List<BaseClusterIdBase> update = (List<BaseClusterIdBase>) invocation.getArgument(2, List.class);
                List<BaseClusterIdBase> delete = (List<BaseClusterIdBase>) invocation.getArgument(3, List.class);

                Assert.assertEquals(0, update.size());
                Assert.assertEquals(15, all.size());

                Assert.assertEquals(runtimeMetadataList.size(), add.size());
                this.assertList(add, runtimeMetadataList);

                Assert.assertEquals(runtimeMetadataList3.size(), delete.size());
                this.assertList(delete, runtimeMetadataList3);

                this.assertAll(all);
                return null;
            }).when(target).handleAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            //MetadataSyncWrapperTest.this.metadataSyncWrapper.getFirstTimeCheckDifference().difference();
            BothNotCacheDifference bothNotCacheDifference =
                Mockito.spy(metadataSyncWrapper.getFirstTimeCheckDifference());
            metadataSyncWrapper.setFirstTimeCheckDifference(bothNotCacheDifference);
            MetadataSyncWrapperTest.this.metadataSyncWrapper.run();

            Mockito.verify(bothNotCacheDifference, Mockito.times(1))
                .difference();

            this.targetAllList.addAll(targetList);

        }

        public void mock_firstToWhom() {
            List<BaseClusterIdBase> runtimeMetadataList = this.createRuntimeMetadata(10);
            Mockito.when(target.getData()).thenReturn(runtimeMetadataList);

            Mockito.doNothing().doAnswer((invocation) -> {
                Collection<BaseClusterIdBase> all = (Collection<BaseClusterIdBase>) invocation.getArgument(0, Collection.class);
                List<BaseClusterIdBase> add = (List<BaseClusterIdBase>) invocation.getArgument(1, List.class);
                List<BaseClusterIdBase> update = (List<BaseClusterIdBase>) invocation.getArgument(2, List.class);
                List<BaseClusterIdBase> delete = (List<BaseClusterIdBase>) invocation.getArgument(3, List.class);
                Assert.assertEquals(0, update.size());
                Assert.assertEquals(0, delete.size());
                Assert.assertEquals(runtimeMetadataList.size(), add.size());
                this.assertList(add, runtimeMetadataList);

                this.assertAll(all);
                return null;
            }).when(source).handleAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            MetadataSyncWrapperTest.this.metadataSyncWrapper.getFirstTimeDifference().difference();
        }

        public void mock_sync() {
            List<BaseClusterIdBase> runtimeMetadataList = this.createRuntimeMetadata(5);
            Mockito.when(source.getData()).thenReturn(runtimeMetadataList);
            Mockito.doNothing().doAnswer((invocation) -> {
                Collection<BaseClusterIdBase> all = (Collection<BaseClusterIdBase>) invocation.getArgument(0, Collection.class);
                List<BaseClusterIdBase> add = (List<BaseClusterIdBase>) invocation.getArgument(1, List.class);
                List<BaseClusterIdBase> update = (List<BaseClusterIdBase>) invocation.getArgument(2, List.class);
                List<BaseClusterIdBase> delete = (List<BaseClusterIdBase>) invocation.getArgument(3, List.class);
                Assert.assertEquals(0, update.size());
                Assert.assertEquals(0, delete.size());
                Assert.assertEquals(runtimeMetadataList.size(), add.size());
                this.assertList(add, runtimeMetadataList);

                this.assertAll(all);
                return null;
            }).when(target).handleAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            MetadataSyncWrapperTest.this.metadataSyncWrapper.getTimingSyncDifference().difference();

            this.sourceAddList(runtimeMetadataList);
        }

        public void mock_check() {
            List<BaseClusterIdBase> deleteList = this.createRuntimeMetadata(5);
            List<BaseClusterIdBase> targetList = new ArrayList<>();
            targetList.addAll(deleteList);
            targetList.addAll(sourceAllList.subList(0, sourceAllList.size() - 5));

            List<BaseClusterIdBase> addList = new ArrayList<>();
            addList.addAll(sourceAllList.subList(sourceAllList.size() - 5, sourceAllList.size()));

            Mockito.when(target.getData()).thenReturn(targetList);

            Mockito.doAnswer((invocation) -> {
                Collection<BaseClusterIdBase> all = (Collection<BaseClusterIdBase>) invocation.getArgument(0, Collection.class);
                List<BaseClusterIdBase> add = (List<BaseClusterIdBase>) invocation.getArgument(1, List.class);
                List<BaseClusterIdBase> update = (List<BaseClusterIdBase>) invocation.getArgument(2, List.class);
                List<BaseClusterIdBase> delete = (List<BaseClusterIdBase>) invocation.getArgument(3, List.class);

                Assert.assertEquals(0, update.size());
                Assert.assertEquals(this.sourceAllList.size(), all.size());

                Assert.assertEquals(addList.size(), add.size());
                this.assertList(add, addList);

                Assert.assertEquals(deleteList.size(), delete.size());
                this.assertList(delete, deleteList);

                this.assertAll(all);
                return null;
            }).when(target).handleAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            MetadataSyncWrapperTest.this.metadataSyncWrapper.getCheckDifference().difference();
        }


        public void assertAll(Collection<BaseClusterIdBase> all) {
            this.assertList(all, this.sourceAllList);
        }

        public void assertList(Collection<BaseClusterIdBase> all, List<BaseClusterIdBase> sourceList) {
            List<BaseClusterIdBase> newAll = new ArrayList<>(all);
            newAll.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));
            sourceList.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));
            for (int i = 0; i < newAll.size(); i++) {
                Assert.assertEquals(sourceList.get(i).getId(), newAll.get(i).getId());
            }

        }

        public List<BaseClusterIdBase> createRuntimeMetadata(int count) {

            List<BaseClusterIdBase> runtimeMetadataList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Long index = this.addIndex++;
                RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
                runtimeMetadata.setClusterId(1L);
                runtimeMetadata.setId(index);
                runtimeMetadata.setHost("127.0.0." + index);
                runtimeMetadata.setPort(8080);

                runtimeMetadataList.add(runtimeMetadata);
            }
            return runtimeMetadataList;
        }

        private void sourceAddList(List<BaseClusterIdBase> newDataList) {
            this.sourceAllList.addAll(newDataList);
        }
    }

}
