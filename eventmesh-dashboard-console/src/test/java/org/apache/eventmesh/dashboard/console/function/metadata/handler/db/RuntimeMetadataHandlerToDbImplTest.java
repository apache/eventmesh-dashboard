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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.cache.ClusterCacheBase;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:runtime-test.sql")
class RuntimeMetadataHandlerToDbImplTest extends ClusterCacheBase {

    @Autowired
    private MetadataHandler<RuntimeMetadata> runtimeMetadataHandlerToDb;

    private RuntimeMetadata runtimeMetadata;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterService clusterService;

    @BeforeEach
    public void initData() {
        runtimeMetadata = new RuntimeMetadata();
        runtimeMetadata.setHost("192.168.3.14");
        runtimeMetadata.setPort(1234);
        runtimeMetadata.setRack("rack");
        runtimeMetadata.setRegistryAddress("12333");
        runtimeMetadata.setJmxPort(12345);
        runtimeMetadata.setClusterName("cluster1");
        runtimeMetadata.setEndpointMap("endpointMap");
        runtimeMetadata.setStorageClusterId(1L);
        runtimeMetadata.setStartTimestamp(123456L);
        runtimeMetadata.setRegistryAddress("registryAddress");
    }

    @Test
    void testAddMetadataWhenClusterExistRuntimeNotExist() {
        runtimeMetadataHandlerToDb.addMetadata(runtimeMetadata);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        Assertions.assertEquals("endpointMap", runtimeEntities.get(runtimeEntities.size() - 1).getEndpointMap());
        List<ClusterEntity> clusterEntities = clusterService.selectAll();
        Assertions.assertEquals("cluster1", clusterEntities.get(clusterEntities.size() - 1).getName());
    }

    @Test
    void testAddMetadataWhenClusterNotExistRuntimeNotExist() {
        runtimeMetadata.setClusterName("cluster2");
        runtimeMetadataHandlerToDb.addMetadata(runtimeMetadata);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        Assertions.assertEquals("endpointMap", runtimeEntities.get(runtimeEntities.size() - 1).getEndpointMap());
        List<ClusterEntity> clusterEntities = clusterService.selectAll();
        Assertions.assertEquals("cluster2", clusterEntities.get(clusterEntities.size() - 1).getName());
    }

    @Test
    void testAddMetadataWhenClusterExistRuntimeExist() {
        runtimeMetadata.setClusterId(1L);
        runtimeMetadata.setHost("127.0.0.1");
        runtimeMetadata.setPort(12345);
        runtimeMetadataHandlerToDb.addMetadata(runtimeMetadata);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        Assertions.assertEquals(3, runtimeEntities.size());
        Assertions.assertNotEquals("2024-03-18 09:53:10", runtimeEntities.get(0).getUpdateTime().toString());
        Assertions.assertNotEquals(-1, runtimeEntities.get(0).getStartTimestamp());
        Assertions.assertEquals(-1, runtimeEntities.get(1).getStartTimestamp());
        List<ClusterEntity> clusterEntities = clusterService.selectAll();
        Assertions.assertEquals(1, clusterEntities.size());
    }

    @Test
    void testAddMetadataWhenClusterNotExistRuntimeExist() {
        runtimeMetadata.setClusterName("cluster2");
        runtimeMetadata.setHost("127.0.0.1");
        runtimeMetadata.setPort(12345);
        runtimeMetadataHandlerToDb.addMetadata(runtimeMetadata);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        Assertions.assertEquals(3, runtimeEntities.size());
        Assertions.assertNotEquals("2024-03-18 09:53:10", runtimeEntities.get(0).getUpdateTime());
        Assertions.assertNotEquals(-1, runtimeEntities.get(0).getStartTimestamp());
        Assertions.assertEquals(-1, runtimeEntities.get(1).getStartTimestamp());
        List<ClusterEntity> clusterEntities = clusterService.selectAll();
        Assertions.assertEquals(1, clusterEntities.size());
    }
}