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

import org.apache.eventmesh.dashboard.common.model.metadata.RegistryMetadata;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.cache.ClusterCacheBase;
import org.apache.eventmesh.dashboard.console.entity.meta.MetaEntity;
import org.apache.eventmesh.dashboard.console.service.registry.RegistryDataService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;

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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:meta-test.sql")
class RegistryMetadataHandlerToDbImplTest extends ClusterCacheBase {

    @Autowired
    private MetadataHandler<RegistryMetadata> registryMetadataHandlerToDb;

    private RegistryMetadata registryMetadata;

    @Autowired
    private RegistryDataService registryDataService;

    @BeforeEach
    public void initData() {
        registryMetadata = new RegistryMetadata();
        registryMetadata.setClusterName("cluster1");
        registryMetadata.setClusterId(1L);
        registryMetadata.setName("registry1");
        registryMetadata.setType("nacos");
        registryMetadata.setVersion("1.0.0");
        registryMetadata.setHost("192.168.3.10");
        registryMetadata.setPort(8848);
        registryMetadata.setRole("leader");
        registryMetadata.setUsername("nacos");
        registryMetadata.setParams("a=1&b=2");
    }

    @Test
    public void testAddMetadata() {
        registryMetadataHandlerToDb.addMetadata(registryMetadata);
        List<MetaEntity> metaEntities = registryDataService.selectAll();
        MetaEntity meta = metaEntities.get(metaEntities.size() - 1);
        assertEquals("registry1", meta.getName());
        assertEquals(1L, meta.getClusterId());
    }
}