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
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManager;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataSyncManagerTest {

    private MetadataSyncManager metadataSyncManager = new MetadataSyncManager();

    private Map<Class<?>, MetadataSyncManager.MetadataSyncWrapper> metadataSyncWrapperMap;


    @Mock
    private RemotingManager remotingManager;

    @Mock
    private MetadataHandler dataBasesHandler;

    @Mock
    private MetadataHandler clusterHandler;

    private List<RuntimeMetadata> databasesList = new ArrayList<>();
    private List<RuntimeMetadata> clusterList = new ArrayList<>();

    @Before
    public void init() throws IllegalAccessException {
        MetadataSyncManager.MetadataSyncConfig metadataSyncConfig = new MetadataSyncManager.MetadataSyncConfig();
        metadataSyncConfig.setDataBasesHandler(dataBasesHandler);
        metadataSyncConfig.setClusterService(clusterHandler);
        metadataSyncConfig.setMetadataClass(RuntimeMetadata.class);
        metadataSyncManager.setRemotingManager(this.remotingManager);
        metadataSyncManager.register(metadataSyncConfig);

        Field metadataSyncWrapperMapField = FieldUtils.getField(MetadataSyncManager.class, "metadataSyncWrapperMap", true);
        metadataSyncWrapperMap = (Map<Class<?>, MetadataSyncManager.MetadataSyncWrapper>) metadataSyncWrapperMapField.get(metadataSyncManager);

    }


    @Test
    public void test_sync_mock() {
        //
        // ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP   3
        //   databases    cluster
        // A    5             3      2
        // B    3             5     -2
        // C    5(2)          5(2)   2
        // ClusterTrusteeshipType.TRUSTEESHIP                   3
        //   databases     cluster
        // E    5             5     0
        // F    3             5     -2
        // G    5             3     2

        this.mock_data(1, 5, 3);
        this.mock_data(2, 3, 5);
        this.mock_data(3, 5, 5);
        this.mock_data(5, 5, 3);
        this.mock_data(6, 3, 5);
        this.mock_data(7, 5, 5);
        MetadataSyncManager.MetadataSyncWrapper metadataSyncWrapper = this.metadataSyncWrapperMap.get(RuntimeMetadata.class);
        Mockito.when(this.dataBasesHandler.getData()).thenReturn(this.databasesList);
        Mockito.when(this.clusterHandler.getData()).thenReturn(this.clusterList);
        Mockito.when(this.remotingManager.isClusterTrusteeshipType(Mockito.anyLong(), Mockito.any())).thenAnswer((invocation) -> {
            Long clusterId = (Long) invocation.getArgument(0);
            ClusterTrusteeshipType clusterTrusteeshipType = (ClusterTrusteeshipType) invocation.getArgument(1);
            if (Objects.equals(clusterTrusteeshipType, ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP) && clusterId < 4) {
                return true;
            }
            if (Objects.equals(clusterTrusteeshipType, ClusterTrusteeshipType.TRUSTEESHIP) && clusterId > 4) {
                return true;
            }
            return false;
        });

        metadataSyncWrapper.run();
    }

    private void mock_data(long clusterId, int databasesCount, int clusterCount) {
        int index = databasesCount > clusterCount ? databasesCount : clusterCount;
        for (int i = 0; i < index; i++) {
            RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
            runtimeMetadata.setClusterId(clusterId);
            runtimeMetadata.setPort(i);
            runtimeMetadata.setHost(clusterId + "." + i + "");
            if (i < databasesCount) {
                this.databasesList.add(runtimeMetadata);
            }
            if (i < clusterCount) {
                if (databasesCount == clusterCount) {
                    if (i == 0 || i == 1) {
                        runtimeMetadata = new RuntimeMetadata();
                        runtimeMetadata.setClusterId(clusterId);
                        runtimeMetadata.setPort(i);
                        runtimeMetadata.setHost(clusterId + ".." + i + " " + i);
                    }
                }
                this.clusterList.add(runtimeMetadata);
            }

        }
    }
}
