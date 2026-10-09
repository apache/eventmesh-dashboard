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
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.*;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.*;
import org.apache.eventmesh.dashboard.console.mapper.cluster.*;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.*;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class BaseIdDatabaseTest {
    static final LocalDateTime BOUNDARY = LocalDateTime.of(2026, 1, 1, 0, 0);
    SqlSession session;
    private String mysqlDatabase;
    private Connection mysqlAdmin;

    @Before
    public void database() throws Exception {
        DataSource dataSource;
        if (Boolean.getBoolean("baseid.mysql")) {
            String server = System.getenv("BASEID_MYSQL_SERVER");
            assertNotNull("BASEID_MYSQL_SERVER is required", server);
            assertTrue("Only local test server is allowed", server.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/"));
            String user = System.getenv("BASEID_MYSQL_USER");
            String password = System.getenv("BASEID_MYSQL_PASSWORD");
            mysqlAdmin = DriverManager.getConnection(server + "?useSSL=false&allowPublicKeyRetrieval=true", user, password);
            mysqlDatabase = "baseid_mysql_" + UUID.randomUUID().toString().replace("-", "");
            try (Statement statement = mysqlAdmin.createStatement()) {
                statement.execute("CREATE DATABASE `" + mysqlDatabase + "`");
            }
            dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", server + mysqlDatabase
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&sessionVariables=time_zone=%27%2B08:00%27", user, password);
        } else {
            JdbcDataSource h2 = new JdbcDataSource();
            h2.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
            dataSource = h2;
        }
        Configuration configuration = new Configuration(new Environment("baseid", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        for (Class<?> mapper : List.of(TopicMapper.class, ConfigMapper.class, GroupMapper.class, GroupMemberMapper.class,
            RuntimeMapper.class, ClusterMapper.class, ClusterRelationshipMapper.class)) {
            configuration.addMapper(mapper);
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        String ddl = new String(getClass().getResourceAsStream("/eventmesh-dashboard.sql").readAllBytes(), StandardCharsets.UTF_8);
        for (String table : List.of("topic", "config", "group", "group_member", "runtime", "cluster", "cluster_relationship")) {
            java.util.regex.Matcher matcher = Pattern.compile("(?is)create table `?" + table + "`?\\s*\\(.*?;").matcher(ddl);
            assertTrue(table, matcher.find());
            try (Statement statement = session.getConnection().createStatement()) {
                // H2 requires DEFAULT before UNIQUE; retain the repository column constraint.
                statement.execute(mysqlDatabase != null ? matcher.group() : matcher.group()
                    .replaceAll("bigint\\s+unique\\s+not null default 0", "bigint not null default 0 unique"));
            }
        }
    }

    @After
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        if (mysqlAdmin != null) {
            try (Connection admin = mysqlAdmin; Statement statement = admin.createStatement()) {
                if (mysqlDatabase != null) {
                    statement.execute("DROP DATABASE `" + mysqlDatabase + "`");
                }
            }
        }
    }

    // Writes below only prepare fixtures; these tests validate SELECT behavior, not production write operations.
    @Test
    public void topicQueriesHideDeletionMarkersAndSyncReadsThem() throws Exception {
        seed("topic", TopicEntity.class, 11, Map.of("topic_name", "orders"));
        TopicMapper mapper = session.getMapper(TopicMapper.class);
        TopicEntity query = new TopicEntity();
        query.setId(11L);
        query.setUpdateTime(BOUNDARY);
        assertNotNull(mapper.queryTopicById(query));
        sql("update topic set is_delete=1 where id=11");
        assertNull(mapper.queryTopicById(query));
        assertTrue(mapper.selectAll().isEmpty());
        TopicEntity tombstone = mapper.syncGet(query).get(0);
        assertEquals(Long.valueOf(1), tombstone.getStatus());
        assertEquals(Integer.valueOf(1), tombstone.getIsDelete());
    }

    @Test
    public void groupQueriesHideDeletionMarkersAndPreserveStatusZero() throws Exception {
        seed("group", GroupEntity.class, 12, Map.of("name", "consumers"));
        GroupMapper mapper = session.getMapper(GroupMapper.class);
        GroupEntity query = new GroupEntity();
        query.setId(12L);
        query.setUpdateTime(BOUNDARY);
        assertNotNull(mapper.selectGroupById(query));
        sql("update `group` set is_delete=1 where id=12");
        assertNull(mapper.selectGroupById(query));
        assertTrue(mapper.selectAll().isEmpty());
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
        sql("update `group` set status=0, is_delete=0 where id=12");
        assertEquals(Long.valueOf(0), mapper.syncGet(query).get(0).getStatus());
    }

    @Test
    public void memberQueriesKeepStatusZeroVisibleAndHideDeletionMarkers() throws Exception {
        seed("group_member", GroupMemberEntity.class, 13, Map.of("topic_name", "orders", "group_name", "consumers", "status", 0));
        GroupMemberMapper mapper = session.getMapper(GroupMemberMapper.class);
        GroupMemberEntity query = new GroupMemberEntity();
        query.setId(13L);
        query.setUpdateTime(BOUNDARY);
        assertNotNull(mapper.selectGroupMemberById(query));
        assertEquals(1, mapper.selectMember(new GroupMemberEntity()).size());
        sql("update group_member set is_delete=1 where id=13");
        assertNull(mapper.selectGroupMemberById(query));
        assertTrue(mapper.selectMember(new GroupMemberEntity()).isEmpty());
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
    }

    @Test
    public void configQueriesHideDeletedRowsAndSyncIncludesLegacyTombstones() throws Exception {
        seed("config", ConfigEntity.class, 14, Map.of("config_name", "retention", "is_default", 0,
            "instance_type", MetadataType.CLUSTER.name()));
        ConfigMapper mapper = session.getMapper(ConfigMapper.class);
        ConfigEntity query = new ConfigEntity();
        query.setId(14L);
        query.setUpdateTime(BOUNDARY);
        assertEquals(1, mapper.selectAll().size());
        sql("update config set is_delete=1 where id=14");
        assertTrue(mapper.selectAll().isEmpty());
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
        sql("update config set status=0, is_delete=0 where id=14");
        assertTrue(mapper.selectAll().isEmpty());
        assertEquals(Long.valueOf(0), mapper.syncGet(query).get(0).getStatus());
        seed("topic", TopicEntity.class, 15, Map.of("topic_name", "legacy", "status", 0));
        TopicEntity topicQuery = new TopicEntity();
        topicQuery.setUpdateTime(BOUNDARY);
        assertEquals(Long.valueOf(0), session.getMapper(TopicMapper.class).syncGet(topicQuery).get(0).getStatus());
    }

    void sql(String sql) throws SQLException {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute(sql);
            session.clearCache();
        }
    }

    // All seven schemas are the repository DDL. Supply required synthetic values without weakening constraints.
    void seed(String table, Class<? extends BaseIdEntity> type, long id, Map<String, Object> overrides) throws Exception {
        Map<String, Class<?>> types = new HashMap<>();
        for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            String column = property.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            types.put(column, property.getPropertyType());
        }
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        try (ResultSet rs = session.getConnection().getMetaData().getColumns(session.getConnection().getCatalog(), null, table, null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                Class<?> propertyType = types.get(column);
                Object value;
                if (column.equals("id")) {
                    value = id;
                } else if (column.endsWith("time") || column.endsWith("timestamp")) {
                    value = Timestamp.valueOf(BOUNDARY);
                } else if (column.equals("cluster_type")) {
                    value = ClusterType.EVENTMESH_RUNTIME.name();
                } else if (propertyType != null && propertyType.isEnum()) {
                    value = ((Enum<?>) propertyType.getEnumConstants()[0]).name();
                } else if (List.of(Types.INTEGER, Types.BIGINT, Types.TINYINT, Types.SMALLINT).contains(rs.getInt("DATA_TYPE"))) {
                    value = column.equals("status") ? 1 : 0;
                } else {
                    value = propertyType == Integer.class || propertyType == Long.class ? "0" : "";
                }
                columns.add("`" + column + "`");
                values.add(overrides.getOrDefault(column, value));
            }
        }
        String sql = "insert into `" + table + "` (" + String.join(",", columns) + ") values ("
            + String.join(",", Collections.nCopies(values.size(), "?")) + ")";
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
            statement.executeUpdate();
        }
    }
}
