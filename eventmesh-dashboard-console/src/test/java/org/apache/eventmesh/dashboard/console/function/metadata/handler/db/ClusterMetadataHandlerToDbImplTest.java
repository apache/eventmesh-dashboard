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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.common.enums.StoreType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandler;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import java.util.List;

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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:client-test.sql")
class ClusterMetadataHandlerToDbImplTest {

    @Autowired
    MetadataHandler<ClusterMetadata> clusterMetadataHandlerToDb;

    @Autowired
    ClusterService clusterDataService;

    @Test
    public void testAddMetadata() {
        ClusterMetadata clusterMetadata = new ClusterMetadata();
        clusterMetadata.setClusterName("cluster");
        clusterMetadata.setDescription("");
        clusterMetadata.setAuthType(0);
        clusterMetadata.setRunState(0);
        clusterMetadata.setBootstrapServers("");
        clusterMetadata.setRegistryAddress("");
        clusterMetadata.setClientProperties("test-property");
        clusterMetadata.setJmxProperties("");
        clusterMetadata.setEventmeshVersion("");
        clusterMetadata.setStoreType(StoreType.REDIS);
        clusterMetadataHandlerToDb.addMetadata(clusterMetadata);
        List<ClusterEntity> results = clusterDataService.selectAll();
        assertEquals("test-property", results.get(results.size() - 1).getClientProperties());
    }
}