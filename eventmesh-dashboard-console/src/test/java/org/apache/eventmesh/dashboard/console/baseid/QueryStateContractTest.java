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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.domain.Impl.ClusterAndRuntimeDomainImpl;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterTreeDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.GetClusterInSyncReturnDO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.ClusterTreeVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.List;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QueryStateContractTest {

    @Test
    public void missingRootReturnsEmptyReadModel() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService, runtimeService);
        ClusterEntity request = cluster(101L);
        when(clusterService.queryClusterById(request)).thenReturn(null);

        ClusterAndRuntimeOfRelationshipDO result = domain.getAllClusterAndRuntimeByCluster(request, null);

        assertNotNull(result);
        assertTrue(result.getClusterEntityList().isEmpty());
        assertTrue(result.getRuntimeEntityList().isEmpty());
        assertTrue(result.getClusterRelationshipEntityList().isEmpty());
        verify(relationshipService, never()).queryListByClusterIdAndType(any());
    }

    @Test
    public void missingRootReturnsEmptySyncReadModel() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService);
        ClusterEntity request = cluster(105L);
        when(clusterService.queryClusterById(request)).thenReturn(null);

        GetClusterInSyncReturnDO result = domain.queryClusterInSync(request, List.of(ClusterType.STORAGE_ROCKETMQ_CLUSTER));

        assertNotNull(result);
        assertTrue(result.getClusterEntityList().isEmpty());
        assertTrue(result.getRuntimeEntityList().isEmpty());
        verify(relationshipService, never()).queryListByClusterIdAndType(any());
    }

    @Test
    public void runtimeRootKeepsItsRuntimeInSyncReadModel() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService, runtimeService);
        ClusterEntity root = cluster(117L);
        root.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        RuntimeEntity runtime = new RuntimeEntity();
        runtime.setId(118L);
        runtime.setClusterId(root.getId());
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(runtimeService.queryRuntimeToFrontByClusterId(any())).thenReturn(List.of(runtime));

        GetClusterInSyncReturnDO result = domain.queryClusterInSync(root, List.of());

        assertEquals(1, result.getRuntimeEntityList().size());
        assertEquals(runtime.getId(), result.getRuntimeEntityList().get(0).getId());
    }

    @Test
    public void emptyRelationshipFrontierDoesNotIssueEmptyIdQuery() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService);
        ClusterEntity root = cluster(102L);
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(relationshipService.queryListByClusterIdAndType(any())).thenReturn(List.of());

        ClusterAndRuntimeOfRelationshipDO result = domain.getAllClusterAndRuntimeByCluster(root, null);

        assertTrue(result.getClusterEntityList().isEmpty());
        assertTrue(result.getRuntimeEntityList().isEmpty());
        assertTrue(result.getClusterRelationshipEntityList().isEmpty());
        verify(relationshipService, never()).queryListByClusterIdListAndType(any());
        verify(clusterService, never()).queryClusterListByClusterList(any());
    }

    @Test
    public void hiddenRelationshipTargetIsExcludedFromTree() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService);
        ClusterEntity root = cluster(103L);
        ClusterRelationshipEntity relation = new ClusterRelationshipEntity();
        relation.setClusterId(root.getId());
        relation.setRelationshipId(104L);
        relation.setRelationshipType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(relationshipService.queryListByClusterIdAndType(any())).thenReturn(List.of(relation));
        when(relationshipService.queryListByClusterIdListAndType(any())).thenReturn(List.of());
        when(clusterService.queryClusterListByClusterList(any())).thenReturn(List.of());

        QueryClusterTreeDO request = new QueryClusterTreeDO();
        request.setClusterId(root.getId());
        List<?> tree = domain.queryClusterTree(request);

        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    @Test
    public void visibleRelationshipTargetAndRuntimeRemainInTree() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService, runtimeService);
        ClusterEntity root = cluster(106L);
        ClusterEntity child = cluster(107L);
        ClusterRelationshipEntity relation = relationship(root, child);
        RuntimeEntity runtime = new RuntimeEntity();
        runtime.setClusterId(child.getId());
        runtime.setId(108L);
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(relationshipService.queryListByClusterIdAndType(any())).thenReturn(List.of(relation));
        when(relationshipService.queryListByClusterIdListAndType(any())).thenReturn(List.of());
        when(clusterService.queryClusterListByClusterList(any())).thenReturn(List.of(child));
        when(runtimeService.queryRuntimeToFrontByClusterIdList(any())).thenReturn(List.of());
        when(runtimeService.queryRuntimeToFrontByClusterIdList(any())).thenReturn(List.of(runtime));

        QueryClusterTreeDO request = new QueryClusterTreeDO();
        request.setClusterId(root.getId());
        List<ClusterTreeVO> tree = domain.queryClusterTree(request);

        assertEquals(1, tree.size());
        assertEquals(child.getId(), tree.get(0).getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(runtime.getId(), tree.get(0).getChildren().get(0).getId());
    }

    @Test
    public void createAssemblyDropsRelationWithHiddenEndpoint() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService);
        ClusterEntity root = cluster(109L);
        ClusterRelationshipEntity relation = relationship(root, cluster(110L));
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(relationshipService.queryListByClusterIdAndType(any())).thenReturn(List.of(relation));
        when(relationshipService.queryListByClusterIdListAndType(any())).thenReturn(List.of());
        when(clusterService.queryClusterListByClusterList(any())).thenReturn(List.of());

        ClusterAndRuntimeOfRelationshipDO result =
            domain.getAllClusterAndRuntimeByCluster(root, DeployStatusType.CREATE);

        assertTrue(result.getClusterRelationshipTripleList().isEmpty());
    }

    @Test
    public void createAssemblyDropsDeletedBranchAndKeepsValidNeighbor() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterRelationshipService relationshipService = mock(ClusterRelationshipService.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ClusterAndRuntimeDomainImpl domain = domain(clusterService, relationshipService, runtimeService);
        ClusterEntity root = cluster(113L);
        ClusterEntity deletedChild = cluster(114L);
        ClusterEntity validChild = cluster(115L);
        ClusterEntity descendant = cluster(116L);
        ClusterRelationshipEntity deletedEdge = relationship(root, deletedChild);
        ClusterRelationshipEntity valid = relationship(root, validChild);
        ClusterRelationshipEntity descendantEdge = relationship(deletedChild, descendant);
        when(clusterService.queryClusterById(root)).thenReturn(root);
        when(relationshipService.queryListByClusterIdAndType(any())).thenReturn(List.of(deletedEdge, valid));
        when(relationshipService.queryListByClusterIdListAndType(any()))
            .thenReturn(List.of(descendantEdge), List.of());
        when(clusterService.queryClusterListByClusterList(any())).thenReturn(List.of(validChild, descendant));
        when(runtimeService.queryRuntimeToFrontByClusterIdList(any())).thenReturn(List.of());

        ClusterAndRuntimeOfRelationshipDO result =
            domain.getAllClusterAndRuntimeByCluster(root, DeployStatusType.CREATE);

        assertEquals(1, result.getClusterRelationshipTripleList().size());
        assertEquals(valid, result.getClusterRelationshipTripleList().get(0).getLeft());
    }

    @Test
    public void treeBuilderDropsRelationshipToMissingCluster() {
        ClusterAndRuntimeDomainImpl domain = new ClusterAndRuntimeDomainImpl();
        ClusterAndRuntimeOfRelationshipDO data = new ClusterAndRuntimeOfRelationshipDO();
        data.setClusterEntity(cluster(111L));
        data.setClusterEntityList(List.of());
        data.setRuntimeEntityList(List.of());
        ClusterRelationshipEntity relation = new ClusterRelationshipEntity();
        relation.setClusterId(111L);
        relation.setRelationshipId(112L);
        data.setClusterRelationshipEntityList(List.of(relation));

        assertTrue(domain.queryClusterTree(data).isEmpty());
    }

    private ClusterAndRuntimeDomainImpl domain(ClusterService clusterService,
                                               ClusterRelationshipService relationshipService) {
        return domain(clusterService, relationshipService, mock(RuntimeService.class));
    }

    private ClusterAndRuntimeDomainImpl domain(ClusterService clusterService,
                                               ClusterRelationshipService relationshipService,
                                               RuntimeService runtimeService) {
        ClusterAndRuntimeDomainImpl domain = new ClusterAndRuntimeDomainImpl();
        ReflectionTestUtils.setField(domain, "clusterService", clusterService);
        ReflectionTestUtils.setField(domain, "clusterRelationshipService", relationshipService);
        ReflectionTestUtils.setField(domain, "runtimeService", runtimeService);
        return domain;
    }

    private ClusterRelationshipEntity relationship(ClusterEntity root, ClusterEntity child) {
        ClusterRelationshipEntity relationship = new ClusterRelationshipEntity();
        relationship.setClusterId(root.getId());
        relationship.setRelationshipId(child.getId());
        relationship.setRelationshipType(child.getClusterType());
        return relationship;
    }

    private ClusterEntity cluster(Long id) {
        ClusterEntity cluster = new ClusterEntity();
        cluster.setId(id);
        cluster.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        return cluster;
    }
}
