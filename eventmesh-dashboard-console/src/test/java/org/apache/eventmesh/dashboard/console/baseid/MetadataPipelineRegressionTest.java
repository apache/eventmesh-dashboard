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

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ConnectionSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ClusterRelationshipSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.KafkaAclSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.AclConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterRelationshipConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConnectionConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.service.remoting.kafka.AclRemotingService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;

/** Regression contracts for metadata conversion and database-driven registry wiring. */
public class MetadataPipelineRegressionTest {

    @Test
    public void databaseJobsRegisterAclConnectionAndRelationshipConverters() {
        assertEquals(MetadataType.ACL, DatabaseAndMetadataType.valueOf("ACL").getDatabaseAndMetadataMapper().getMetaType());
        assertEquals(MetadataType.CONNECTION,
            DatabaseAndMetadataType.valueOf("CONNECTION").getDatabaseAndMetadataMapper().getMetaType());
        assertEquals(MetadataType.CLUSTER_RELATIONSHIP,
            DatabaseAndMetadataType.valueOf("CLUSTER_RELATIONSHIP").getDatabaseAndMetadataMapper().getMetaType());
    }

    @Test
    public void aclConverterMapsOnlyPersistableKafkaResourceAndPermissionCodes() {
        AclEntity entity = new AclEntity();
        entity.setId(101L);
        entity.setClusterId(12L);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 10, 1, 2, 3));
        entity.setCreateUserId(501L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 11, 4, 5, 6));
        entity.setUpdateUserId(502L);
        entity.setStatus(1L);
        entity.setIsDelete(0);
        entity.setPattern("User:orders-reader");
        entity.setHost("*");
        entity.setResourceType(10);
        entity.setResourceName("orders");
        entity.setOperation(3);
        entity.setPermissionType(2);
        entity.setPatternType(1);

        AclMetadata metadata = AclConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals("User:orders-reader", metadata.getPrincipal());
        assertEquals("TOPIC", metadata.getResourceType());
        assertEquals("DENY", metadata.getPermissionType());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        AclEntity roundTrip = AclConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getPattern(), roundTrip.getPattern());
        assertEquals(entity.getResourceType(), roundTrip.getResourceType());
        assertEquals(entity.getPermissionType(), roundTrip.getPermissionType());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
        assertEquals(entity.getOperation(), roundTrip.getOperation());
        assertEquals(entity.getHost(), roundTrip.getHost());
        assertEquals(entity.getResourceName(), roundTrip.getResourceName());
        assertEquals(entity.getPatternType(), roundTrip.getPatternType());

        entity.setResourceType(11);
        entity.setPermissionType(3);
        metadata = AclConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals("GROUP", metadata.getResourceType());
        assertEquals("ALLOW", metadata.getPermissionType());
        assertThrows(IllegalArgumentException.class, () -> {
            entity.setResourceType(12);
            AclConvertMetaData.INSTANCE.toMetaData(entity);
        });
        AclMetadata invalidPermission = AclConvertMetaData.INSTANCE.toMetaData(roundTrip);
        invalidPermission.setPermissionType("ADMIN");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(invalidPermission));
        AclMetadata rocketMqPolicy = new AclMetadata();
        rocketMqPolicy.setPolicyType("CUSTOM");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(rocketMqPolicy));
    }

    @Test
    public void connectionConverterPreservesClusterRuntimeAndPersistedEndTime() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(202L);
        entity.setClusterId(20L);
        entity.setRuntimeId(2001L);
        entity.setSourceType("connector");
        entity.setSourceId(31L);
        entity.setSinkType("connector");
        entity.setSinkId(32L);
        entity.setTopic("orders");
        entity.setGroupId(40L);
        entity.setDescription("persisted connection");
        entity.setEndTime(Timestamp.valueOf("2026-10-16 13:14:15"));

        ConnectionMetadata metadata = ConnectionConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getRuntimeId(), metadata.getRuntimeId());
        assertEquals("orders", metadata.getTopic());
        assertEquals(LocalDateTime.of(2026, 10, 16, 13, 14, 15), metadata.getEndTime());
        ConnectionEntity roundTrip = ConnectionConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getEndTime(), roundTrip.getEndTime());
        assertEquals(entity.getSourceId(), roundTrip.getSourceId());
        assertEquals(entity.getSinkId(), roundTrip.getSinkId());

        entity.setEndTime(null);
        metadata = ConnectionConvertMetaData.INSTANCE.toMetaData(entity);
        assertNull(metadata.getEndTime());
    }

    @Test
    public void relationshipConverterPreservesEndpointTypesAndRoles() {
        ClusterRelationshipEntity entity = new ClusterRelationshipEntity();
        entity.setId(303L);
        entity.setOrganizationId(7L);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 12, 1, 2, 3));
        entity.setCreateUserId(601L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 13, 4, 5, 6));
        entity.setUpdateUserId(602L);
        entity.setClusterId(30L);
        entity.setRelationshipId(3001L);
        entity.setClusterType(ClusterType.EVENTMESH);
        entity.setRelationshipType(ClusterType.STORAGE_KAFKA_RAFT);
        entity.setStatus(1L);
        entity.setIsDelete(0);

        ClusterRelationshipMetadata metadata = ClusterRelationshipConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getClusterType(), metadata.getClusterType());
        assertEquals(entity.getRelationshipId(), metadata.getRelationshipId());
        assertEquals(entity.getRelationshipType(), metadata.getRelationshipType());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getOrganizationId(), metadata.getOrganizationId());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        ClusterRelationshipEntity roundTrip = ClusterRelationshipConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getClusterType(), roundTrip.getClusterType());
        assertEquals(entity.getRelationshipId(), roundTrip.getRelationshipId());
        assertEquals(entity.getRelationshipType(), roundTrip.getRelationshipType());
        assertEquals(entity.getOrganizationId(), roundTrip.getOrganizationId());
        assertEquals(entity.getCreateTime(), roundTrip.getCreateTime());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateTime(), roundTrip.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
    }

    @Test
    public void connectionSnapshotFiltersClosedDeletedAndInactiveRowsByClusterAndRuntime() {
        ConnectionSnapshotHandler handler = new ConnectionSnapshotHandler();
        ConnectionMetadata active = connection(1L, 10L, 101L);
        ConnectionMetadata closed = connection(2L, 10L, 101L);
        closed.setEndTime(LocalDateTime.of(2026, 10, 1, 0, 0));
        ConnectionMetadata deleted = connection(3L, 10L, 101L);
        deleted.setIsDelete(1);
        ConnectionMetadata inactive = connection(4L, 10L, 101L);
        inactive.setStatus(0L);

        handler.synchronize(List.of(active, closed, deleted, inactive), null);

        assertEquals(List.of(1L), handler.list().stream().map(ConnectionMetadata::getId).toList());
        assertEquals(List.of(1L), handler.list(10L, 101L).stream().map(ConnectionMetadata::getId).toList());
        assertEquals(List.of(), handler.list(10L, 999L));
        active.setTopic("caller mutation");
        assertEquals("topic-1", handler.list(10L, 101L).get(0).getTopic());

        handler.synchronize(List.of(), null);
        assertTrue(handler.list().isEmpty());
    }

    @Test
    public void relationshipSnapshotPreservesDuplicateActiveEdgesAndMovesEndpoint() {
        ColonyDO<ClusterDO> topology = topology();
        ClusterRelationshipSnapshotHandler handler = new ClusterRelationshipSnapshotHandler();
        ClusterRelationshipMetadata first = relationship(1L, 10L, 20L);
        ClusterRelationshipMetadata second = relationship(2L, 10L, 20L);
        ClusterRelationshipMetadata deletedDuplicate = relationship(3L, 10L, 20L);
        deletedDuplicate.setStatus(0L);

        handler.synchronize(List.of(first, second), topology);
        assertTrue(topology.getAllColonyDO().get(10L).getStorageColonyDOMap().containsKey(20L));
        assertEquals(Long.valueOf(10L), topology.getAllColonyDO().get(20L).getSuperiorId());

        handler.synchronize(List.of(first, second, deletedDuplicate), topology);
        assertTrue("one duplicate tombstone must not unbind a still-active relationship",
            topology.getAllColonyDO().get(10L).getStorageColonyDOMap().containsKey(20L));
        assertEquals(Long.valueOf(10L), topology.getAllColonyDO().get(20L).getSuperiorId());

        handler.synchronize(List.of(relationship(4L, 11L, 20L)), topology);
        assertFalse(topology.getAllColonyDO().get(10L).getStorageColonyDOMap().containsKey(20L));
        assertTrue(topology.getAllColonyDO().get(11L).getStorageColonyDOMap().containsKey(20L));
        assertEquals(Long.valueOf(11L), topology.getAllColonyDO().get(20L).getSuperiorId());
    }

    @Test
    public void relationshipSnapshotRejectsInvalidEndpointsBeforeChangingTopology() {
        ColonyDO<ClusterDO> topology = topology();
        ClusterRelationshipSnapshotHandler handler = new ClusterRelationshipSnapshotHandler();
        handler.synchronize(List.of(relationship(1L, 10L, 20L)), topology);
        ClusterRelationshipMetadata invalid = relationship(2L, 11L, 20L);
        invalid.setRelationshipType(ClusterType.EVENTMESH_CLUSTER);

        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(List.of(invalid), topology));
        assertTrue(topology.getAllColonyDO().get(10L).getStorageColonyDOMap().containsKey(20L));
        assertFalse(topology.getAllColonyDO().get(11L).getStorageColonyDOMap().containsKey(20L));
        assertEquals(Long.valueOf(10L), topology.getAllColonyDO().get(20L).getSuperiorId());
    }

    @Test
    public void relationshipSnapshotRejectsMultipleParentsCyclesAndSelfEdgesAtomically() {
        ColonyDO<ClusterDO> topology = topology();
        ClusterRelationshipSnapshotHandler handler = new ClusterRelationshipSnapshotHandler();
        ClusterRelationshipMetadata current = relationship(1L, 10L, 20L);
        handler.synchronize(List.of(current), topology);

        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(
            List.of(relationship(2L, 10L, 20L), relationship(3L, 11L, 20L)), topology));
        assertTopologyStillBoundTo(topology, 10L);

        ClusterRelationshipMetadata reverse = relationship(4L, 20L, 10L);
        reverse.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        reverse.setRelationshipType(ClusterType.EVENTMESH_CLUSTER);
        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(List.of(current, reverse), topology));
        assertTopologyStillBoundTo(topology, 10L);

        ClusterRelationshipMetadata self = relationship(5L, 10L, 10L);
        self.setRelationshipType(ClusterType.EVENTMESH_CLUSTER);
        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(List.of(self), topology));
        assertTopologyStillBoundTo(topology, 10L);

        ClusterRelationshipMetadata wrongParentType = relationship(6L, 10L, 20L);
        wrongParentType.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        assertThrows(IllegalArgumentException.class, () -> handler.synchronize(List.of(wrongParentType), topology));
        assertTopologyStillBoundTo(topology, 10L);

        assertThrows(IllegalStateException.class, () -> handler.synchronize(
            List.of(current, relationship(8L, 10L, 999L)), topology));
        assertTopologyStillBoundTo(topology, 10L);

        ClusterRelationshipMetadata deleted = relationship(7L, 10L, 20L);
        deleted.setIsDelete(1);
        handler.synchronize(List.of(deleted), topology);
        assertFalse(topology.getAllColonyDO().get(10L).getStorageColonyDOMap().containsKey(20L));
        assertNull(topology.getAllColonyDO().get(20L).getSuperiorId());
    }

    @Test
    public void kafkaAclSnapshotReconcilesFullBindingsWithoutBroadeningOwnership() {
        FakeKafkaAclService service = new FakeKafkaAclService();
        AclMetadata unrelated = acl(90L, "User:other", "unrelated", 3);
        service.remote.put(Binding.of(unrelated), unrelated);
        KafkaAclSnapshotHandler handler = new KafkaAclSnapshotHandler(ignored -> service);
        ColonyDO<ClusterDO> colony = kafkaColony();
        AclMetadata readOrders = acl(1L, "User:shared", "orders", 3);
        AclMetadata writeOrders = acl(2L, "User:shared", "orders", 4);
        AclMetadata readPayments = acl(3L, "User:shared", "payments", 3);

        handler.synchronize(List.of(readOrders, writeOrders, readPayments), colony);
        assertEquals(4, service.remote.size());
        assertEquals(3, service.createCount);
        handler.synchronize(List.of(readOrders, writeOrders, readPayments), colony);
        assertEquals(3, service.createCount);

        AclMetadata deletedDuplicate = acl(4L, "User:shared", "orders", 3);
        deletedDuplicate.setStatus(0L);
        handler.synchronize(List.of(readOrders, writeOrders, readPayments, deletedDuplicate), colony);
        assertTrue(service.remote.containsKey(Binding.of(readOrders)));
        assertTrue(service.remote.containsKey(Binding.of(writeOrders)));
        assertTrue(service.remote.containsKey(Binding.of(readPayments)));

        AclMetadata changedRead = acl(1L, "User:shared", "orders", 5);
        handler.synchronize(List.of(changedRead, writeOrders, readPayments), colony);
        assertFalse(service.remote.containsKey(Binding.of(readOrders)));
        assertTrue(service.remote.containsKey(Binding.of(changedRead)));
        assertTrue(service.remote.containsKey(Binding.of(writeOrders)));
        assertTrue(service.remote.containsKey(Binding.of(readPayments)));
        assertTrue("unowned Broker binding is preserved", service.remote.containsKey(Binding.of(unrelated)));
    }

    @Test
    public void kafkaAclSnapshotPropagatesQueryAndWriteFailuresAndRetriesDesiredState() {
        ColonyDO<ClusterDO> colony = kafkaColony();
        FakeKafkaAclService service = new FakeKafkaAclService();
        KafkaAclSnapshotHandler handler = new KafkaAclSnapshotHandler(ignored -> service);
        AclMetadata first = acl(1L, "User:retry", "one", 3);
        AclMetadata second = acl(2L, "User:retry", "two", 3);

        service.queryCode = 503;
        assertThrows(IllegalStateException.class, () -> handler.synchronize(List.of(first), colony));
        assertEquals(0, service.createCount);
        assertEquals(0, service.deleteCount);
        service.queryCode = 200;

        service.failCreateCall = 1;
        assertThrows(IllegalStateException.class, () -> handler.synchronize(List.of(first), colony));
        assertTrue(service.remote.isEmpty());
        handler.synchronize(List.of(first), colony);
        assertTrue(service.remote.containsKey(Binding.of(first)));

        service.failCreateCall = service.createCount + 2;
        AclMetadata third = acl(3L, "User:retry", "three", 3);
        assertThrows(IllegalStateException.class, () -> handler.synchronize(List.of(second, third), colony));
        assertTrue("first acknowledged create remains present for retry reconciliation",
            service.remote.containsKey(Binding.of(first)));
        handler.synchronize(List.of(third), colony);
        assertFalse("a partial create removed from the next DB snapshot must be cleaned up",
            service.remote.containsKey(Binding.of(first)));
        assertFalse(service.remote.containsKey(Binding.of(second)));
        assertTrue(service.remote.containsKey(Binding.of(third)));

        service.failDeleteCall = service.deleteCount + 1;
        assertThrows(IllegalStateException.class, () -> handler.synchronize(List.of(), colony));
        assertTrue(service.remote.containsKey(Binding.of(third)));
        handler.synchronize(List.of(), colony);
        assertTrue(service.remote.isEmpty());
    }

    @Test
    public void kafkaAclSnapshotAppliesUnownedTombstonesAndPreservesOtherBindings() {
        FakeKafkaAclService service = new FakeKafkaAclService();
        AclMetadata revoked = acl(1L, "User:revoke", "revoked", 3);
        AclMetadata active = acl(2L, "User:shared", "active", 3);
        AclMetadata unrelated = acl(3L, "User:unrelated", "unrelated", 4);
        service.remote.put(Binding.of(revoked), revoked);
        service.remote.put(Binding.of(active), active);
        service.remote.put(Binding.of(unrelated), unrelated);
        AclMetadata tombstone = acl(4L, "User:revoke", "revoked", 3);
        tombstone.setStatus(0L);

        new KafkaAclSnapshotHandler(ignored -> service).synchronize(List.of(tombstone, active), kafkaColony());

        assertFalse("an explicit tombstone removes a preexisting unowned remote binding",
            service.remote.containsKey(Binding.of(revoked)));
        assertTrue("an active binding remains", service.remote.containsKey(Binding.of(active)));
        assertTrue("an unrelated remote binding remains", service.remote.containsKey(Binding.of(unrelated)));
        assertEquals(1, service.deleteCount);
        assertEquals(0, service.createCount);
    }

    @Test
    public void kafkaAclSnapshotSupportsBrokerAndRaftAndSkipsOtherClusters() {
        for (ClusterType type : List.of(ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT)) {
            FakeKafkaAclService service = new FakeKafkaAclService();
            AclMetadata desired = acl(1L, "User:" + type, "topic-" + type, 3);
            new KafkaAclSnapshotHandler(ignored -> service).synchronize(List.of(desired), topologyWithCluster(70L, type));
            assertTrue("registered Kafka cluster type is synchronized: " + type, service.remote.containsKey(Binding.of(desired)));
        }

        FakeKafkaAclService untouched = new FakeKafkaAclService();
        AclMetadata remote = acl(2L, "User:remote", "untouched", 3);
        untouched.remote.put(Binding.of(remote), remote);
        new KafkaAclSnapshotHandler(ignored -> untouched).synchronize(List.of(remote),
            topologyWithCluster(70L, ClusterType.STORAGE_ROCKETMQ));
        assertEquals(0, untouched.queryCount);
        assertEquals(Map.of(Binding.of(remote), remote), untouched.remote);
    }

    @Test
    public void kafkaAclSnapshotRejectsMissingClusterWithoutChangingRemoteState() {
        FakeKafkaAclService service = new FakeKafkaAclService();
        AclMetadata remote = acl(2L, "User:existing", "existing", 3);
        service.remote.put(Binding.of(remote), remote);
        AclMetadata missing = acl(1L, "User:missing", "missing", 3);
        missing.setClusterId(999L);
        KafkaAclSnapshotHandler handler = new KafkaAclSnapshotHandler(ignored -> service);

        assertThrows(IllegalStateException.class,
            () -> handler.synchronize(List.of(missing), kafkaColony()));

        assertEquals(0, service.queryCount);
        assertEquals(Map.of(Binding.of(remote), remote), service.remote);

        AclMetadata managed = acl(3L, "User:managed", "managed", 3);
        ColonyDO<ClusterDO> topology = kafkaColony();
        handler.synchronize(List.of(managed), topology);
        topology.setAllColonyDO(new HashMap<>());
        assertThrows(IllegalStateException.class, () -> handler.synchronize(List.of(), topology));
        assertEquals(1, service.createCount);
        assertTrue("an owned Broker binding remains intact while its registered cluster is absent",
            service.remote.containsKey(Binding.of(managed)));
    }

    @Test
    public void kafkaAclSnapshotPreservesRemoteFailureAsRootCause() {
        FakeKafkaAclService service = new FakeKafkaAclService();
        RuntimeException remoteFailure = new RuntimeException("broker query failed");
        service.queryCode = 500;
        service.queryFailure = remoteFailure;
        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> new KafkaAclSnapshotHandler(ignored -> service).synchronize(List.of(acl(1L, "User:failed", "query", 3)),
                kafkaColony()));

        assertSame(remoteFailure, failure.getCause().getCause());
        assertEquals(0, service.createCount);
        assertEquals(0, service.deleteCount);
    }

    @Test
    public void kafkaAclSnapshotRejectsNullQueryResultWithoutChangingRemoteState() {
        FakeKafkaAclService service = new FakeKafkaAclService();
        AclMetadata remote = acl(2L, "User:existing", "existing", 3);
        service.remote.put(Binding.of(remote), remote);
        service.returnNullQueryResult = true;

        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> new KafkaAclSnapshotHandler(ignored -> service).synchronize(List.of(acl(1L, "User:desired", "desired", 3)),
                kafkaColony()));

        assertEquals("Kafka ACL synchronization failed for cluster 70", failure.getMessage());
        assertTrue(failure.getCause() instanceof IllegalStateException);
        assertEquals("Kafka ACL operation did not succeed", failure.getCause().getMessage());
        assertNull(failure.getCause().getCause());
        assertEquals(Map.of(Binding.of(remote), remote), service.remote);
        assertEquals(0, service.createCount);
        assertEquals(0, service.deleteCount);
    }

    private static void assertTopologyStillBoundTo(ColonyDO<ClusterDO> topology, long parentId) {
        assertTrue(topology.getAllColonyDO().get(parentId).getStorageColonyDOMap().containsKey(20L));
        assertEquals(Long.valueOf(parentId), topology.getAllColonyDO().get(20L).getSuperiorId());
        long otherParent = parentId == 10L ? 11L : 10L;
        assertFalse(topology.getAllColonyDO().get(otherParent).getStorageColonyDOMap().containsKey(20L));
    }

    private static ConnectionMetadata connection(long id, long clusterId, long runtimeId) {
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setId(id);
        metadata.setClusterId(clusterId);
        metadata.setRuntimeId(runtimeId);
        metadata.setStatus(1L);
        metadata.setTopic("topic-" + id);
        return metadata;
    }

    private static AclMetadata acl(long id, String principal, String resourceName, int operation) {
        AclMetadata metadata = new AclMetadata();
        metadata.setId(id);
        metadata.setClusterId(70L);
        metadata.setStatus(1L);
        metadata.setIsDelete(0);
        metadata.setPrincipal(principal);
        metadata.setHost("*");
        metadata.setResourceType("TOPIC");
        metadata.setResourceName(resourceName);
        metadata.setPatternType(3);
        metadata.setOperation(operation);
        metadata.setPermissionType("ALLOW");
        return metadata;
    }

    private static ColonyDO<ClusterDO> kafkaColony() {
        return topologyWithCluster(70L, ClusterType.STORAGE_KAFKA_RAFT);
    }

    private static ColonyDO<ClusterDO> topologyWithCluster(long id, ClusterType type) {
        ColonyDO<ClusterDO> colony = colony(id, type);
        colony.setAllColonyDO(Map.of(id, colony));
        return colony;
    }

    private static ClusterRelationshipMetadata relationship(long id, long parentId, long childId) {
        ClusterRelationshipMetadata metadata = new ClusterRelationshipMetadata();
        metadata.setId(id);
        metadata.setClusterId(parentId);
        metadata.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        metadata.setRelationshipId(childId);
        metadata.setRelationshipType(ClusterType.STORAGE_KAFKA_BROKER);
        metadata.setStatus(1L);
        metadata.setIsDelete(0);
        return metadata;
    }

    private static ColonyDO<ClusterDO> topology() {
        ColonyDO<ClusterDO> root = colony(10L, ClusterType.EVENTMESH_CLUSTER);
        ColonyDO<ClusterDO> secondParent = colony(11L, ClusterType.EVENTMESH_CLUSTER);
        ColonyDO<ClusterDO> child = colony(20L, ClusterType.STORAGE_KAFKA_BROKER);
        Map<Long, ColonyDO<ClusterDO>> all = new HashMap<>();
        all.put(10L, root);
        all.put(11L, secondParent);
        all.put(20L, child);
        root.setAllColonyDO(all);
        secondParent.setAllColonyDO(all);
        child.setAllColonyDO(all);
        return root;
    }

    private static ColonyDO<ClusterDO> colony(long id, ClusterType type) {
        ClusterMetadata metadata = new ClusterMetadata();
        metadata.setId(id);
        metadata.setClusterType(type);
        ClusterDO clusterDO = new ClusterDO();
        clusterDO.setClusterInfo(metadata);
        ColonyDO<ClusterDO> colony = new ColonyDO<>();
        colony.setClusterId(id);
        colony.setClusterType(type);
        colony.setClusterDO(clusterDO);
        return colony;
    }

    private static final class FakeKafkaAclService implements AclRemotingService {

        private final Map<Binding, AclMetadata> remote = new LinkedHashMap<>();

        private int createCount;

        private int deleteCount;

        private int queryCode = 200;

        private Throwable queryFailure;

        private boolean returnNullQueryResult;

        private int queryCount;

        private int failCreateCall = -1;

        private int failDeleteCall = -1;

        @Override
        public CreateAclResult createAcl(CreateAclRequest request) {
            this.createCount++;
            if (this.createCount == this.failCreateCall) {
                return result(new CreateAclResult(), 500);
            }
            this.remote.put(Binding.of(request.getMetaData()), request.getMetaData());
            return result(new CreateAclResult(), 200);
        }

        @Override
        public DeleteAclResult deleteAcl(DeleteAclRequest request) {
            this.deleteCount++;
            if (this.deleteCount == this.failDeleteCall) {
                return result(new DeleteAclResult(), 500);
            }
            this.remote.remove(Binding.of(request.getMetaData()));
            return result(new DeleteAclResult(), 200);
        }

        @Override
        public GetAclsResult getAllAcls(GetAcls2Request request) {
            this.queryCount++;
            if (this.returnNullQueryResult) {
                return null;
            }
            GetAclsResult result = new GetAclsResult();
            result.setCode(this.queryCode);
            result.setThrowable(this.queryFailure);
            result.setData(new ArrayList<>(this.remote.values()));
            return result;
        }

        private static <T extends org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult> T result(T result,
            int code) {
            result.setCode(code);
            return result;
        }
    }

    private record Binding(String principal, String host, Integer operation, String permission, String resourceType,
        String resourceName, Integer patternType) {

        private static Binding of(AclMetadata metadata) {
            return new Binding(metadata.getPrincipal(), metadata.getHost(), metadata.getOperation(),
                metadata.getPermissionType(), metadata.getResourceType(), metadata.getResourceName(), metadata.getPatternType());
        }
    }
}
