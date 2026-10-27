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

import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.domain.Impl.ClusterAndRuntimeDomainImpl;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryRelationClusterByClusterIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterRelationshipServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.RuntimeServiceImpl;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 使用已有 MySQL 数据，只读验证集群身份及查询组装链路。 */
public class ClusterIdentityMysqlQueryTest {

    private SqlSession session;
    private ClusterServiceImpl clusterService;
    private ClusterAndRuntimeDomainImpl domain;

    @Before
    public void connectReadOnlyDatabase() throws Exception {
        String url = System.getenv("CLUSTER_QUERY_MYSQL_URL");
        Assert.assertNotNull("需要设置 CLUSTER_QUERY_MYSQL_URL", url);
        UnpooledDataSource source = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getenv("CLUSTER_QUERY_MYSQL_USER"), System.getenv("CLUSTER_QUERY_MYSQL_PASSWORD"));
        Configuration configuration = new Configuration(new Environment("mysql-query", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(ClusterMapper.class);
        configuration.addMapper(RuntimeMapper.class);
        configuration.addMapper(ClusterRelationshipMapper.class);
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(false);
        session.getConnection().setReadOnly(true);
        clusterService = new ClusterServiceImpl();
        ReflectionTestUtils.setField(clusterService, "clusterMapper", session.getMapper(ClusterMapper.class));
        RuntimeServiceImpl runtimes = new RuntimeServiceImpl();
        ReflectionTestUtils.setField(runtimes, "runtimeMapper", session.getMapper(RuntimeMapper.class));
        ClusterRelationshipServiceImpl relationships = new ClusterRelationshipServiceImpl();
        ReflectionTestUtils.setField(relationships, "clusterRelationshipMapper", session.getMapper(ClusterRelationshipMapper.class));
        domain = new ClusterAndRuntimeDomainImpl();
        ReflectionTestUtils.setField(domain, "clusterService", clusterService);
        ReflectionTestUtils.setField(domain, "runtimeService", runtimes);
        ReflectionTestUtils.setField(domain, "clusterRelationshipService", relationships);
    }

    @After
    public void closeReadOnlyDatabase() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    public void existingClustersRoundTripThroughQueryAndMetadata() throws Exception {
        List<ClusterEntity> clusters = session.getMapper(ClusterMapper.class).queryAllCluster();
        Assert.assertFalse("实际库需要已有集群数据", clusters.isEmpty());
        Assert.assertEquals(queryIds("select id from cluster where status=1 and is_delete=0"),
            clusters.stream().map(ClusterEntity::getId).collect(Collectors.toSet()));
        for (ClusterEntity cluster : clusters) {
            ClusterRequestDTO dto = new ClusterRequestDTO();
            dto.setId(cluster.getId());
            ClusterEntity result = clusterService.queryClusterById(ClusterControllerMapper.INSTANCE.toClusterEntity(dto));
            Assert.assertEquals(cluster.getId(), result.getId());
            Assert.assertEquals(cluster.getName(), result.getName());
            ClusterMetadata metadata = ClusterConvertMetaData.INSTANCE.toMetaData(result);
            Assert.assertEquals(result.getId().toString(), metadata.nodeUnique());
            Assert.assertEquals("ClusterMetadata-" + result.getId(), metadata.getUnique());
            Assert.assertTrue(metadata.isCluster());
            ClusterEntity restored = ClusterConvertMetaData.INSTANCE.toEntity(metadata);
            Assert.assertEquals(result.getId(), restored.getId());
            Assert.assertEquals(result.getClusterType(), restored.getClusterType());
            Assert.assertEquals(result.getTrusteeshipType(), restored.getTrusteeshipType());
            assertMatchesDatabaseRow("cluster", metadata);
            ObjectMapper json = new ObjectMapper().findAndRegisterModules();
            Assert.assertFalse(json.valueToTree(result).has("clusterId"));
            Assert.assertFalse(json.valueToTree(metadata).has("clusterId"));
        }
        System.out.println("MySQL 实际查询通过：" + clusters.size() + " 个集群，DTO → Service → MyBatis → Entity → Metadata 往返");
    }

    @Test
    public void existingRelationsAndRuntimesMatchSqlResults() throws Exception {
        long rootId = Long.parseLong(System.getenv("CLUSTER_QUERY_ROOT_ID"));
        QueryRelationClusterByClusterIdAndTypeDTO dto = new QueryRelationClusterByClusterIdAndTypeDTO();
        dto.setId(rootId);
        Set<Long> expectedDirect = queryIds("select relationship_id from cluster_relationship where status=1 and is_delete=0 and cluster_id=" + rootId);
        Set<Long> actualDirect = clusterService.queryRelationClusterByClusterIdAndType(
            ClusterControllerMapper.INSTANCE.queryRelationClusterByClusterIdAndType(dto)).stream()
            .map(ClusterEntity::getId).collect(Collectors.toSet());
        Assert.assertFalse(expectedDirect.isEmpty());
        Assert.assertEquals(expectedDirect, actualDirect);
        ClusterEntity query = ClusterControllerMapper.INSTANCE.toClusterEntity(dto);
        ClusterAndRuntimeOfRelationshipDO result = domain.getAllClusterAndRuntimeByCluster(query, DeployStatusType.CREATE);
        Assert.assertFalse(result.getClusterRelationshipTripleList().isEmpty());
        result.getClusterRelationshipTripleList().forEach(triple -> {
            Assert.assertEquals(triple.getLeft().getClusterId(), triple.getMiddle().getId());
            Assert.assertEquals(triple.getLeft().getRelationshipId(), triple.getRight().getId());
        });
        for (var pair : result.getClusterEntityPairleList()) {
            Assert.assertEquals(queryIds("select relationship_id from cluster_relationship where status=1 and is_delete=0 and cluster_id=" + pair.getLeft().getId()),
                pair.getRight().stream().map(ClusterEntity::getId).collect(Collectors.toSet()));
        }
        Set<Long> expectedRuntimeIds = new HashSet<>();
        for (ClusterEntity child : result.getClusterEntityList()) {
            expectedRuntimeIds.addAll(queryIds("select id from runtime where is_delete=0 and cluster_id=" + child.getId()));
        }
        Assert.assertFalse(expectedRuntimeIds.isEmpty());
        Assert.assertEquals(expectedRuntimeIds, result.getRuntimeEntityList().stream().map(RuntimeEntity::getId).collect(Collectors.toSet()));
        for (var type : result.getRuntimeEntityList().stream().map(RuntimeEntity::getClusterType).collect(Collectors.toSet())) {
            for (var group : result.getRuntimeEntityByClusterType(type).entrySet()) {
                Set<Long> expected = queryIds("select id from runtime where is_delete=0 and cluster_id=" + group.getKey()
                    + " and cluster_type='" + type.name() + "'");
                Assert.assertEquals(expected, group.getValue().stream().map(RuntimeEntity::getId).collect(Collectors.toSet()));
            }
        }
        for (var pair : result.getRuntimeEntityPairList()) {
            Assert.assertEquals(queryIds("select id from runtime where is_delete=0 and cluster_id=" + pair.getLeft().getId()),
                pair.getRight().stream().map(RuntimeEntity::getId).collect(Collectors.toSet()));
        }
        ClusterMetadataDomain topology = new ClusterMetadataDomain();
        topology.setMainCluster(result.getClusterEntity());
        Assert.assertEquals(Long.valueOf(rootId), topology.getColonyDO().getClusterId());
        System.out.println("MySQL 关系查询通过：根集群 " + rootId + "，关系 " + result.getClusterRelationshipTripleList().size()
            + " 条，节点 " + expectedRuntimeIds.size() + " 个，主集群拓扑及父子编号匹配");
    }

    @Test
    public void existingRuntimeMetadataKeepsOwnIdAndParentReference() throws Exception {
        Set<Long> visited = new HashSet<>();
        int distinctIdentityCount = 0;
        for (Long clusterId : queryIds("select distinct cluster_id from runtime where is_delete=0")) {
            RuntimeEntity query = new RuntimeEntity();
            query.setClusterId(clusterId);
            List<RuntimeEntity> runtimes = session.getMapper(RuntimeMapper.class).getRuntimesToFrontByCluster(query);
            Map<Long, RuntimeEntity> byId = runtimes.stream().collect(Collectors.toMap(RuntimeEntity::getId, value -> value));
            Assert.assertEquals(queryIds("select id from runtime where is_delete=0 and cluster_id=" + clusterId), byId.keySet());
            for (RuntimeEntity entity : runtimes) {
                Assert.assertTrue("Each runtime belongs to exactly one selected cluster", visited.add(entity.getId()));
                RuntimeMetadata metadata = RuntimeConvertMetaData.INSTANCE.toMetaData(entity);
                Assert.assertEquals(entity.getId(), metadata.getId());
                Assert.assertEquals(clusterId, metadata.getClusterId());
                Assert.assertEquals("RuntimeMetadata-" + entity.getId(), metadata.getUnique());
                Assert.assertEquals("ClusterMetadata-" + clusterId, metadata.clusterUnique());
                Assert.assertFalse(metadata.isCluster());
                assertMatchesDatabaseRow("runtime", metadata);
                Assert.assertEquals(clusterId.longValue(),
                    new ObjectMapper().findAndRegisterModules().valueToTree(metadata).get("clusterId").asLong());
                RuntimeEntity restored = RuntimeConvertMetaData.INSTANCE.toEntity(metadata);
                Assert.assertEquals(entity.getId(), restored.getId());
                Assert.assertEquals(clusterId, restored.getClusterId());
                Assert.assertEquals(entity.getClusterType(), restored.getClusterType());
                Assert.assertEquals(entity.getTrusteeshipType(), restored.getTrusteeshipType());
                Assert.assertEquals(entity.getStatus(), restored.getStatus());
                if (!entity.getId().equals(clusterId)) {
                    distinctIdentityCount++;
                }
            }
        }
        Assert.assertFalse(visited.isEmpty());
        Assert.assertTrue("Require real rows whose own id differs from parent id", distinctIdentityCount > 0);
        Assert.assertEquals(queryIds("select id from runtime where is_delete=0"), visited);
        System.out.println("MySQL Metadata 验证通过：" + visited.size() + " 个 Runtime，"
            + distinctIdentityCount + " 个自身 id 与所属 clusterId 不同，类型、托管及状态字段与原始 SQL 一致");
    }

    private void assertMatchesDatabaseRow(String table, BaseSyncBase metadata) throws Exception {
        Assert.assertTrue(table.equals("cluster") || table.equals("runtime"));
        try (PreparedStatement statement = session.getConnection().prepareStatement("select * from " + table + " where id=?")) {
            statement.setLong(1, metadata.getId());
            try (ResultSet row = statement.executeQuery()) {
                Assert.assertTrue(row.next());
                Assert.assertEquals(row.getLong("id"), metadata.getId().longValue());
                Assert.assertEquals(row.getLong("organization_id"), metadata.getOrganizationId().longValue());
                Assert.assertEquals(row.getString("cluster_type"), metadata.getClusterType().name());
                Assert.assertEquals(row.getString("trusteeship_type"), metadata.getTrusteeshipType().name());
                Assert.assertEquals(row.getString("first_to_whom"), metadata.getFirstToWhom().name());
                Assert.assertEquals(row.getString("first_sync_state"), metadata.getFirstSyncState().name());
                Assert.assertEquals(row.getLong("status"), metadata.getStatus().longValue());
                Assert.assertEquals(row.getInt("is_delete"), metadata.getIsDelete().intValue());
                Assert.assertEquals(row.getTimestamp("create_time").toLocalDateTime(), metadata.getCreateTime());
                Assert.assertEquals(row.getTimestamp("update_time").toLocalDateTime(), metadata.getUpdateTime());
                if (metadata instanceof RuntimeMetadata runtime) {
                    Assert.assertEquals(row.getLong("cluster_id"), runtime.getClusterId().longValue());
                    Assert.assertEquals(row.getString("host") + "-" + row.getInt("port"), runtime.nodeUnique());
                }
                Assert.assertFalse(row.next());
            }
        }
    }

    @Test
    public void missingClusterQueryReturnsNull() {
        ClusterRequestDTO dto = new ClusterRequestDTO();
        dto.setId(Long.MAX_VALUE);
        Assert.assertNull(clusterService.queryClusterById(ClusterControllerMapper.INSTANCE.toClusterEntity(dto)));
        System.out.println("MySQL 不存在集群查询通过：返回空");
    }

    private Set<Long> queryIds(String sql) throws Exception {
        Set<Long> ids = new HashSet<>();
        try (Statement statement = session.getConnection().createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                ids.add(rows.getLong(1));
            }
        }
        return ids;
    }
}
