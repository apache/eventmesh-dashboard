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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.MetadataAllDO;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeDO;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class RefactoringLifecycleTest {

    @Test
    public void logicallyDeletedActiveClusterIsUnregisteredAndRepeatedRemovalIsSafe() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        ClusterEntity deleted = cluster(type);
        deleted.setStatus(1L);
        deleted.setIsDelete(1);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setClusterEntityAndDefinitionCluster(List.of(deleted, deleted), List.of(), new HashMap<>());

        assertNull(domain.getColonyDO(10L));
        verify(callbacks).unRegisterCluster(eq(deleted), any(), any());
        verify(callbacks, never()).registerCluster(any(), any(), any());
    }

    @Test
    public void stoppedClusterIsRemovedEvenWhenItIsNotLogicallyDeleted() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        ClusterEntity stopped = cluster(type);
        stopped.setStatus(0L);
        stopped.setIsDelete(0);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setClusterEntityAndDefinitionCluster(List.of(stopped), List.of(), new HashMap<>());

        assertNull(domain.getColonyDO(10L));
        verify(callbacks).unRegisterCluster(eq(stopped), any(), any());
    }

    @Test
    public void activeClusterIsRetainedWhenNeitherStoppedNorLogicallyDeleted() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        ClusterEntity active = cluster(type);
        active.setStatus(1L);
        active.setIsDelete(0);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setClusterEntityAndDefinitionCluster(List.of(active), List.of(), new HashMap<>());

        assertNotNull(domain.getColonyDO(10L));
        verify(callbacks, never()).unRegisterCluster(any(), any(), any());
    }

    @Test
    public void logicallyDeletedRuntimeIsUnregisteredWhileBusinessStatusIsActive() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        RuntimeEntity deleted = runtime(type);
        deleted.setStatus(1L);
        deleted.setIsDelete(1);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setRuntimeEntity(List.of(deleted), new HashMap<>());

        verify(callbacks).unRegisterRuntime(eq(deleted), isNull(), eq(domain.getColonyDO(10L)));
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().isEmpty());
    }

    @Test
    public void activeRuntimeIsRegisteredAndKeptInTheCluster() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        RuntimeEntity active = runtime(type);
        active.setStatus(1L);
        active.setIsDelete(0);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setRuntimeEntity(List.of(active), new HashMap<>());

        verify(callbacks).registerRuntime(eq(active), any(), eq(domain.getColonyDO(10L)));
        verify(callbacks, never()).unRegisterRuntime(any(), any(), any());
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().containsKey(11L));
    }

    @Test
    public void stoppedRuntimeIsUnregisteredWhenItIsNotLogicallyDeleted() {
        ClusterType type = ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE;
        ClusterMetadataDomain domain = domain(type);
        RuntimeEntity stopped = runtime(type);
        stopped.setStatus(0L);
        stopped.setIsDelete(0);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.setRuntimeEntity(List.of(stopped), new HashMap<>());

        verify(callbacks).unRegisterRuntime(eq(stopped), isNull(), eq(domain.getColonyDO(10L)));
        verify(callbacks, never()).registerRuntime(any(), any(), any());
        assertTrue(domain.getColonyDO(10L).getClusterDO().getRuntimeMap().isEmpty());
    }

    @Test
    public void capRegistrationSuppliesTheCurrentClusterEntity() {
        ClusterType type = ClusterType.STORAGE_KAFKA_BROKER;
        ClusterMetadataDomain domain = domain(type);
        var callbacks = mock(ClusterMetadataDomain.DataHandler.class);
        domain.setHandler(callbacks);

        domain.handlerMetadata(MetadataAllDO.builder().runtimeEntityList(List.of(runtime(type))).build());

        verify(callbacks).registerCluster(argThat((ClusterEntity entity) -> entity != null && entity.getId().equals(10L)), any(), any());
    }

    private static ClusterMetadataDomain domain(ClusterType type) {
        ClusterMetadataDomain domain = new ClusterMetadataDomain();
        domain.rootClusterDHO();
        ClusterMetadata metadata = new ClusterMetadata();
        metadata.setId(10L);
        metadata.setStatus(1L);
        metadata.setClusterType(type);
        ClusterDO cluster = new ClusterDO();
        cluster.setClusterInfo(metadata);
        AbstractMultiCreateSDKConfig config = new AbstractMultiCreateSDKConfig();
        config.addNetAddress(NetAddress.create("localhost", 9092));
        cluster.setMultiCreateSDKConfig(config);
        RuntimeDO runtime = new RuntimeDO();
        RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
        runtimeMetadata.setId(11L);
        runtimeMetadata.setClusterId(10L);
        runtimeMetadata.setHost("localhost");
        runtimeMetadata.setPort(9092);
        runtimeMetadata.setClusterType(type);
        runtime.setRuntimeMetadata(runtimeMetadata);
        cluster.getRuntimeMap().put(11L, runtime);
        domain.getColonyDO().register(10L, type, cluster);
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
