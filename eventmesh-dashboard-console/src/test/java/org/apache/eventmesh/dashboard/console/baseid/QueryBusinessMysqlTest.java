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
import org.apache.eventmesh.dashboard.console.domain.Impl.ClusterAndRuntimeDomainImpl;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.GetClusterInSyncReturnDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterTreeDO;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterRelationshipServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.RuntimeServiceImpl;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.ClusterTreeVO;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Exercises read assembly from MySQL-backed mapper queries through services into the domain. */
public class QueryBusinessMysqlTest {

    private BaseIdDatabaseTest database;
    private ClusterAndRuntimeDomainImpl domain;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Run with -Dbaseid.mysql=true for real MySQL acceptance", Boolean.getBoolean("baseid.mysql"));
        database = new BaseIdDatabaseTest();
        database.database();

        ClusterServiceImpl clusterService = new ClusterServiceImpl();
        ReflectionTestUtils.setField(clusterService, "clusterMapper", database.session.getMapper(ClusterMapper.class));
        RuntimeServiceImpl runtimeService = new RuntimeServiceImpl();
        ReflectionTestUtils.setField(runtimeService, "runtimeMapper", database.session.getMapper(RuntimeMapper.class));
        ClusterRelationshipServiceImpl relationshipService = new ClusterRelationshipServiceImpl();
        ReflectionTestUtils.setField(relationshipService, "clusterRelationshipMapper",
            database.session.getMapper(ClusterRelationshipMapper.class));

        domain = new ClusterAndRuntimeDomainImpl();
        ReflectionTestUtils.setField(domain, "clusterService", clusterService);
        ReflectionTestUtils.setField(domain, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(domain, "clusterRelationshipService", relationshipService);
    }

    @After
    public void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void deletedRootReturnsEmptyTreeAndSyncSelection() throws Exception {
        seedCluster(101, "deleted-root", ClusterType.EVENTMESH_CLUSTER, 1, 0);
        database.sql("update cluster set is_delete=1 where id=101");

        assertTrue(domain.queryClusterTree(treeQuery(101)).isEmpty());
        GetClusterInSyncReturnDO sync = domain.queryClusterInSync(cluster(101, ClusterType.EVENTMESH_CLUSTER), List.of());
        assertTrue(sync.getClusterEntityList().isEmpty());
        assertTrue(sync.getRuntimeEntityList().isEmpty());
    }

    @Test
    public void rootWithoutRelationsReturnsEmptyTreeAndSyncSelection() throws Exception {
        seedCluster(201, "unlinked-root", ClusterType.EVENTMESH_CLUSTER, 1, 0);

        assertTrue(domain.queryClusterTree(treeQuery(201)).isEmpty());
        GetClusterInSyncReturnDO sync = domain.queryClusterInSync(cluster(201, ClusterType.EVENTMESH_CLUSTER), List.of());
        assertTrue(sync.getClusterEntityList().isEmpty());
        assertTrue(sync.getRuntimeEntityList().isEmpty());
    }

    @Test
    public void inactiveAndDeletedRelationshipsDoNotCreateTreeChildren() throws Exception {
        seedCluster(251, "root", ClusterType.EVENTMESH_CLUSTER, 1, 0);
        seedCluster(252, "unlinked-child", ClusterType.EVENTMESH_JVM_CLUSTER, 1, 0);
        seedCluster(253, "deleted-link-child", ClusterType.EVENTMESH_JVM_CLUSTER, 1, 0);
        seedRelationship(451, 251, 252, ClusterType.EVENTMESH_CLUSTER, ClusterType.EVENTMESH_JVM_CLUSTER, 3, 0);
        seedRelationship(452, 251, 253, ClusterType.EVENTMESH_CLUSTER, ClusterType.EVENTMESH_JVM_CLUSTER, 1, 1);

        assertTrue(domain.queryClusterTree(treeQuery(251)).isEmpty());
        GetClusterInSyncReturnDO sync = domain.queryClusterInSync(cluster(251, ClusterType.EVENTMESH_CLUSTER), List.of());
        assertTrue(sync.getClusterEntityList().isEmpty());
        assertTrue(sync.getRuntimeEntityList().isEmpty());
    }

    @Test
    public void deletedChildIsOmittedWhileValidNeighborRemainsInTree() throws Exception {
        seedCluster(301, "root", ClusterType.EVENTMESH_CLUSTER, 1, 0);
        seedCluster(302, "deleted-child", ClusterType.EVENTMESH_JVM_CLUSTER, 1, 1);
        seedCluster(303, "valid-child", ClusterType.EVENTMESH_JVM_CLUSTER, 1, 0);
        seedRelationship(401, 301, 302, ClusterType.EVENTMESH_CLUSTER, ClusterType.EVENTMESH_JVM_CLUSTER);
        seedRelationship(402, 301, 303, ClusterType.EVENTMESH_CLUSTER, ClusterType.EVENTMESH_JVM_CLUSTER);

        List<ClusterTreeVO> children = domain.queryClusterTree(treeQuery(301));
        Map<String, ClusterTreeVO> byName = children.stream().collect(Collectors.toMap(ClusterTreeVO::getName, value -> value));
        assertEquals(List.of("valid-child"), byName.keySet().stream().toList());
        assertEquals(Long.valueOf(303), byName.get("valid-child").getId());
    }

    private void seedCluster(long id, String name, ClusterType type, int status, int isDelete) throws Exception {
        database.seed("cluster", ClusterEntity.class, id, Map.of("name", name, "cluster_type", type.name(),
            "status", status, "is_delete", isDelete));
    }

    private void seedRelationship(long id, long clusterId, long relationshipId, ClusterType clusterType,
        ClusterType relationshipType) throws Exception {
        this.seedRelationship(id, clusterId, relationshipId, clusterType, relationshipType, 1, 0);
    }

    private void seedRelationship(long id, long clusterId, long relationshipId, ClusterType clusterType,
        ClusterType relationshipType, int status, int isDelete) throws Exception {
        database.seed("cluster_relationship", ClusterRelationshipEntity.class, id, Map.of("cluster_id", clusterId,
            "relationship_id", relationshipId, "cluster_type", clusterType.name(),
            "relationship_type", relationshipType.name(), "status", status, "is_delete", isDelete));
    }

    private QueryClusterTreeDO treeQuery(long id) {
        QueryClusterTreeDO query = new QueryClusterTreeDO();
        query.setClusterId(id);
        return query;
    }

    private ClusterEntity cluster(long id, ClusterType type) {
        ClusterEntity query = new ClusterEntity();
        query.setId(id);
        query.setClusterType(type);
        return query;
    }
}
