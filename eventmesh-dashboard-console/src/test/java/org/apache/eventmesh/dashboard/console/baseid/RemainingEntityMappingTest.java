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

import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.LogEntity;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.OprLogMapper;

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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/** Regression checks for inherited identity properties in the remaining entities. */
public class RemainingEntityMappingTest {

    @Test
    public void identitiesAreInheritedAndRemainJsonBeanProperties() throws Exception {
        assertFalse(declaresField(ConnectorEntity.class, "id"));
        assertFalse(declaresField(ConnectorEntity.class, "clusterId"));
        assertFalse(declaresField(LogEntity.class, "id"));
        assertFalse(declaresField(LogEntity.class, "clusterId"));
        assertEquals(BaseRuntimeIdEntity.class, ConnectorEntity.class.getSuperclass());
        assertEquals(BaseRuntimeIdEntity.class, LogEntity.class.getSuperclass());

        ObjectMapper mapper = new ObjectMapper();
        ConnectorEntity connector = mapper.readValue(
            "{\"id\":17,\"clusterId\":23,\"runtimeId\":31,\"name\":\"source\",\"port\":9000}",
            ConnectorEntity.class);
        assertEquals(Long.valueOf(17L), connector.getId());
        assertEquals(Long.valueOf(23L), connector.getClusterId());
        assertEquals(Long.valueOf(31L), connector.getRuntimeId());
        JsonNode connectorJson = mapper.valueToTree(connector);
        assertEquals(23L, connectorJson.get("clusterId").asLong());
        assertEquals("source", connectorJson.get("name").asText());

        LogEntity log = mapper.readValue(
            "{\"id\":41,\"clusterId\":43,\"operationType\":\"CREATE\",\"content\":\"created\"}",
            LogEntity.class);
        assertEquals(Long.valueOf(41L), log.getId());
        assertEquals(Long.valueOf(43L), log.getClusterId());
        assertEquals("CREATE", log.getOperationType());
        assertEquals("created", mapper.valueToTree(log).get("content").asText());

        ConnectorEntity same = new ConnectorEntity();
        same.setId(connector.getId());
        same.setClusterId(connector.getClusterId());
        same.setRuntimeId(connector.getRuntimeId());
        same.setName(connector.getName());
        same.setPort(connector.getPort());
        assertTrue(connector.equals(same));
        same.setClusterId(24L);
        assertFalse(connector.equals(same));
    }

    @Test
    public void connectorAndOperationLogMappersBindAndReadInheritedIdentity() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        try (Connection keeper = dataSource.getConnection()) {
            try (Statement statement = keeper.createStatement()) {
                statement.execute("create table connector (id bigint, cluster_id bigint, runtime_id bigint, name varchar(100), "
                    + "class_name varchar(100), type varchar(40), status bigint, pod_state int, config_ids varchar(100), host varchar(100), "
                    + "port int)");
                statement.execute("insert into connector values (5, 23, 31, 'source', 'C', 'SOURCE', 1, 2, '7,8', 'localhost', 9000)");
                statement.execute("create table operation_log (id bigint generated by default as identity, cluster_id bigint, "
                    + "operation_type varchar(40), target_type varchar(40), content varchar(200), operation_user varchar(100), "
                    + "result varchar(100), state int, end_time timestamp, is_delete int default 0)");
                statement.execute("insert into operation_log (id, cluster_id, operation_type, target_type, content, operation_user, "
                    + "result, state, is_delete) values (9, 43, 'CREATE', 'CONNECTOR', 'created', 'alice', 'ok', 1, 0)");
                statement.execute("insert into operation_log (id, cluster_id, operation_type, target_type, content, operation_user, "
                    + "result, state, is_delete) values (10, 44, 'CREATE', 'CONNECTOR', 'other cluster', 'alice', 'ok', 1, 0)");
            }

            Configuration configuration = new Configuration(
                new Environment("remaining-entity-mapping", new JdbcTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addMapper(ConnectorMapper.class);
            configuration.addMapper(OprLogMapper.class);
            configuration.addInterceptor(new H2OperationLogValueSyntaxAdapter());
            try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession(true)) {
                ConnectorEntity query = new ConnectorEntity();
                query.setClusterId(23L);
                ConnectorEntity connector = session.getMapper(ConnectorMapper.class).selectByClusterId(query).get(0);
                assertEquals(Long.valueOf(5L), connector.getId());
                assertEquals(Long.valueOf(23L), connector.getClusterId());
                assertEquals(Long.valueOf(31L), connector.getRuntimeId());
                assertEquals("source", connector.getName());

                BoundSql connectorInsert = mapperSql(configuration, ConnectorMapper.class, "insert", connector);
                assertBoundParameter(connectorInsert, connector, "clusterId", 23L);

                LogEntity logQuery = new LogEntity();
                logQuery.setClusterId(43L);
                logQuery.setOperationUser("alice");
                List<LogEntity> logs = session.getMapper(OprLogMapper.class).getLogList(logQuery);
                assertEquals(1, logs.size());
                LogEntity log = logs.get(0);
                assertEquals(Long.valueOf(9L), log.getId());
                assertEquals(Long.valueOf(43L), log.getClusterId());
                assertEquals("CREATE", log.getOperationType());
                assertEquals("alice", log.getOperationUser());

                LogEntity otherCluster = new LogEntity();
                otherCluster.setClusterId(44L);
                assertEquals(10L, session.getMapper(OprLogMapper.class).getLogList(otherCluster).get(0).getId().longValue());

                LogEntity insert = new LogEntity();
                insert.setClusterId(47L);
                insert.setOperationType("DELETE");
                insert.setTargetType("CONNECTOR");
                insert.setContent("removed");
                insert.setOperationUser("bob");
                insert.setResult("ok");
                session.getMapper(OprLogMapper.class).addLog(insert);
                assertNotNull("Generated key must populate inherited BaseIdEntity.id", insert.getId());
                insert.setState(2);
                insert.setResult("finished");
                assertEquals(Integer.valueOf(1), session.getMapper(OprLogMapper.class).updateLog(insert));
                LogEntity inserted = new LogEntity();
                inserted.setClusterId(47L);
                LogEntity persisted = session.getMapper(OprLogMapper.class).getLogList(inserted).get(0);
                assertEquals(insert.getId(), persisted.getId());
                assertEquals(Integer.valueOf(2), persisted.getState());
                assertEquals("finished", persisted.getResult());
            }
        }
    }

    private boolean declaresField(Class<?> type, String name) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private BoundSql mapperSql(Configuration configuration, Class<?> mapper, String methodName, Object parameter)
        throws Exception {
        String[] statement = mapper.getMethod(methodName, parameter.getClass())
            .getAnnotation(org.apache.ibatis.annotations.Insert.class).value();
        return new XMLLanguageDriver().createSqlSource(configuration, String.join(" ", statement), parameter.getClass())
            .getBoundSql(parameter);
    }

    private void assertBoundParameter(BoundSql boundSql, Object parameter, String property, Object expected) {
        String mappedProperty = boundSql.getParameterMappings().stream().map(mapping -> mapping.getProperty())
            .filter(property::equals).findFirst().orElseThrow();
        assertEquals(expected, SystemMetaObject.forObject(parameter).getValue(mappedProperty));
    }

    /** H2 does not accept OprLogMapper's MySQL VALUE token; production SQL remains unchanged. */
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    private static class H2OperationLogValueSyntaxAdapter implements Interceptor {

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
            // No configuration is needed for this test-only SQL adapter.
        }
    }
}
