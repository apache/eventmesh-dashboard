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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
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
import org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.metadata.AclDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.ClusterRelationshipDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.ConnectionDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionConfig;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionManage;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ClusterRelationshipSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ConnectionSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.KafkaAclSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.AclConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterRelationshipConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConnectionConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SnapshotMetadataHandler;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    public void snapshotFactoryConvertsWholeSnapshotBeforeDeliveryAndRetriesFromSource() {
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        DataMetadataHandler<Object> source = mock(DataMetadataHandler.class);
        ConvertMetaData<Object, BaseClusterIdBase> converter = mock(ConvertMetaData.class);
        SnapshotMetadataHandler<BaseClusterIdBase> target = mock(SnapshotMetadataHandler.class);
        ColonyDO<ClusterDO> colony = new ColonyDO<>();
        BaseClusterIdBase first = metadata(1L);
        BaseClusterIdBase second = metadata(2L);
        Object rowOne = new Object();
        Object rowTwo = new Object();
        List<Object> rows = Arrays.asList(rowOne, rowTwo);
        AtomicInteger attempts = new AtomicInteger();

        when(source.getData()).thenReturn(rows);
        when(converter.toMetaData(rowOne)).thenReturn(first);
        when(converter.toMetaData(rowTwo)).thenReturn(second);
        org.mockito.Mockito.doAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("destination unavailable");
            }
            return null;
        }).when(target).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.same(colony));
        factory.setDataMetadataHandler(source);
        factory.setConvertMetaData(converter);
        factory.setSnapshotMetadataHandler(target);
        factory.setColonyDO(colony);

        assertThrows(IllegalStateException.class, factory::loadData);
        verify(target).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.same(colony));
        factory.loadData();

        verify(source, org.mockito.Mockito.times(2)).getData();
        verify(target, org.mockito.Mockito.times(2)).synchronize(org.mockito.ArgumentMatchers.argThat(snapshot ->
            snapshot.size() == 2 && snapshot.get(0).getId().equals(1L) && snapshot.get(1).getId().equals(2L)),
            org.mockito.ArgumentMatchers.same(colony));
    }

    @Test
    public void snapshotFactoryDoesNotDeliverPartialOrAmbiguousSnapshots() {
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        DataMetadataHandler<Object> source = mock(DataMetadataHandler.class);
        ConvertMetaData<Object, BaseClusterIdBase> converter = mock(ConvertMetaData.class);
        SnapshotMetadataHandler<BaseClusterIdBase> target = mock(SnapshotMetadataHandler.class);
        BaseClusterIdBase first = metadata(1L);
        Object rowOne = new Object();
        Object rowTwo = new Object();
        when(source.getData()).thenReturn(Arrays.asList(rowOne, rowTwo));
        when(converter.toMetaData(rowOne)).thenReturn(first);
        when(converter.toMetaData(rowTwo)).thenThrow(new IllegalArgumentException("bad row"));
        factory.setDataMetadataHandler(source);
        factory.setConvertMetaData(converter);
        factory.setSnapshotMetadataHandler(target);

        assertThrows(IllegalArgumentException.class, factory::loadData);
        verify(target, never()).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());

        doReturn(metadata(1L)).when(converter).toMetaData(rowTwo);
        assertThrows(IllegalArgumentException.class, factory::loadData);
        verify(target, never()).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());

        doReturn(null).when(converter).toMetaData(rowTwo);
        assertThrows(IllegalArgumentException.class, factory::loadData);
        verify(target, never()).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());

        doReturn(metadataWithoutId()).when(converter).toMetaData(rowTwo);
        assertThrows(IllegalArgumentException.class, factory::loadData);
        verify(target, never()).synchronize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void emptySnapshotIsDeliveredAndSnapshotDatabaseHandlersRejectReverseWrites() {
        SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        DataMetadataHandler<Object> source = mock(DataMetadataHandler.class);
        SnapshotMetadataHandler<BaseClusterIdBase> target = mock(SnapshotMetadataHandler.class);
        when(source.getData()).thenReturn(Collections.emptyList());
        factory.setDataMetadataHandler(source);
        factory.setConvertMetaData(mock(ConvertMetaData.class));
        factory.setSnapshotMetadataHandler(target);

        factory.loadData();

        verify(target).synchronize(org.mockito.ArgumentMatchers.argThat(List::isEmpty), org.mockito.ArgumentMatchers.isNull());
        assertThrows(UnsupportedOperationException.class,
            () -> new AclDataMetadataHandler(mock(org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper.class))
                .handleAll(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertThrows(UnsupportedOperationException.class,
            () -> new ConnectionDataMetadataHandler(mock(org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper.class))
                .handleAll(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertThrows(UnsupportedOperationException.class,
            () -> new ClusterRelationshipDataMetadataHandler(mock(org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper.class))
                .handleAll(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void registryBindsNewMetadataKindsToTypedSnapshotSourcesAndConverters() {
        assertRegistered(MetadataType.ACL, AclDataMetadataHandler.class, AclConvertMetaData.INSTANCE, KafkaAclSnapshotHandler.class);
        assertRegistered(MetadataType.CONNECTION, ConnectionDataMetadataHandler.class, ConnectionConvertMetaData.INSTANCE,
            ConnectionSnapshotHandler.class);
        assertRegistered(MetadataType.CLUSTER_RELATIONSHIP, ClusterRelationshipDataMetadataHandler.class,
            ClusterRelationshipConvertMetaData.INSTANCE, ClusterRelationshipSnapshotHandler.class);
    }

    @Test
    public void aclAndRelationshipSnapshotSourcesReturnRowsAndPropagateMapperFailures() {
        AclMapper aclMapper = mock(AclMapper.class);
        AclDataMetadataHandler aclHandler = new AclDataMetadataHandler(aclMapper);
        List<AclEntity> aclRows = Collections.singletonList(aclEntity());
        when(aclMapper.selectKafkaSnapshot()).thenReturn(aclRows);
        assertSame(aclRows, aclHandler.getData());
        verify(aclMapper).selectKafkaSnapshot();

        ClusterRelationshipMapper relationshipMapper = mock(ClusterRelationshipMapper.class);
        ClusterRelationshipDataMetadataHandler relationshipHandler = new ClusterRelationshipDataMetadataHandler(relationshipMapper);
        List<ClusterRelationshipEntity> relationshipRows = Collections.singletonList(
            relationship(91L, ClusterType.EVENTMESH_CLUSTER, 92L, ClusterType.STORAGE_KAFKA_BROKER, 1L));
        when(relationshipMapper.selectSnapshot()).thenReturn(relationshipRows);
        assertSame(relationshipRows, relationshipHandler.getData());
        verify(relationshipMapper).selectSnapshot();

        when(aclMapper.selectKafkaSnapshot()).thenThrow(new IllegalStateException("ACL snapshot query failed"));
        when(relationshipMapper.selectSnapshot()).thenThrow(new IllegalStateException("relationship snapshot query failed"));
        assertThrows(IllegalStateException.class, aclHandler::getData);
        assertThrows(IllegalStateException.class, relationshipHandler::getData);
    }

    @Test
    public void connectionSnapshotUsesPersistedIdsAndReturnsDefensiveScopedCopies() {
        ConnectionSnapshotHandler handler = new ConnectionSnapshotHandler();
        ConnectionMetadata first = connection(1001L, 50L, 60L, "orders");
        ConnectionMetadata second = connection(1002L, 50L, 60L, "orders");
        ConnectionMetadata ended = connection(1003L, 50L, 60L, "ended");
        ended.setEndTime(LocalDateTime.of(2026, 10, 6, 0, 0));
        ConnectionMetadata inactive = connection(1004L, 50L, 61L, "inactive");
        inactive.setStatus(0L);
        ConnectionMetadata deleted = connection(1005L, 51L, 60L, "deleted");
        deleted.setIsDelete(1);

        handler.synchronize(Arrays.asList(first, second, ended, inactive, deleted), null);

        List<ConnectionMetadata> all = handler.list();
        assertEquals(2, all.size());
        assertEquals(Set.of(1001L, 1002L), all.stream().map(ConnectionMetadata::getId).collect(java.util.stream.Collectors.toSet()));
        List<ConnectionMetadata> scoped = handler.list(50L, 60L);
        assertEquals(2, scoped.size());
        assertTrue(handler.list(50L, 61L).isEmpty());
        assertTrue(handler.list(51L, 60L).isEmpty());

        ConnectionMetadata scopedCopy = scoped.stream().filter(value -> value.getId().equals(1001L)).findFirst().orElseThrow();
        scopedCopy.setDescription("caller mutation");
        ConnectionMetadata storedCopy = handler.list(50L, 60L).stream()
            .filter(value -> value.getId().equals(1001L)).findFirst().orElseThrow();
        assertEquals("connection-1001", storedCopy.getDescription());
        assertNotSame(scopedCopy, storedCopy);

        ConnectionMetadata duplicateOne = connection(1006L, 50L, 60L, "before-duplicate");
        handler.synchronize(Collections.singletonList(duplicateOne), null);
        ConnectionMetadata duplicateTwo = connection(1006L, 50L, 60L, "after-duplicate");
        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(Arrays.asList(duplicateOne, duplicateTwo), null));
        assertEquals("before-duplicate", handler.list().get(0).getTopic());
    }

    @Test
    public void clusterMetadataDomainAppliesConvertedRelationshipAndUnlinksStatusThree() {
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

        relationship.setStatus(3L);
        domain.setClusterEntityAndDefinitionCluster(Collections.emptyList(), Collections.singletonList(relationship), new HashMap<>());

        assertFalse(parent.getStorageColonyDOMap().containsKey(702L));
        assertNull(child.getSuperiorId());
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
            802L, ClusterType.STORAGE_KAFKA_BROKER, 1L);
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

        assertThrows(IllegalStateException.class, manager::sync);
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
        FunctionManage enabledManager = configuredFunctionManage(true, Set.of(MetadataType.CONNECTION));
        ClusterMetadataDomain enabledDomain = (ClusterMetadataDomain) ReflectionTestUtils.getField(enabledManager, "clusterMetadataDomain");
        enabledDomain.rootClusterDHO();
        try {
            ReflectionTestUtils.invokeMethod(enabledManager, "buildMetadataSyncManage");
            MetadataSyncManage syncManager = (MetadataSyncManage) ReflectionTestUtils.getField(enabledManager, "metadataSyncManage");
            assertEquals(Collections.singleton(MetadataType.CONNECTION), syncManager.getSyncMetadataCreateFactoryMap().keySet());
            assertSame(ReflectionTestUtils.getField(enabledManager, "defaultMetadataSyncResultHandler"),
                ReflectionTestUtils.getField(syncManager, "metadataSyncResultHandler"));
            assertSame(enabledDomain.getColonyDO(), ReflectionTestUtils.getField(syncManager, "colonyDO"));
            SyncMetadataCreateFactory factory = syncManager.getSyncMetadataCreateFactoryMap().get(MetadataType.CONNECTION);
            assertEquals(MetadataType.CONNECTION, factory.getMetadataType());
            assertSame(mapper(MetadataType.CONNECTION), factory.getDatabaseAndMetadataMapper());
        } finally {
            ReflectionTestUtils.invokeMethod(enabledManager, "closeMetadataSync");
        }
        MetadataSyncManage enabledSyncManager = (MetadataSyncManage) ReflectionTestUtils.getField(enabledManager, "metadataSyncManage");
        assertTrue(((java.util.concurrent.ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(enabledSyncManager,
            "scheduledExecutorService")).isShutdown());

        FunctionManage disabledManager = configuredFunctionManage(false, Set.of(MetadataType.CONNECTION));
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
    public void metadataManagerRegistersAndRunsOnlySelectedConnectionSnapshotThenCloses() {
        ConnectionMapper connectionMapper = mock(ConnectionMapper.class);
        AclMapper aclMapper = mock(AclMapper.class);
        ClusterRelationshipMapper relationshipMapper = mock(ClusterRelationshipMapper.class);
        ConnectionDataMetadataHandler connectionSource = new ConnectionDataMetadataHandler(connectionMapper);
        AclDataMetadataHandler aclSource = new AclDataMetadataHandler(aclMapper);
        ClusterRelationshipDataMetadataHandler relationshipSource = new ClusterRelationshipDataMetadataHandler(relationshipMapper);
        ConnectionSnapshotHandler target = spy(new ConnectionSnapshotHandler());
        MetadataSyncManage manager = configuredManager(
            Arrays.asList(connectionSource, aclSource, relationshipSource), Collections.singletonList(target));
        ColonyDO<ClusterDO> colony = new ColonyDO<>();
        manager.setColonyDO(colony);
        DatabaseAndMetadataMapper mapping = mapper(MetadataType.CONNECTION);
        ConnectionEntity row = new ConnectionEntity();
        row.setId(901L);
        row.setClusterId(902L);
        row.setRuntimeId(903L);
        row.setStatus(1L);
        row.setTopic("selected");
        when(connectionMapper.selectSnapshot()).thenReturn(Collections.singletonList(row));

        try {
            manager.init(Integer.MAX_VALUE, 60000, Collections.singletonList(mapping));
            assertEquals(Collections.singleton(MetadataType.CONNECTION), manager.getSyncMetadataCreateFactoryMap().keySet());
            SyncMetadataCreateFactory factory = manager.getSyncMetadataCreateFactoryMap().get(MetadataType.CONNECTION);
            assertEquals(MetadataType.CONNECTION, factory.getMetadataType());
            assertSame(mapping, factory.getDatabaseAndMetadataMapper());
            assertSame(colony, ReflectionTestUtils.getField(factory, "colonyDO"));
            manager.run();
            assertEquals(1, target.list().size());
            assertEquals(Long.valueOf(901L), target.list().get(0).getId());
            verify(target).synchronize(org.mockito.ArgumentMatchers.argThat(snapshot -> snapshot.size() == 1
                && snapshot.get(0).getId().equals(901L)), same(colony));
            verify(connectionMapper).selectSnapshot();
            verify(aclMapper, never()).selectAll();
            verify(relationshipMapper, never()).selectSnapshot();
        } finally {
            manager.close();
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void metadataManagerFailsFastWhenSnapshotSourceOrTargetIsMissing() {
        DatabaseAndMetadataMapper mapping = mapper(MetadataType.CONNECTION);
        MetadataSyncManage missingSource = configuredManager(Collections.emptyList(), Collections.singletonList(new ConnectionSnapshotHandler()));
        assertThrows(IllegalStateException.class,
            () -> missingSource.init(Integer.MAX_VALUE, 60000, Collections.singletonList(mapping)));
        missingSource.close();

        ConnectionDataMetadataHandler source = new ConnectionDataMetadataHandler(mock(ConnectionMapper.class));
        MetadataSyncManage missingTarget = configuredManager(Collections.singletonList(source), Collections.emptyList());
        assertThrows(IllegalStateException.class,
            () -> missingTarget.init(Integer.MAX_VALUE, 60000, Collections.singletonList(mapping)));
        missingTarget.close();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void metadataManagerRegistrationSkipsSnapshotAndUnselectedRemoteTypes() {
        AclMapper aclMapper = mock(AclMapper.class);
        MetadataSyncResultHandler resultHandler = mock(MetadataSyncResultHandler.class);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList((List) Collections.singletonList(new AclDataMetadataHandler(aclMapper)));
        manager.setSnapshotMetadataHandlerList(Collections.singletonList(new KafkaAclSnapshotHandler()));
        manager.setMetadataSyncResultHandler(resultHandler);
        try {
            manager.init(Integer.MAX_VALUE, 60000, Collections.singletonList(mapper(MetadataType.ACL)));

            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(991L);
            cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
            cluster.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
            manager.register(cluster);

            assertTrue(manager.getSyncMetadataCreateFactoryMap().containsKey(MetadataType.ACL));
            java.util.Map<?, ?> registeredConfigs = (java.util.Map<?, ?>) ReflectionTestUtils.getField(manager, "metadataSyncConfigMap");
            assertEquals(1, registeredConfigs.size());
            assertTrue(registeredConfigs.values().stream().allMatch(value -> ((List<?>) value).isEmpty()));
            verify(resultHandler).register(org.mockito.ArgumentMatchers.argThat(List::isEmpty));
            verify(aclMapper, never()).selectKafkaSnapshot();
        } finally {
            manager.close();
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void metadataManagerRetainsSelectedLegacyTopicWrapperBesideSnapshotFactories() {
        TopicDataMetadataHandler topicSource = mock(TopicDataMetadataHandler.class);
        AclMapper aclMapper = mock(AclMapper.class);
        AclDataMetadataHandler aclSource = new AclDataMetadataHandler(aclMapper);
        KafkaAclSnapshotHandler aclTarget = new KafkaAclSnapshotHandler();
        MetadataSyncResultHandler resultHandler = mock(MetadataSyncResultHandler.class);
        Remoting2Manage remoting = mock(Remoting2Manage.class);
        DataMetadataHandler<BaseClusterIdBase> remoteTopicHandler = mock(DataMetadataHandler.class);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList((List) Arrays.asList(topicSource, aclSource));
        manager.setSnapshotMetadataHandlerList(Collections.singletonList(aclTarget));
        manager.setMetadataSyncResultHandler(resultHandler);
        ReflectionTestUtils.setField(manager, "remoting2Manage", remoting);
        DatabaseAndMetadataMapper topicMapping = mapper(MetadataType.TOPIC);
        DatabaseAndMetadataMapper aclMapping = mapper(MetadataType.ACL);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(992L);
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        cluster.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
        cluster.setFirstToWhom(ClusterTrusteeshipType.FirstToWhom.DASHBOARD);
        when(remoting.createDataMetadataHandler(topicMapping.resolveMetadataHandlerClass(cluster.getClusterType()), cluster))
            .thenReturn(remoteTopicHandler);

        try {
            manager.init(Integer.MAX_VALUE, 60000, Arrays.asList(topicMapping, aclMapping));
            manager.register(cluster);

            java.util.Map<?, ?> registeredConfigs = (java.util.Map<?, ?>) ReflectionTestUtils.getField(manager, "metadataSyncConfigMap");
            List<?> wrappers = (List<?>) registeredConfigs.get(cluster.getUnique());
            assertEquals(1, wrappers.size());
            MetadataSyncWrapper wrapper = (MetadataSyncWrapper) wrappers.get(0);
            assertEquals(MetadataType.TOPIC, wrapper.getMetadataSyncConfig().getMetadataType());
            verify(resultHandler).register(org.mockito.ArgumentMatchers.argThat(results -> results.size() == 1
                && results.get(0).getMetadataType() == MetadataType.TOPIC));
            verify(remoting).createDataMetadataHandler(topicMapping.resolveMetadataHandlerClass(cluster.getClusterType()), cluster);
            verify(aclMapper, never()).selectKafkaSnapshot();
        } finally {
            manager.close();
        }
    }

    private static void assertRegistered(MetadataType type, Class<?> handlerType, ConvertMetaData<?, ?> converter, Class<?> targetType) {
        DatabaseAndMetadataType registered = Arrays.stream(DatabaseAndMetadataType.values())
            .filter(value -> value.getDatabaseAndMetadataMapper().getMetaType() == type)
            .findFirst().orElseThrow();
        assertEquals(handlerType, registered.getDatabaseAndMetadataMapper().getDatabaseHandlerClass());
        assertSame(converter, registered.getDatabaseAndMetadataMapper().getConvertMetaData());
        assertEquals(targetType, registered.getDatabaseAndMetadataMapper().getSnapshotHandlerClass());
        assertTrue(SnapshotMetadataHandler.class.isAssignableFrom(registered.getDatabaseAndMetadataMapper().getSnapshotHandlerClass()));
        assertFalse(DataMetadataHandler.class.isAssignableFrom(registered.getDatabaseAndMetadataMapper().getSnapshotHandlerClass()));
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

    private static BaseClusterIdBase metadata(long id) {
        BaseClusterIdBase metadata = new AclMetadata();
        metadata.setId(id);
        return metadata;
    }

    private static BaseClusterIdBase metadataWithoutId() {
        return new AclMetadata();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MetadataSyncManage configuredManager(List<?> sources, List<?> targets) {
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList((List) sources);
        manager.setSnapshotMetadataHandlerList((List) targets);
        manager.setMetadataSyncResultHandler(mock(MetadataSyncResultHandler.class));
        return manager;
    }

    private static DatabaseAndMetadataMapper mapper(MetadataType type) {
        return Arrays.stream(DatabaseAndMetadataType.values())
            .map(DatabaseAndMetadataType::getDatabaseAndMetadataMapper)
            .filter(value -> value.getMetaType() == type)
            .findFirst().orElseThrow();
    }

    private static ConnectionMetadata connection(long id, long clusterId, long runtimeId, String topic) {
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setId(id);
        metadata.setClusterId(clusterId);
        metadata.setRuntimeId(runtimeId);
        metadata.setStatus(1L);
        metadata.setTopic(topic);
        metadata.setDescription("connection-" + id);
        return metadata;
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
            Collections.singletonList(new ConnectionDataMetadataHandler(mock(ConnectionMapper.class))));
        ReflectionTestUtils.setField(manager, "snapshotMetadataHandlerList", Collections.singletonList(new ConnectionSnapshotHandler()));
        ReflectionTestUtils.setField(manager, "defaultMetadataSyncResultHandler", new DefaultMetadataSyncResultHandler());
        return manager;
    }
}
