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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionConfig;
import org.apache.eventmesh.dashboard.console.spring.support.FunctionManage;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class MetadataPipelineContractTest {

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

}
