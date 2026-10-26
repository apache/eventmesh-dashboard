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
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.NetConnectionEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

public class EntityIdentityMappingTest {

    @Test
    public void identityFieldsAreStoredOnlyInTheBaseClasses() {
        assertFalse(declaresField(AclEntity.class, "id"));
        assertTrue(declaresField(AclEntity.class, "clusterId"));
        assertFalse(declaresField(ConnectionEntity.class, "clusterId"));
        assertFalse(declaresField(ConnectionEntity.class, "runtimeId"));
        assertFalse(declaresField(NetConnectionEntity.class, "clusterId"));
        assertFalse(declaresField(NetConnectionEntity.class, "runtimeId"));
        assertEquals(BaseIdEntity.class, AclEntity.class.getSuperclass());
        assertEquals(BaseRuntimeIdEntity.class, ConnectionEntity.class.getSuperclass());
        assertEquals(BaseRuntimeIdEntity.class, NetConnectionEntity.class.getSuperclass());
    }

    @Test
    public void inheritedIdentityParticipatesInEqualityAndHashCode() {
        AclEntity acl1 = new AclEntity();
        AclEntity acl2 = new AclEntity();
        acl1.setId(7L);
        acl2.setId(7L);
        acl1.setClusterId(11L);
        acl2.setClusterId(11L);
        acl1.setPattern("orders");
        acl2.setPattern("orders");
        assertEquals(acl1, acl2);
        assertEquals(acl1.hashCode(), acl2.hashCode());
        acl2.setId(8L);
        assertFalse(acl1.equals(acl2));

        ConnectionEntity connection1 = new ConnectionEntity();
        ConnectionEntity connection2 = new ConnectionEntity();
        connection1.setClusterId(11L);
        connection2.setClusterId(11L);
        connection1.setRuntimeId(21L);
        connection2.setRuntimeId(21L);
        assertEquals(connection1, connection2);
        connection2.setClusterId(12L);
        assertFalse(connection1.equals(connection2));

        NetConnectionEntity net1 = new NetConnectionEntity();
        NetConnectionEntity net2 = new NetConnectionEntity();
        net1.setClusterId(11L);
        net2.setClusterId(11L);
        net1.setRuntimeId(21L);
        net2.setRuntimeId(21L);
        assertEquals(net1, net2);
        net2.setRuntimeId(22L);
        assertFalse(net1.equals(net2));
    }

    @Test
    public void inheritedIdentityKeepsJsonAndBeanPropertyContracts() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AclEntity acl = objectMapper.readValue("{\"id\":7,\"clusterId\":11}", AclEntity.class);
        assertEquals(Long.valueOf(7L), acl.getId());
        assertEquals(Long.valueOf(11L), acl.getClusterId());
        JsonNode aclJson = objectMapper.valueToTree(acl);
        assertEquals(7L, aclJson.get("id").asLong());
        assertEquals(11L, aclJson.get("clusterId").asLong());

        ConnectionEntity connection = objectMapper.readValue(
            "{\"id\":7,\"clusterId\":11,\"runtimeId\":21}", ConnectionEntity.class);
        assertEquals(Long.valueOf(7L), connection.getId());
        assertEquals(Long.valueOf(11L), connection.getClusterId());
        assertEquals(Long.valueOf(21L), connection.getRuntimeId());
        JsonNode connectionJson = objectMapper.valueToTree(connection);
        assertEquals(11L, connectionJson.get("clusterId").asLong());
        assertEquals(21L, connectionJson.get("runtimeId").asLong());

        NetConnectionEntity net = objectMapper.readValue(
            "{\"id\":7,\"clusterId\":11,\"runtimeId\":21}", NetConnectionEntity.class);
        assertEquals(Long.valueOf(7L), net.getId());
        assertEquals(Long.valueOf(11L), net.getClusterId());
        assertEquals(Long.valueOf(21L), net.getRuntimeId());
        JsonNode netJson = objectMapper.valueToTree(net);
        assertEquals(11L, netJson.get("clusterId").asLong());
        assertEquals(21L, netJson.get("runtimeId").asLong());

        Schema clusterIdSchema = annotationOnField(ConnectionEntity.class, "clusterId");
        Method getter = ConnectionEntity.class.getMethod("getClusterId");
        if (clusterIdSchema == null) {
            clusterIdSchema = getter.getAnnotation(Schema.class);
        }
        assertNotNull(clusterIdSchema);
        assertEquals("runtime cluster id", clusterIdSchema.description());
    }

    @Test
    public void actualAclAndConnectionMappersBindAndReadInheritedProperties() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create table acl (id bigint generated by default as identity, cluster_id bigint, pattern varchar(100), operation int, "
                + "permission_type int, host varchar(100), resource_type int, resource_name varchar(100), pattern_type int)");
            statement.execute("insert into acl values (7, 11, 'orders', 1, 2, 'localhost', 3, 'orders', 4)");
            statement.execute("alter table acl add column is_delete int default 0");
            statement.execute("create table connection (id bigint, cluster_id bigint, runtime_id bigint, "
                + "source_type varchar(30), source_id bigint, sink_type varchar(30), sink_id bigint, "
                + "status int, topic varchar(100), group_id bigint, description varchar(100))");
            statement.execute("insert into connection values (8, 11, 21, 'client', 31, 'connector', 41, 1, 'orders', 51, 'route')");
            statement.execute("alter table connection add column end_time timestamp null");
        }

        Configuration configuration = new Configuration(
            new Environment("entity-identity", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(AclMapper.class);
        configuration.addMapper(ConnectionMapper.class);
        configuration.addInterceptor(new H2AclValuesSyntaxAdapter());
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession(true)) {
            AclEntity insertedAcl = new AclEntity();
            insertedAcl.setClusterId(12L);
            insertedAcl.setPattern("payments");
            insertedAcl.setOperation(1);
            insertedAcl.setPermissionType(2);
            insertedAcl.setHost("localhost");
            insertedAcl.setResourceType(3);
            insertedAcl.setResourceName("payments");
            insertedAcl.setPatternType(4);
            session.getMapper(AclMapper.class).insert(insertedAcl);
            assertNotNull("Generated key must populate inherited BaseIdEntity.id", insertedAcl.getId());

            AclEntity acl = session.getMapper(AclMapper.class).selectById(insertedAcl);
            assertEquals(insertedAcl.getId(), acl.getId());
            assertEquals(Long.valueOf(12L), acl.getClusterId());

            AclEntity seededAcl = session.getMapper(AclMapper.class).selectAll().stream()
                .filter(row -> Long.valueOf(7L).equals(row.getId())).findFirst().orElseThrow();
            assertEquals(Long.valueOf(7L), seededAcl.getId());
            assertEquals(Long.valueOf(11L), seededAcl.getClusterId());

            ConnectionEntity query = new ConnectionEntity();
            query.setClusterId(11L);
            List<ConnectionEntity> found = session.getMapper(ConnectionMapper.class).selectByClusterId(query);
            assertEquals(1, found.size());
            assertEquals(Long.valueOf(11L), found.get(0).getClusterId());
            assertEquals(Long.valueOf(21L), found.get(0).getRuntimeId());

            ConnectionEntity candidate = new ConnectionEntity();
            candidate.setClusterId(12L);
            candidate.setRuntimeId(22L);
            BoundSql insertSql = mapperSql(configuration, ConnectionMapper.class, "insert", candidate);
            assertBoundParameter(insertSql, candidate, "clusterId", 12L);
            assertBoundParameter(insertSql, candidate, "runtimeId", 22L);
        }
    }

    private BoundSql mapperSql(Configuration configuration, Class<?> mapper, String methodName, Object parameter)
        throws Exception {
        String script = String.join(" ", Arrays.asList(mapper.getMethod(methodName, parameter.getClass())
            .getAnnotation(org.apache.ibatis.annotations.Insert.class).value()));
        return new XMLLanguageDriver().createSqlSource(configuration, script, parameter.getClass()).getBoundSql(parameter);
    }

    private void assertBoundParameter(BoundSql boundSql, Object parameter, String property, Object expected) {
        String mappedProperty = boundSql.getParameterMappings().stream().map(mapping -> mapping.getProperty())
            .filter(mapped -> mapped.equals(property)).findFirst().orElseThrow();
        assertEquals(expected, SystemMetaObject.forObject(parameter).getValue(mappedProperty));
    }

    private boolean declaresField(Class<?> type, String name) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Schema annotationOnField(Class<?> type, String name) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field.getAnnotation(Schema.class);
            }
        }
        return null;
    }

    /** Adapts AclMapper's VALUE token for H2; this test does not validate the original SQL on MySQL. */
    @Intercepts(@Signature(type = StatementHandler.class,
        method = "prepare", args = {Connection.class, Integer.class}))
    private static class H2AclValuesSyntaxAdapter implements Interceptor {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            MetaObject statement = SystemMetaObject.forObject(invocation.getTarget());
            String sql = (String) statement.getValue("delegate.boundSql.sql");
            if (sql != null && sql.contains(")VALUE (")) {
                statement.setValue("delegate.boundSql.sql", sql.replace(")VALUE (", ")VALUES ("));
            }
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        @Override
        public void setProperties(Properties properties) {
            // No configurable behavior is needed for this test-only adapter.
        }
    }
}
