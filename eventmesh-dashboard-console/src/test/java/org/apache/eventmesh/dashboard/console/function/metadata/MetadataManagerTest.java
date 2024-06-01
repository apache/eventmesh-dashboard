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

package org.apache.eventmesh.dashboard.console.function.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.cache.ClusterCacheBase;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.MetadataServiceWrapper.SingleMetadataServiceWrapper;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.db.RuntimeMetadataHandlerToDbImpl;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.SyncDataService;
import org.apache.eventmesh.dashboard.core.metadata.cluster.RuntimeSyncFromClusterService;
import org.apache.eventmesh.dashboard.console.function.optration.TopicMetadataHandlerToClusterImpl;
import org.apache.eventmesh.dashboard.console.service.runtime.RuntimeService;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Slf4j
class MetadataManagerTest extends ClusterCacheBase {

    private final MetadataManager metadataManager = new MetadataManager();
    private static RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
    private static TopicEntity topicEntity = new TopicEntity();

    private SyncDataService<RuntimeMetadata> sourceService1 = Mockito.mock(RuntimeSyncFromClusterService.class);
    @Autowired
    private RuntimeMetadataHandlerToDbImpl targetService1;

    @Autowired
    private SyncDataService<TopicEntity> sourceService2;

    MetadataHandler<TopicEntity> targetService2 = Mockito.mock(TopicMetadataHandlerToClusterImpl.class);
    @Autowired
    private RuntimeService runtimeService;

    @BeforeAll
    public static void init() {
        topicEntity.setClusterId(0L);
        topicEntity.setTopicName("");
        topicEntity.setStorageId(0L);
        topicEntity.setRetentionMs(0L);
        topicEntity.setType(0);
        topicEntity.setDescription("");
        topicEntity.setId(0L);
        topicEntity.setClusterId(0L);

        runtimeMetadata.setHost("0.0.0.0");
        runtimeMetadata.setPort(1000);
        runtimeMetadata.setJmxPort(0);
        runtimeMetadata.setRack("");
        runtimeMetadata.setEndpointMap("");
        runtimeMetadata.setStorageClusterId(0L);
        runtimeMetadata.setStartTimestamp(0L);
        runtimeMetadata.setClusterName("");
        runtimeMetadata.setRegistryAddress("");
        runtimeMetadata.setClusterId(0L);

    }

    @BeforeEach
    public void initManager() {

        Mockito.when(sourceService1.getData()).thenReturn(Collections.singletonList(runtimeMetadata));

        Mockito.doNothing().when(targetService2).addMetadataObject(Mockito.any());
        Mockito.doNothing().when(targetService2).addMetadata(Mockito.any(List.class));

        MetadataServiceWrapper metadataServiceWrapper = new MetadataServiceWrapper();
        metadataServiceWrapper.setDbToService(
            new MetadataServiceWrapper.SingleMetadataServiceWrapper(true, sourceService1, targetService1));

        metadataServiceWrapper.setServiceToDb(new SingleMetadataServiceWrapper(true, sourceService1, targetService1));
        metadataServiceWrapper.setServiceToDb(new SingleMetadataServiceWrapper(false, sourceService2, targetService2));

        metadataManager.addMetadataService(metadataServiceWrapper);
    }

    @Test
    public void testMetadataToDb() throws InterruptedException {
        metadataManager.run(true, false);
        Thread.sleep(2000);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        assertEquals(1000, runtimeEntities.get(runtimeEntities.size() - 1).getPort());
    }

    @Test
    public void testMetadataToService() throws InterruptedException {
        metadataManager.run(false, true);
        Thread.sleep(2000);
    }

    @Test
    public void testUpdateMetadata() throws InterruptedException {
        metadataManager.run(true, false);
        Thread.sleep(2000);

        runtimeMetadata.setHost("1.1.1.1");
        Mockito.when(sourceService1.getData()).thenReturn(Collections.emptyList());
        metadataManager.run(true, false);
        Thread.sleep(2000);

        metadataManager.run(true, false);
        Thread.sleep(2000);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        assertEquals(1, runtimeEntities.size());
    }

    @Test
    public void testDeleteMetadata() throws InterruptedException {
        metadataManager.run(true, false);
        Thread.sleep(2000);
        runtimeMetadata.setHost("1.1.1.1");
        Mockito.when(sourceService1.getData()).thenReturn(Collections.singletonList(runtimeMetadata));
        metadataManager.run(true, false);
        Thread.sleep(2000);
        List<RuntimeEntity> runtimeEntities = runtimeService.selectAll();
        assertEquals(1, runtimeEntities.size());
    }

}