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
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.domain.Impl.ClusterAndRuntimeDomainImpl;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterInSyncDO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryRelationClusterByClusterIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterRelationshipServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.RuntimeServiceImpl;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterConvertMetaData;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ClusterIdentityQueryTest {

    private Connection connection;
    private SqlSession session;
    private ClusterServiceImpl clusterService;
    private ClusterAndRuntimeDomainImpl domain;

    @Before
    public void prepareQueryDatabase() throws Exception {
        UnpooledDataSource source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        connection = source.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("create table cluster (id bigint, name varchar(100), cluster_type varchar(100), status int, is_delete int default 0)");
            statement.execute("insert into cluster values (10, 'root', 'STORAGE_JVM_CLUSTER', 1, 0),"
                + " (20, 'broker', 'STORAGE_JVM_BROKER', 1, 0), (30, 'other', 'STORAGE_JVM_BROKER', 1, 0)");
            statement.execute("create table runtime (id bigint, cluster_id bigint, name varchar(100), cluster_type varchar(100), status int, is_delete int default 0)");
            statement.execute("insert into runtime values (201, 20, 'node', 'STORAGE_JVM_BROKER', 1, 0),"
                + " (301, 30, 'other-node', 'STORAGE_JVM_BROKER', 1, 0)");
            statement.execute("create table cluster_relationship (id bigint, cluster_id bigint, relationship_id bigint,"
                + " cluster_type varchar(100), relationship_type varchar(100), status int, is_delete int default 0)");
            statement.execute("insert into cluster_relationship values (1, 10, 20, 'STORAGE_JVM_CLUSTER', 'STORAGE_JVM_BROKER', 1, 0)");
        }
        Configuration configuration = new Configuration(new Environment("cluster-query", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(ClusterMapper.class);
        configuration.addMapper(RuntimeMapper.class);
        configuration.addMapper(ClusterRelationshipMapper.class);
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        clusterService = new ClusterServiceImpl();
        ReflectionTestUtils.setField(clusterService, "clusterMapper", session.getMapper(ClusterMapper.class));
        RuntimeServiceImpl runtimeService = new RuntimeServiceImpl();
        ReflectionTestUtils.setField(runtimeService, "runtimeMapper", session.getMapper(RuntimeMapper.class));
        ClusterRelationshipServiceImpl relationships = new ClusterRelationshipServiceImpl();
        ReflectionTestUtils.setField(relationships, "clusterRelationshipMapper", session.getMapper(ClusterRelationshipMapper.class));
        domain = new ClusterAndRuntimeDomainImpl();
        ReflectionTestUtils.setField(domain, "clusterService", clusterService);
        ReflectionTestUtils.setField(domain, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(domain, "clusterRelationshipService", relationships);
    }

    @After
    public void closeQueryDatabase() throws Exception {
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void queryFlowsFromDtoThroughDatabaseToMetadataAndTopology() throws Exception {
        ClusterRequestDTO request = new ObjectMapper().readValue("{\"id\":10}", ClusterRequestDTO.class);
        Assert.assertEquals(Long.valueOf(10), request.getId());
        Assert.assertFalse(new ObjectMapper().valueToTree(request).has("clusterId"));
        ClusterEntity query = ClusterControllerMapper.INSTANCE.toClusterEntity(request);
        Assert.assertEquals(Long.valueOf(10), query.getId());
        ClusterEntity result = clusterService.queryClusterById(query);
        Assert.assertEquals("root", result.getName());
        ClusterMetadata metadata = ClusterConvertMetaData.INSTANCE.toMetaData(result);
        Assert.assertEquals("10", metadata.nodeUnique());
        Assert.assertEquals(Long.valueOf(10), ClusterConvertMetaData.INSTANCE.toEntity(metadata).getId());
        ClusterMetadataDomain topology = new ClusterMetadataDomain();
        topology.setMainCluster(result);
        Assert.assertEquals(Long.valueOf(10), topology.getColonyDO().getClusterId());
        Assert.assertFalse(new ObjectMapper().valueToTree(result).has("clusterId"));
        Assert.assertFalse(Arrays.stream(ClusterEntity.class.getMethods()).anyMatch(method -> method.getName().equals("getClusterId")));
        System.out.println("查询通过：DTO.id=10 → Entity.id=10 → SQL → Metadata → 主集群拓扑，Entity 无 clusterId 属性");
    }

    @Test
    public void relatedClusterQueryUsesSelectedClusterId() throws Exception {
        QueryRelationClusterByClusterIdAndTypeDTO request = new ObjectMapper().readValue(
            "{\"id\":10}", QueryRelationClusterByClusterIdAndTypeDTO.class);
        Assert.assertFalse(new ObjectMapper().valueToTree(request).has("clusterId"));
        ClusterEntity query = ClusterControllerMapper.INSTANCE.queryRelationClusterByClusterIdAndType(request);
        List<ClusterEntity> related = clusterService.queryRelationClusterByClusterIdAndType(query);
        Assert.assertEquals(1, related.size());
        Assert.assertEquals(Long.valueOf(20), related.get(0).getId());
        System.out.println("关联查询通过：集群 10 只返回关联集群 20，不返回集群 30");
    }

    @Test
    public void relationshipAssemblyKeepsRuntimeIdSeparateFromParentId() {
        ClusterEntity query = QueryClusterInSyncDO.create(10L, new Object()).getClusterEntity();
        Assert.assertNotNull(query);
        ClusterAndRuntimeOfRelationshipDO result = domain.getAllClusterAndRuntimeByCluster(query, DeployStatusType.CREATE);
        Assert.assertEquals(1, result.getClusterRelationshipTripleList().size());
        Assert.assertEquals(Long.valueOf(10), result.getClusterRelationshipTripleList().get(0).getMiddle().getId());
        Assert.assertEquals(Long.valueOf(20), result.getClusterRelationshipTripleList().get(0).getRight().getId());
        Assert.assertEquals(1, result.getClusterEntityPairleList().size());
        Assert.assertEquals(Long.valueOf(20), result.getClusterEntityPairleList().get(0).getRight().get(0).getId());
        Assert.assertEquals(1, result.getRuntimeEntityPairList().size());
        List<RuntimeEntity> nodes = result.getRuntimeEntityPairList().get(0).getRight();
        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals(Long.valueOf(201), nodes.get(0).getId());
        Assert.assertEquals(Long.valueOf(20), nodes.get(0).getClusterId());
        System.out.println("查询与组装通过：主集群 10 → 子集群 20 → Runtime 201，按所属集群 20 分组");
    }

    @Test
    public void runtimeQueryRetainsParentClusterReference() throws Exception {
        ClusterIdDTO request = new ObjectMapper().readValue("{\"clusterId\":20}", ClusterIdDTO.class);
        RuntimeEntity query = org.apache.eventmesh.dashboard.console.mapstruct.cluster.RuntimeControllerMapper.INSTANCE
            .queryRuntimeListByClusterId(request);
        Assert.assertNull(query.getId());
        Assert.assertEquals(Long.valueOf(20), query.getClusterId());
        List<RuntimeEntity> nodes = session.getMapper(RuntimeMapper.class).getRuntimesToFrontByCluster(query);
        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals(Long.valueOf(201), nodes.get(0).getId());
    }

    @Test
    public void unknownClusterDoesNotFallBackToAnotherIdentity() {
        ClusterRequestDTO request = new ClusterRequestDTO();
        request.setId(999L);
        Assert.assertNull(clusterService.queryClusterById(ClusterControllerMapper.INSTANCE.toClusterEntity(request)));
        System.out.println("不存在编号查询通过：集群 999 返回空");
    }
}
