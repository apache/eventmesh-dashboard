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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.MetadataAllDO;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.spring.support.DefaultDataHandler;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeDO;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class RefactoringLifecycleTest {

    @Test
    public void failedRuntimeUnregisterRetainsGraphUntilRetryAndIsIdempotent() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        RuntimeEntity runtime = runtime(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        runtime.setStatus(0L);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);
        doThrow(new IllegalStateException("retry")).doNothing().when(callbacks).unRegisterRuntime(eq(runtime), any(), any());
        assertThrows(IllegalStateException.class, () -> domain.setRuntimeEntity(List.of(runtime), new HashMap<>()));
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().containsKey(11L));
        domain.setRuntimeEntity(List.of(runtime), new HashMap<>());
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().isEmpty());
        domain.setRuntimeEntity(List.of(runtime), new HashMap<>());
        verify(callbacks, times(3)).unRegisterRuntime(eq(runtime), any(), any());
    }

    @Test
    public void clusterLogicalDeletionRetriesChildrenThenUnlinksReferences() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ColonyDO<ClusterDO> child = domain.getColonyDO(10L);
        domain.getColonyDO().getRuntimeColonyDOMap().put(10L, child);
        // A peer's relationship to the removed cluster must disappear too.
        ClusterDO peerData = new ClusterDO();
        var peer = domain.getColonyDO().register(20L, ClusterType.STORAGE_ROCKETMQ_CLUSTER, peerData);
        peer.setSuperiorId(10L);
        peer.getRuntimeColonyDOMap().put(10L, child);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);
        ClusterEntity entity = cluster(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        entity.setIsDelete(1);
        doThrow(new IllegalStateException("retry")).doNothing().when(callbacks).unRegisterCluster(eq(entity), any(), any());
        assertThrows(IllegalStateException.class, () -> domain.setClusterEntityAndDefinitionCluster(List.of(entity), List.of(), new HashMap<>()));
        assertSame(child, domain.getColonyDO(10L));
        domain.setClusterEntityAndDefinitionCluster(List.of(entity), List.of(), new HashMap<>());
        assertNull(domain.getColonyDO(10L));
        assertFalse(peer.getRuntimeColonyDOMap().containsKey(10L));
        assertNull(peer.getSuperiorId());
        domain.setClusterEntityAndDefinitionCluster(List.of(entity), List.of(), new HashMap<>());
        verify(callbacks, times(2)).unRegisterCluster(eq(entity), any(), any());
        verify(callbacks, times(2)).unRegisterRuntime(any(), any(), eq(child));
    }

    @Test
    public void stoppedClusterStatusUnregistersAndDetachesEvenWhenNotLogicallyDeleted() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ColonyDO<ClusterDO> child = domain.getColonyDO(10L);
        ClusterDO peerData = new ClusterDO();
        var peer = domain.getColonyDO().register(20L, ClusterType.STORAGE_KAFKA_BROKER, peerData);
        peer.getRuntimeColonyDOMap().put(10L, child);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        ClusterEntity stopped = cluster(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        stopped.setStatus(0L);
        stopped.setIsDelete(0);
        domain.setClusterEntityAndDefinitionCluster(List.of(stopped), List.of(), new HashMap<>());

        verify(callbacks).unRegisterCluster(eq(stopped), any(), any());
        assertNull(domain.getColonyDO(10L));
        assertFalse(peer.getRuntimeColonyDOMap().containsKey(10L));
    }

    @Test
    public void stoppedEndpointRetainsRelationshipAndRebindsWhenClusterReturns() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        domain.getColonyDO().register(20L, ClusterType.STORAGE_KAFKA_BROKER, new ClusterDO());
        ClusterRelationshipEntity relation = new ClusterRelationshipEntity();
        relation.setId(30L);
        relation.setClusterId(20L);
        relation.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        relation.setRelationshipId(10L);
        relation.setRelationshipType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        relation.setStatus(1L);

        ClusterEntity stopped = cluster(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        stopped.setStatus(0L);
        stopped.setIsDelete(0);
        domain.setClusterEntityAndDefinitionCluster(List.of(stopped), List.of(relation), new HashMap<>());
        assertNull(domain.getColonyDO(10L));

        RuntimeEntity staleChild = runtime(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        domain.setRuntimeEntity(List.of(staleChild), new HashMap<>());
        assertNull(domain.getColonyDO(10L));

        ClusterEntity restarted = cluster(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        domain.setClusterEntityAndDefinitionCluster(List.of(restarted), List.of(), new HashMap<>());
        ColonyDO<ClusterDO> restored = domain.getColonyDO(10L);
        assertNotNull(restored);
        assertSame(restored, domain.getColonyDO(20L).getRuntimeColonyDOMap().get(10L));
        assertEquals(Long.valueOf(20L), restored.getSuperiorId());
    }

    @Test
    public void refreshingClusterPreservesExistingRuntimeAndConfig() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ColonyDO<ClusterDO> existing = domain.getColonyDO(10L);
        RuntimeDO runtime = existing.getClusterDO().getRuntimeMap().get(11L);
        AbstractMultiCreateSDKConfig config = existing.getClusterDO().getMultiCreateSDKConfig();

        ClusterEntity refresh = cluster(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        refresh.setConfig("updated-config");
        domain.setClusterEntityAndDefinitionCluster(List.of(refresh), List.of(), new HashMap<>());

        ColonyDO<ClusterDO> updated = domain.getColonyDO(10L);
        assertSame(runtime, updated.getClusterDO().getRuntimeMap().get(11L));
        assertSame(config, updated.getClusterDO().getMultiCreateSDKConfig());
        assertEquals("localhost:9092", config.getNetAddresses()[0]);
    }

    @Test
    public void runtimeLogicalDeletionUnregistersWhileBusinessStatusRemainsActive() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        RuntimeEntity deleted = runtime(type);
        deleted.setStatus(1L);
        deleted.setIsDelete(1);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setRuntimeEntity(List.of(deleted), new HashMap<>());

        verify(callbacks).unRegisterRuntime(eq(deleted), isNull(), eq(domain.getColonyDO(10L)));
        verify(callbacks, never()).unRegisterCluster(any(), any(), any());
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().isEmpty());
    }

    @Test
    public void lastCapAddressUnregistersAndRetainsPendingCleanupOnFailure() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_KAFKA_BROKER);
        RuntimeEntity runtime = runtime(ClusterType.STORAGE_KAFKA_BROKER);
        runtime.setIsDelete(1);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);
        doThrow(new IllegalStateException("retry")).doNothing().when(callbacks).unRegisterCluster(any(), any(), any());
        MetadataAllDO batch = MetadataAllDO.builder().runtimeEntityList(List.of(runtime)).build();
        assertThrows(IllegalStateException.class, () -> domain.handlerMetadata(batch));
        assertEquals(0, domain.getColonyDO(10L).getClusterDO().getMultiCreateSDKConfig().getNetAddresses().length);
        domain.handlerMetadata(MetadataAllDO.builder().build());
        verify(callbacks, times(2)).unRegisterCluster(argThat((ClusterEntity e) -> e.getId().equals(10L)), any(), any());
        verify(callbacks, never()).registerCluster(any(), any(), any());
    }

    @Test
    public void capRegistrationSuppliesEntityAndRepeatedAddressDoesNotDuplicate() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_KAFKA_BROKER);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);
        MetadataAllDO batch = MetadataAllDO.builder().runtimeEntityList(List.of(runtime(ClusterType.STORAGE_KAFKA_BROKER))).build();
        domain.handlerMetadata(batch);
        domain.handlerMetadata(batch);
        verify(callbacks, times(2)).registerCluster(argThat((ClusterEntity e) -> e.getId().equals(10L)), any(), any());
        assertEquals(1, domain.getColonyDO(10L).getClusterDO().getMultiCreateSDKConfig().getNetAddresses().length);
    }

    @Test
    public void capAddressChangeReplacesOldAddressAndDeletionUsesRegisteredAddress() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_KAFKA_BROKER);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);
        RuntimeEntity changed = runtime(ClusterType.STORAGE_KAFKA_BROKER);
        changed.setPort(9093);
        domain.handlerMetadata(MetadataAllDO.builder().runtimeEntityList(List.of(changed)).build());
        String[] addresses = domain.getColonyDO(10L).getClusterDO().getMultiCreateSDKConfig().getNetAddresses();
        assertEquals(1, addresses.length);
        assertEquals("localhost:9093", addresses[0]);
        changed.setPort(9094);
        changed.setStatus(0L);
        domain.handlerMetadata(MetadataAllDO.builder().runtimeEntityList(List.of(changed)).build());
        verify(callbacks).unRegisterCluster(any(), any(), any());
        assertTrue(domain.getColonyDO(10L).getClusterDO().getMultiCreateSDKConfig().isNullAddress());
    }

    @Test
    public void defaultClusterCleanupRemovesSyncAndBothHealthRegistrations() {
        ClusterMetadataDomain domain = domain(ClusterType.STORAGE_KAFKA_BROKER);
        DefaultDataHandler handler = new DefaultDataHandler();
        Health2Service health = mock(Health2Service.class);
        MetadataSyncManage sync = mock(MetadataSyncManage.class);
        handler.setHealthService(health);
        handler.setMetadataSyncManage(sync);
        ClusterDO data = domain.getColonyDO(10L).getClusterDO();
        handler.unRegisterCluster(cluster(ClusterType.STORAGE_KAFKA_BROKER), data, domain.getColonyDO(10L));
        verify(sync).unRegister(data.getClusterInfo());
        verify(health).unRegister(data.getClusterInfo());
        verify(health).unRegisterCluster(10L);
    }

    private static ClusterMetadataDomain domain(ClusterType type) {
        ClusterMetadataDomain domain = new ClusterMetadataDomain();
        domain.rootClusterDHO();
        ClusterMetadata metadata = new ClusterMetadata();
        metadata.setId(10L);
        metadata.setStatus(1L);
        metadata.setClusterType(type);
        ClusterDO data = new ClusterDO();
        data.setClusterInfo(metadata);
        AbstractMultiCreateSDKConfig config = new AbstractMultiCreateSDKConfig();
        config.addNetAddress(NetAddress.create("localhost", 9092));
        data.setMultiCreateSDKConfig(config);
        RuntimeDO node = new RuntimeDO();
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(11L);
        runtime.setClusterId(10L);
        runtime.setHost("localhost");
        runtime.setPort(9092);
        runtime.setClusterType(type);
        node.setRuntimeMetadata(runtime);
        data.getRuntimeMap().put(11L, node);
        domain.getColonyDO().register(10L, type, data);
        return domain;
    }

    private static ClusterEntity cluster(ClusterType type) {
        ClusterEntity entity = new ClusterEntity();
        entity.setId(10L);
        entity.setStatus(1L);
        entity.setClusterType(type);
        return entity;
    }

    private static RuntimeEntity runtime(ClusterType type) {
        RuntimeEntity entity = new RuntimeEntity();
        entity.setId(11L);
        entity.setClusterId(10L);
        entity.setClusterType(type);
        entity.setStatus(1L);
        entity.setHost("localhost");
        entity.setPort(9092);
        return entity;
    }
}
