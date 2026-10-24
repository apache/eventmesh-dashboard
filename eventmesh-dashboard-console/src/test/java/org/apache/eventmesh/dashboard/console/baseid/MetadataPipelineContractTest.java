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

package org.apache.eventmesh.dashboard.console.baseid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionConfig;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionManage;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.AclConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterRelationshipConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConnectionConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncWrapper;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class MetadataPipelineContractTest {

    @Test
    public void aclConverterPreservesPersistedFieldsAndUsesRepositoryCodes() {
        AclEntity entity = aclEntity();

        AclMetadata metadata = AclConvertMetaData.INSTANCE.toMetaData(entity);

        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        assertEquals("principal-A", metadata.getPrincipal());
        assertEquals(entity.getOperation(), metadata.getOperation());
        assertEquals(entity.getHost(), metadata.getHost());
        assertEquals("TOPIC", metadata.getResourceType());
        assertEquals("ALLOW", metadata.getPermissionType());
        assertEquals(entity.getResourceName(), metadata.getResourceName());
        assertEquals(entity.getPatternType(), metadata.getPatternType());

        AclEntity roundTrip = AclConvertMetaData.INSTANCE.toEntity(metadata);

        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getStatus(), roundTrip.getStatus());
        assertEquals(entity.getCreateTime(), roundTrip.getCreateTime());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateTime(), roundTrip.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
        assertEquals(entity.getOperation(), roundTrip.getOperation());
        assertEquals(entity.getHost(), roundTrip.getHost());
        assertEquals(entity.getResourceName(), roundTrip.getResourceName());
        assertEquals(entity.getPatternType(), roundTrip.getPatternType());
        assertEquals(Integer.valueOf(10), roundTrip.getResourceType());
        assertEquals(Integer.valueOf(3), roundTrip.getPermissionType());
        assertEquals(entity.getPattern(), roundTrip.getPattern());

        AclEntity groupDeny = aclEntity();
        groupDeny.setResourceType(11);
        groupDeny.setPermissionType(2);
        AclMetadata groupDenyMetadata = AclConvertMetaData.INSTANCE.toMetaData(groupDeny);
        assertEquals("GROUP", groupDenyMetadata.getResourceType());
        assertEquals("DENY", groupDenyMetadata.getPermissionType());
        AclEntity groupDenyRoundTrip = AclConvertMetaData.INSTANCE.toEntity(groupDenyMetadata);
        assertEquals(Integer.valueOf(11), groupDenyRoundTrip.getResourceType());
        assertEquals(Integer.valueOf(2), groupDenyRoundTrip.getPermissionType());
    }

    @Test
    public void aclConverterRejectsUnsupportedCodesAndUnpersistableRocketMqFields() {
        AclEntity unsupportedResource = aclEntity();
        unsupportedResource.setResourceType(21);
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toMetaData(unsupportedResource));

        AclEntity unsupportedPermission = aclEntity();
        unsupportedPermission.setPermissionType(9);
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toMetaData(unsupportedPermission));

        AclMetadata rocketMqPolicy = AclConvertMetaData.INSTANCE.toMetaData(aclEntity());
        rocketMqPolicy.setPolicyType("Default");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(rocketMqPolicy));

        AclMetadata actions = AclConvertMetaData.INSTANCE.toMetaData(aclEntity());
        actions.setActions(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(actions));

        AclMetadata sourceIps = AclConvertMetaData.INSTANCE.toMetaData(aclEntity());
        sourceIps.setSourceIps(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(sourceIps));

        AclMetadata unsupportedType = AclConvertMetaData.INSTANCE.toMetaData(aclEntity());
        unsupportedType.setResourceType("CLUSTER");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(unsupportedType));

        AclMetadata unsupportedPermissionName = AclConvertMetaData.INSTANCE.toMetaData(aclEntity());
        unsupportedPermissionName.setPermissionType("READ_WRITE");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(unsupportedPermissionName));
    }

    @Test
    public void metadataConvertersReturnNullForNullSourceValues() {
        assertNull(AclConvertMetaData.INSTANCE.toMetaData(null));
        assertNull(AclConvertMetaData.INSTANCE.toEntity(null));
        assertNull(ConnectionConvertMetaData.INSTANCE.toMetaData(null));
        assertNull(ConnectionConvertMetaData.INSTANCE.toEntity(null));
        assertNull(ClusterRelationshipConvertMetaData.INSTANCE.toMetaData(null));
        assertNull(ClusterRelationshipConvertMetaData.INSTANCE.toEntity(null));
    }

    @Test
    public void connectionConverterPreservesPersistedScopeAndEndTime() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(101L);
        entity.setOrganizationId(204L);
        entity.setClusterId(202L);
        entity.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        entity.setRuntimeId(303L);
        entity.setStatus(3L);
        entity.setIsDelete(0);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 1, 2, 3, 4));
        entity.setCreateUserId(205L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 2, 5, 6, 7));
        entity.setUpdateUserId(206L);
        entity.setSourceType("client");
        entity.setSourceId(404L);
        entity.setSinkType("connector");
        entity.setSinkId(505L);
        entity.setTopic("orders");
        entity.setGroupId(606L);
        entity.setDescription("persisted connection");
        entity.setEndTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 10, 3, 8, 9, 10)));

        ConnectionMetadata metadata = ConnectionConvertMetaData.INSTANCE.toMetaData(entity);
        ConnectionEntity roundTrip = ConnectionConvertMetaData.INSTANCE.toEntity(metadata);

        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getOrganizationId(), metadata.getOrganizationId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getClusterType(), metadata.getClusterType());
        assertEquals(entity.getRuntimeId(), metadata.getRuntimeId());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getOrganizationId(), roundTrip.getOrganizationId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getClusterType(), roundTrip.getClusterType());
        assertEquals(entity.getRuntimeId(), roundTrip.getRuntimeId());
        assertEquals(entity.getStatus(), roundTrip.getStatus());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
        assertEquals(entity.getCreateTime(), roundTrip.getCreateTime());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateTime(), roundTrip.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getSourceType(), roundTrip.getSourceType());
        assertEquals(entity.getSourceId(), roundTrip.getSourceId());
        assertEquals(entity.getSinkType(), roundTrip.getSinkType());
        assertEquals(entity.getSinkId(), roundTrip.getSinkId());
        assertEquals(entity.getTopic(), roundTrip.getTopic());
        assertEquals(entity.getGroupId(), roundTrip.getGroupId());
        assertEquals(entity.getDescription(), roundTrip.getDescription());
        assertEquals(entity.getEndTime(), roundTrip.getEndTime());
    }

    @Test
    public void relationshipConverterPreservesEndpointsAndPersistedState() {
        ClusterRelationshipEntity entity = new ClusterRelationshipEntity();
        entity.setId(11L);
        entity.setClusterId(22L);
        entity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        entity.setStatus(3L);
        entity.setIsDelete(0);
        entity.setOrganizationId(23L);
        entity.setCreateTime(LocalDateTime.of(2026, 9, 1, 0, 0));
        entity.setCreateUserId(24L);
        entity.setUpdateTime(LocalDateTime.of(2026, 9, 2, 0, 0));
        entity.setUpdateUserId(25L);
        entity.setRelationshipId(33L);
        entity.setRelationshipType(ClusterType.STORAGE_KAFKA_BROKER);
        entity.setRelationshipTime(LocalDateTime.of(2026, 8, 1, 0, 0));
        entity.setUnRelationshipTime(LocalDateTime.of(2026, 8, 2, 0, 0));
        entity.setBindType("owned");

        ClusterRelationshipMetadata metadata = ClusterRelationshipConvertMetaData.INSTANCE.toMetaData(entity);
        ClusterRelationshipEntity roundTrip = ClusterRelationshipConvertMetaData.INSTANCE.toEntity(metadata);

        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getClusterType(), metadata.getClusterType());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getStatus(), roundTrip.getStatus());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
        assertEquals(entity.getOrganizationId(), metadata.getOrganizationId());
        assertEquals(entity.getOrganizationId(), roundTrip.getOrganizationId());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateTime(), roundTrip.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateTime(), roundTrip.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getRelationshipId(), roundTrip.getRelationshipId());
        assertEquals(entity.getRelationshipType(), roundTrip.getRelationshipType());
        assertNull(roundTrip.getRelationshipTime());
        assertNull(roundTrip.getUnRelationshipTime());
        assertNull(roundTrip.getBindType());
    }

    @Test
    public void clusterMetadataDomainLinksRelationshipsAndUnlinksStatusZero() {
        ClusterMetadataDomain domain = new ClusterMetadataDomain();
        ColonyDO<ClusterDO> topology = new ColonyDO<>();
        topology.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        HashMap<Long, ColonyDO<ClusterDO>> clusters = new HashMap<>();
        topology.setAllColonyDO(clusters);
        ColonyDO<ClusterDO> parent = new ColonyDO<>();
        parent.setClusterId(701L);
        parent.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        ColonyDO<ClusterDO> child = new ColonyDO<>();
        child.setClusterId(702L);
        child.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        clusters.put(parent.getClusterId(), parent);
        clusters.put(child.getClusterId(), child);
        ReflectionTestUtils.setField(domain, "colonyDO", topology);
        ClusterRelationshipEntity relationship = relationship(701L, ClusterType.EVENTMESH_CLUSTER,
            702L, ClusterType.STORAGE_KAFKA_BROKER, 1L);

        domain.setClusterEntityAndDefinitionCluster(Collections.emptyList(), Collections.singletonList(relationship), new HashMap<>());

        assertSame(child, parent.getStorageColonyDOMap().get(702L));
        assertEquals(Long.valueOf(701L), child.getSuperiorId());

        relationship.setStatus(0L);
        domain.setClusterEntityAndDefinitionCluster(Collections.emptyList(), Collections.singletonList(relationship), new HashMap<>());

        assertFalse(parent.getStorageColonyDOMap().containsKey(702L));
    }

    @Test
    public void functionManageKeepsIncrementalCursorsOnDomainFailureAndAdvancesAfterRecovery() {
        FunctionManage manager = new FunctionManage();
        RuntimeService runtimeService = mock(RuntimeService.class);
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        ReflectionTestUtils.setField(manager, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(manager, "clusterService", clusterService);
        ReflectionTestUtils.setField(manager, "clusterRelationshipService", relationshipService);
        FunctionConfig functionConfig = new FunctionConfig();
        functionConfig.setEnabledSync(true);
        ReflectionTestUtils.setField(manager, "functionConfig", functionConfig);
        ClusterMetadataDomain domain = (ClusterMetadataDomain) ReflectionTestUtils.getField(manager, "clusterMetadataDomain");
        domain.rootClusterDHO();

        ClusterRelationshipEntity unresolved = relationship(801L, ClusterType.EVENTMESH_CLUSTER,
            802L, ClusterType.STORAGE_KAFKA_BROKER, 0L);
        ClusterEntity inactiveCluster = new ClusterEntity();
        inactiveCluster.setId(803L);
        inactiveCluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        inactiveCluster.setStatus(0L);
        when(runtimeService.queryByUpdateTime(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        when(clusterService.queryByUpdateTime(org.mockito.ArgumentMatchers.any()))
            .thenReturn(Collections.emptyList(), Collections.singletonList(inactiveCluster));
        when(relationshipService.queryByUpdateTime(org.mockito.ArgumentMatchers.any()))
            .thenReturn(Collections.singletonList(unresolved), Collections.emptyList());

        RuntimeEntity runtimeCursor = (RuntimeEntity) ReflectionTestUtils.getField(manager, "runtimeEntity");
        ClusterEntity clusterCursor = (ClusterEntity) ReflectionTestUtils.getField(manager, "clusterEntity");
        ClusterRelationshipEntity relationshipCursor = (ClusterRelationshipEntity) ReflectionTestUtils.getField(manager, "clusterRelationshipEntity");
        LocalDateTime runtimeBefore = runtimeCursor.getUpdateTime();
        LocalDateTime clusterBefore = clusterCursor.getUpdateTime();
        LocalDateTime relationshipBefore = relationshipCursor.getUpdateTime();

        assertThrows(RuntimeException.class, manager::sync);
        assertEquals(runtimeBefore, runtimeCursor.getUpdateTime());
        assertEquals(clusterBefore, clusterCursor.getUpdateTime());
        assertEquals(relationshipBefore, relationshipCursor.getUpdateTime());

        manager.sync();

        assertTrue(runtimeCursor.getUpdateTime().isAfter(runtimeBefore));
        assertTrue(clusterCursor.getUpdateTime().isAfter(clusterBefore));
        assertTrue(relationshipCursor.getUpdateTime().isAfter(relationshipBefore));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void functionManageUsesExplicitSyncSelectionAndEnabledGate() {
        FunctionManage enabledManager = configuredFunctionManage(true, Set.of(MetadataType.TOPIC));
        ClusterMetadataDomain enabledDomain = (ClusterMetadataDomain) ReflectionTestUtils.getField(enabledManager, "clusterMetadataDomain");
        enabledDomain.rootClusterDHO();
        try {
            ReflectionTestUtils.invokeMethod(enabledManager, "buildMetadataSyncManage");
            MetadataSyncManage syncManager = (MetadataSyncManage) ReflectionTestUtils.getField(enabledManager, "metadataSyncManage");
            assertEquals(Collections.singleton(MetadataType.TOPIC), syncManager.getSyncMetadataCreateFactoryMap().keySet());
            assertSame(ReflectionTestUtils.getField(enabledManager, "defaultMetadataSyncResultHandler"),
                ReflectionTestUtils.getField(syncManager, "metadataSyncResultHandler"));
            assertSame(enabledDomain.getColonyDO(), ReflectionTestUtils.getField(syncManager, "colonyDO"));
            SyncMetadataCreateFactory factory = syncManager.getSyncMetadataCreateFactoryMap().get(MetadataType.TOPIC);
            assertEquals(MetadataType.TOPIC, factory.getMetadataType());
            assertSame(mapper(MetadataType.TOPIC), factory.getDatabaseAndMetadataMapper());
        } finally {
            ReflectionTestUtils.invokeMethod(enabledManager, "closeMetadataSync");
        }
        MetadataSyncManage enabledSyncManager = (MetadataSyncManage) ReflectionTestUtils.getField(enabledManager, "metadataSyncManage");
        assertTrue(((java.util.concurrent.ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(enabledSyncManager,
            "scheduledExecutorService")).isShutdown());

        FunctionManage disabledManager = configuredFunctionManage(false, Set.of(MetadataType.TOPIC));
        ClusterMetadataDomain disabledDomain = (ClusterMetadataDomain) ReflectionTestUtils.getField(disabledManager, "clusterMetadataDomain");
        disabledDomain.rootClusterDHO();
        try {
            ReflectionTestUtils.invokeMethod(disabledManager, "buildMetadataSyncManage");
            MetadataSyncManage syncManager = (MetadataSyncManage) ReflectionTestUtils.getField(disabledManager, "metadataSyncManage");
            assertTrue(syncManager.getSyncMetadataCreateFactoryMap().isEmpty());
            assertSame(ReflectionTestUtils.getField(disabledManager, "defaultMetadataSyncResultHandler"),
                ReflectionTestUtils.getField(syncManager, "metadataSyncResultHandler"));
            assertSame(disabledDomain.getColonyDO(), ReflectionTestUtils.getField(syncManager, "colonyDO"));
        } finally {
            ReflectionTestUtils.invokeMethod(disabledManager, "closeMetadataSync");
        }
        MetadataSyncManage disabledSyncManager = (MetadataSyncManage) ReflectionTestUtils.getField(disabledManager, "metadataSyncManage");
        assertTrue(((java.util.concurrent.ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(disabledSyncManager,
            "scheduledExecutorService")).isShutdown());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void metadataManagerRegistrationSkipsUnselectedRemoteTypes() {
        MetadataSyncResultHandler resultHandler = mock(MetadataSyncResultHandler.class);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList(Collections.emptyList());
        manager.setMetadataSyncResultHandler(resultHandler);
        try {
            manager.init(Integer.MAX_VALUE, 60000, Collections.<DatabaseAndMetadataMapper>emptyList());

            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(991L);
            cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
            cluster.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
            manager.register(cluster);

            assertTrue(manager.getSyncMetadataCreateFactoryMap().isEmpty());
            java.util.Map<?, ?> registeredConfigs = (java.util.Map<?, ?>) ReflectionTestUtils.getField(manager, "metadataSyncConfigMap");
            assertEquals(1, registeredConfigs.size());
            assertTrue(registeredConfigs.values().stream().allMatch(value -> ((List<?>) value).isEmpty()));
            verify(resultHandler).register(org.mockito.ArgumentMatchers.argThat(List::isEmpty));
        } finally {
            manager.close();
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void metadataManagerRegistersSelectedTopicWrapper() {
        TopicDataMetadataHandler topicSource = mock(TopicDataMetadataHandler.class);
        MetadataSyncResultHandler resultHandler = mock(MetadataSyncResultHandler.class);
        Remoting2Manage remoting = mock(Remoting2Manage.class);
        DataMetadataHandler<BaseClusterIdBase> remoteTopicHandler = mock(DataMetadataHandler.class);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList((List) Collections.singletonList(topicSource));
        manager.setMetadataSyncResultHandler(resultHandler);
        ReflectionTestUtils.setField(manager, "remoting2Manage", remoting);
        DatabaseAndMetadataMapper topicMapping = mapper(MetadataType.TOPIC);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(992L);
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        cluster.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
        cluster.setFirstToWhom(ClusterTrusteeshipType.FirstToWhom.DASHBOARD);
        when(remoting.createDataMetadataHandler(topicMapping.resolveMetadataHandlerClass(cluster.getClusterType()), cluster))
            .thenReturn(remoteTopicHandler);

        try {
            manager.init(Integer.MAX_VALUE, 60000, Collections.singletonList(topicMapping));
            manager.register(cluster);

            java.util.Map<?, ?> registeredConfigs = (java.util.Map<?, ?>) ReflectionTestUtils.getField(manager, "metadataSyncConfigMap");
            List<?> wrappers = (List<?>) registeredConfigs.get(cluster.getUnique());
            assertEquals(1, wrappers.size());
            MetadataSyncWrapper wrapper = (MetadataSyncWrapper) wrappers.get(0);
            assertEquals(MetadataType.TOPIC, wrapper.getMetadataSyncConfig().getMetadataType());
            verify(resultHandler).register(org.mockito.ArgumentMatchers.argThat(results -> results.size() == 1
                && results.get(0).getMetadataType() == MetadataType.TOPIC));
            verify(remoting).createDataMetadataHandler(topicMapping.resolveMetadataHandlerClass(cluster.getClusterType()), cluster);
        } finally {
            manager.close();
        }
    }

    private static AclEntity aclEntity() {
        AclEntity entity = new AclEntity();
        entity.setId(7L);
        entity.setClusterId(8L);
        entity.setStatus(1L);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 4, 1, 2, 3));
        entity.setCreateUserId(70L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 5, 4, 5, 6));
        entity.setUpdateUserId(71L);
        entity.setIsDelete(0);
        entity.setPattern("principal-A");
        entity.setOperation(1);
        entity.setPermissionType(3);
        entity.setHost("127.0.0.1");
        entity.setResourceType(10);
        entity.setResourceName("orders");
        entity.setPatternType(1);
        return entity;
    }

    private static DatabaseAndMetadataMapper mapper(MetadataType type) {
        return Arrays.stream(DatabaseAndMetadataType.values())
            .map(DatabaseAndMetadataType::getDatabaseAndMetadataMapper)
            .filter(value -> value.getMetaType() == type)
            .findFirst().orElseThrow();
    }

    private static ClusterRelationshipEntity relationship(long parentId, ClusterType parentType, long childId,
        ClusterType childType, long status) {
        ClusterRelationshipEntity entity = new ClusterRelationshipEntity();
        entity.setId(parentId + childId);
        entity.setClusterId(parentId);
        entity.setClusterType(parentType);
        entity.setRelationshipId(childId);
        entity.setRelationshipType(childType);
        entity.setStatus(status);
        entity.setIsDelete(0);
        return entity;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static FunctionManage configuredFunctionManage(boolean enabled, Set<MetadataType> selectedTypes) {
        FunctionManage manager = new FunctionManage();
        FunctionConfig config = new FunctionConfig();
        config.setEnabledSync(enabled);
        config.setIncludeSyncType(selectedTypes);
        ReflectionTestUtils.setField(manager, "functionConfig", config);
        ReflectionTestUtils.setField(manager, "dataMetadataHandlerList",
            Collections.singletonList(mock(TopicDataMetadataHandler.class)));
        ReflectionTestUtils.setField(manager, "defaultMetadataSyncResultHandler", new DefaultMetadataSyncResultHandler());
        return manager;
    }
}
