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


package org.apache.eventmesh.dashboard.console.model;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseOrganizationBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;
import org.apache.eventmesh.dashboard.console.controller.message.TopicController;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterOperationHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.report.collect.CollectManage;
import org.apache.eventmesh.dashboard.console.model.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;
import org.apache.eventmesh.dashboard.console.spring.support.DBRemotingResultHook;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.console.utils.data.adaptation.Adaptation;
import org.apache.eventmesh.dashboard.console.utils.data.adaptation.eventmesh.topic.AdaptationService.AdaptationMetadataTypeWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQGroupRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClusterMetadataIdentityTest {

    @Test
    public void clusterRoundTripHasOneIdentityAndOneCopyOfSharedFields() throws Exception {
        ClusterEntity entity = new ClusterEntity();
        entity.setId(20L);
        entity.setOrganizationId(7L);
        entity.setName("cluster");
        entity.setStatus(1L);
        entity.setClusterType(ClusterType.STORAGE_ROCKETMQ);
        entity.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
        ClusterMetadata metadata = ClusterConvertMetaData.INSTANCE.toMetaData(entity);
        Assert.assertEquals("20", metadata.nodeUnique());
        Assert.assertEquals("ClusterMetadata-20", metadata.getUnique());
        Assert.assertTrue(metadata.isCluster());
        Assert.assertFalse(Arrays.stream(ClusterMetadata.class.getMethods())
            .anyMatch(method -> method.getName().equals("getClusterId") || method.getName().equals("setClusterId")));
        for (String field : List.of("id", "clusterType", "trusteeshipType")) {
            int count = 0;
            for (Class<?> type = ClusterMetadata.class; type != null; type = type.getSuperclass()) {
                count += (int) Arrays.stream(type.getDeclaredFields()).filter(value -> value.getName().equals(field)).count();
            }
            Assert.assertEquals(field, 1, count);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(metadata);
        Assert.assertFalse(json.has("clusterId"));
        Assert.assertEquals(20L, json.get("id").asLong());
        ClusterEntity restored = ClusterConvertMetaData.INSTANCE.toEntity(metadata);
        Assert.assertEquals(entity.getId(), restored.getId());
        Assert.assertEquals(entity.getOrganizationId(), restored.getOrganizationId());
        Assert.assertEquals(entity.getClusterType(), restored.getClusterType());
        Assert.assertEquals(entity.getTrusteeshipType(), restored.getTrusteeshipType());
        ClusterMetadata decoded = mapper.readValue(
            "{\"id\":20,\"clusterType\":\"STORAGE_ROCKETMQ\",\"trusteeshipType\":\"TRUSTEESHIP\"}", ClusterMetadata.class);
        Assert.assertEquals(metadata.getClusterType(), decoded.getClusterType());
        Assert.assertEquals(metadata.getTrusteeshipType(), decoded.getTrusteeshipType());
    }

    @Test
    public void runtimeRoundTripRetainsOwnParentAndStorageIdentities() {
        RuntimeEntity entity = new RuntimeEntity();
        entity.setId(201L);
        entity.setStatus(1L);
        entity.setClusterId(20L);
        entity.setName("broker");
        entity.setHost("127.0.0.1");
        entity.setPort(10911);
        entity.setClusterType(ClusterType.STORAGE_ROCKETMQ);
        entity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        RuntimeMetadata metadata = RuntimeConvertMetaData.INSTANCE.toMetaData(entity);
        metadata.setStorageClusterId(30L);
        Assert.assertEquals(Long.valueOf(201), metadata.getId());
        Assert.assertEquals(Long.valueOf(20), metadata.getClusterId());
        Assert.assertEquals(Long.valueOf(30), metadata.getStorageClusterId());
        Assert.assertEquals("RuntimeMetadata-201", metadata.getUnique());
        Assert.assertEquals("ClusterMetadata-20", metadata.clusterUnique());
        Assert.assertEquals("127.0.0.1-10911", metadata.nodeUnique());
        Assert.assertFalse(metadata.isCluster());
        Assert.assertEquals(20L, new ObjectMapper().valueToTree(metadata).get("clusterId").asLong());
        RuntimeEntity restored = RuntimeConvertMetaData.INSTANCE.toEntity(metadata);
        Assert.assertEquals(entity.getId(), restored.getId());
        Assert.assertEquals(entity.getClusterId(), restored.getClusterId());
        Assert.assertEquals(entity.getClusterType(), restored.getClusterType());
        Assert.assertEquals(entity.getTrusteeshipType(), restored.getTrusteeshipType());
    }

    @Test
    public void inheritedStatePredicatesKeepExistingSemantics() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 8, 0, 0);
        for (BaseOrganizationBase metadata : List.of(new ClusterMetadata(), new RuntimeMetadata(), new TopicMetadata())) {
            metadata.setCreateTime(created);
            metadata.setUpdateTime(created);
            metadata.setStatus(1L);
            Assert.assertTrue(metadata.isInsert());
            Assert.assertFalse(metadata.isUpdate());
            Assert.assertFalse(metadata.isDelete());
            metadata.setUpdateTime(created.plusSeconds(1));
            Assert.assertTrue(metadata.isUpdate());
            Assert.assertFalse(metadata.isInsert());
            metadata.setStatus(0L);
            Assert.assertTrue(metadata.isDelete());
            Assert.assertFalse(metadata.isUpdate());
        }
    }

    @Test
    public void runtimeOwnershipMoveDoesNotChangeExistingEqualityContract() {
        RuntimeMetadata first = new RuntimeMetadata();
        RuntimeMetadata second = new RuntimeMetadata();
        first.setClusterId(20L);
        second.setClusterId(30L);
        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
        ClusterMetadata cluster = new ClusterMetadata();
        ClusterMetadata other = new ClusterMetadata();
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ);
        other.setClusterType(ClusterType.STORAGE_KAFKA);
        Assert.assertNotEquals(cluster, other);
        other.setClusterType(cluster.getClusterType());
        Assert.assertEquals(cluster, other);
        other.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        Assert.assertNotEquals(cluster, other);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void databaseSyncQueriesUseTargetIdentity() {
        DataMetadataHandler<Object> database = Mockito.mock(DataMetadataHandler.class);
        Mockito.when(database.getData(Mockito.any(Global2Request.class))).thenReturn(List.of());
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        factory.setDataMetadataHandler(database);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(20L);
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(201L);
        runtime.setClusterId(20L);
        factory.createDataMetadataHandler(cluster).getData(new Global2Request());
        factory.createDataMetadataHandler(runtime).getData(new Global2Request());
        ArgumentCaptor<Global2Request> requests = ArgumentCaptor.forClass(Global2Request.class);
        Mockito.verify(database, Mockito.times(2)).getData(requests.capture());
        Assert.assertEquals(Long.valueOf(20), requests.getAllValues().get(0).getClusterId());
        Assert.assertNull(requests.getAllValues().get(0).getRuntimeId());
        Assert.assertEquals(Long.valueOf(201), requests.getAllValues().get(1).getRuntimeId());
        Assert.assertNull(requests.getAllValues().get(1).getClusterId());
    }

    @Test
    public void remotingHandlerSeparatesClusterAndRuntimeScope() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(-9020L);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ);
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(-9201L);
        runtime.setClusterId(cluster.getId());
        runtime.setClusterType(cluster.getClusterType());
        Remoting2Manage.registerService(RocketMQGroupRemotingService.class);
        Remoting2Manage manager = Remoting2Manage.getInstance();
        try {
            Object clusterHandler = manager.createDataMetadataHandler(GroupRemotingService.class, cluster);
            Object runtimeHandler = manager.createDataMetadataHandler(GroupRemotingService.class, runtime);
            Assert.assertEquals(cluster.getId(), ReflectionTestUtils.getField(clusterHandler, "clusterId"));
            Assert.assertNull(ReflectionTestUtils.getField(clusterHandler, "runtimeId"));
            Assert.assertEquals(cluster.getId(), ReflectionTestUtils.getField(runtimeHandler, "clusterId"));
            Assert.assertEquals(runtime.getId(), ReflectionTestUtils.getField(runtimeHandler, "runtimeId"));
        } finally {
            SDKManage.getInstance().deleteClient(null, cluster.getUnique());
            SDKManage.getInstance().deleteClient(null, runtime.getUnique());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void topicCreationUsesClusterIdAndRuntimeParentId() {
        TopicController controller = new TopicController();
        ClusterMetadataDomain domain = Mockito.mock(ClusterMetadataDomain.class);
        TopicService topics = Mockito.mock(TopicService.class);
        DBRemotingResultHook hook = Mockito.mock(DBRemotingResultHook.class);
        AdaptationMetadataTypeWrapper adaptations = Mockito.mock(AdaptationMetadataTypeWrapper.class);
        Mockito.when(adaptations.get(Mockito.any(), Mockito.eq("create"))).thenReturn(Mockito.mock(Adaptation.class));
        ReflectionTestUtils.setField(controller, "clusterMetadataDomain", domain);
        ReflectionTestUtils.setField(controller, "topicService", topics);
        ReflectionTestUtils.setField(controller, "dbRemotingResultHook", hook);
        ReflectionTestUtils.setField(controller, "adaptationMetadataTypeWrapper", adaptations);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(20L);
        cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ);
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(201L);
        runtime.setClusterId(20L);
        runtime.setClusterType(cluster.getClusterType());
        Mockito.doAnswer(invocation -> {
            ClusterOperationHandler handler = invocation.getArgument(1);
            handler.handler(cluster);
            handler.handler(runtime);
            return null;
        }).when(domain).operation(Mockito.eq(20L), Mockito.any(ClusterOperationHandler.class));
        CreateTopicDTO request = new CreateTopicDTO();
        request.setClusterId(20L);
        request.setName("identity-test");
        controller.createTopic(request);
        ArgumentCaptor<List<TopicEntity>> entities = ArgumentCaptor.forClass(List.class);
        Mockito.verify(topics).batchInsert(entities.capture());
        Assert.assertEquals(2, entities.getValue().size());
        Assert.assertEquals(Long.valueOf(20), entities.getValue().get(0).getClusterId());
        Assert.assertEquals(Long.valueOf(0), entities.getValue().get(0).getRuntimeId());
        Assert.assertEquals(Long.valueOf(20), entities.getValue().get(1).getClusterId());
        Assert.assertEquals(Long.valueOf(201), entities.getValue().get(1).getRuntimeId());
        Mockito.verify(hook).monitor(entities.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectorCachesClusterByOwnIdentityAfterInheritanceChange() {
        CollectManage manager = new CollectManage();
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(20L);
        cluster.setStatus(1L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        Map<Long, ClusterMetadata> clusters = (Map<Long, ClusterMetadata>) ReflectionTestUtils.getField(manager, "clusterMetadataMap");
        Assert.assertSame(cluster, clusters.get(20L));
        Assert.assertEquals("ClusterMetadata-20", ReflectionTestUtils.invokeMethod(manager, "createCollectKey", cluster));
        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        Assert.assertFalse(clusters.containsKey(20L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void healthResultsAndUnregistrationUseClusterIdentity() throws Exception {
        Health2Service service = new Health2Service();
        Map<Long, ClusterHealthCheckService> checks = (Map<Long, ClusterHealthCheckService>)
            ReflectionTestUtils.getField(service, "clusterHealthCheckServiceMap");
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(20L);
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(201L);
        runtime.setClusterId(20L);
        Class<?> wrapperType = Class.forName(Health2Service.class.getName() + "$HealthCheckWrapper");
        Constructor<?> constructor = wrapperType.getDeclaredConstructor(Health2Service.class);
        constructor.setAccessible(true);
        for (BaseSyncBase metadata : List.of(cluster, runtime)) {
            Object wrapper = constructor.newInstance(service);
            ReflectionTestUtils.setField(wrapper, "baseSyncBase", metadata);
            HealthCheckResultEntity result = ReflectionTestUtils.invokeMethod(wrapper, "createHealthCheckResultEntity", "test");
            Assert.assertNotNull(result);
            Assert.assertEquals(Long.valueOf(20), result.getClusterId());
            Assert.assertEquals(metadata.getId(), result.getTypeId());
            ClusterHealthCheckService check = Mockito.mock(ClusterHealthCheckService.class);
            checks.put(20L, check);
            service.unRegister(metadata);
            Mockito.verify(check).unRegister(metadata);
            Assert.assertFalse(checks.containsKey(20L));
        }
    }
}
