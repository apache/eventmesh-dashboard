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
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.common.model.base.BaseOrganizationBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMemberMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.LogEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.deploy.DeployScriptMapper;
import org.apache.eventmesh.dashboard.console.mapper.deploy.PortMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.OprLogMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatusSeparationTest {
    private final BaseIdDatabaseTest db = new BaseIdDatabaseTest();

    @Before
    public void open() throws Exception {
        db.database();
        for (Class<?> mapper : List.of(DeployScriptMapper.class, ConnectionMapper.class, OprLogMapper.class, AclMapper.class, PortMapper.class)) {
            db.session.getConfiguration().addMapper(mapper);
        }
        createRepositoryTable("deploy_script");
        createRepositoryTable("operation_log");
        createRepositoryTable("acl");
        createRepositoryTable("port");
        // Production Connection DDL is absent. This isolated fixture covers only existing Mapper columns.
        db.sql("CREATE TABLE connection (id BIGINT AUTO_INCREMENT PRIMARY KEY, cluster_id BIGINT, source_id BIGINT, source_type INT,"
            + "sink_id BIGINT, sink_type INT, runtime_id BIGINT, status INT DEFAULT 1, topic VARCHAR(100),"
            + "group_id BIGINT, description VARCHAR(100), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, end_time TIMESTAMP NULL)");
    }

    @After
    public void close() throws Exception {
        db.close();
    }

    @Test
    public void deletionDoesNotDependOnBusinessStatusOrEqualTimestamps() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 22, 0, 0);
        for (BaseOrganizationBase metadata : List.of(new TopicMetadata(), new GroupMetadata(), new ConfigMetadata(), new GroupMemberMetadata())) {
            metadata.setCreateTime(time);
            metadata.setUpdateTime(time);
            for (Long status : Arrays.asList(null, 0L, 1L, 2L, 3L)) {
                metadata.setStatus(status);
                metadata.setIsDelete(0);
                assertFalse(metadata.isDelete());
                metadata.setIsDelete(1);
                assertTrue(metadata.isDelete());
                assertFalse(metadata.isInsert());
                assertFalse(metadata.isUpdate());
                assertEquals(status, metadata.getStatus());
            }
        }
    }

    @Test
    public void scriptDeletionPreservesBusinessStatusAndHidesRows() throws Exception {
        db.seed("deploy_script", DeployScriptEntity.class, 1, Map.of("organization_id", 10, "name", "stopped", "status", 0));
        db.seed("deploy_script", DeployScriptEntity.class, 2, Map.of("organization_id", 10, "name", "running", "status", 2));
        DeployScriptMapper mapper = db.session.getMapper(DeployScriptMapper.class);
        DeployScriptEntity query = new DeployScriptEntity();
        query.setOrganizationId(10L);
        assertEquals(2, mapper.queryByName(query).size());
        DeployScriptEntity row = mapper.queryByName(query).stream().filter(v -> v.getId() == 2L).findFirst().orElseThrow();
        mapper.deleteById(row);
        mapper.deleteById(row);
        assertEquals(1, mapper.queryByName(query).size());
        try (var statement = db.session.getConnection().createStatement();
             var result = statement.executeQuery("SELECT status,is_delete,update_time FROM deploy_script WHERE id=2")) {
            assertTrue(result.next());
            assertEquals(2, result.getLong("status"));
            assertEquals(1, result.getInt("is_delete"));
            assertTrue(result.getTimestamp("update_time").toLocalDateTime().isAfter(BaseIdDatabaseTest.BOUNDARY));
        }
    }

    @Test
    public void runtimeStopAndDeleteAreDifferentWritesAndRecreationClearsDeletion() throws Exception {
        db.seed("runtime", RuntimeEntity.class, 10, Map.of("cluster_id", 7, "status", 2, "host", 2130706433));
        db.seed("runtime", RuntimeEntity.class, 11, Map.of("cluster_id", 8, "status", 1, "kubernetes_cluster_id", 1));
        RuntimeMapper mapper = db.session.getMapper(RuntimeMapper.class);
        RuntimeEntity query = new RuntimeEntity();
        query.setId(10L);
        query.setClusterId(7L);
        query.setUpdateTime(BaseIdDatabaseTest.BOUNDARY);
        mapper.deactivate(query);
        RuntimeEntity row = mapper.getRuntimesToFrontByCluster(query).get(0);
        assertEquals(Long.valueOf(0), row.getStatus());
        assertEquals(Integer.valueOf(0), row.getIsDelete());
        mapper.deleteRuntimeByCluster(query);
        mapper.deleteRuntimeByCluster(query);
        assertTrue(mapper.getRuntimesToFrontByCluster(query).isEmpty());
        RuntimeEntity deleted = mapper.queryByUpdateTime(query).stream().filter(v -> v.getId() == 10L).findFirst().orElseThrow();
        assertEquals(Long.valueOf(0), deleted.getStatus());
        assertEquals(Integer.valueOf(1), deleted.getIsDelete());
        query.setClusterId(8L);
        assertEquals(1, mapper.getRuntimesToFrontByCluster(query).size());
        mapper.insertRuntime(row);
        query.setClusterId(7L);
        RuntimeEntity restored = mapper.getRuntimesToFrontByCluster(query).get(0);
        assertEquals(Long.valueOf(1), restored.getStatus());
        assertEquals(Integer.valueOf(0), restored.getIsDelete());
    }

    @Test
    public void endedConnectionsAreInactiveButRetainedForHistory() throws Exception {
        seedConnections();
        ConnectionMapper mapper = db.session.getMapper(ConnectionMapper.class);
        ConnectionEntity query = connection(1);
        assertEquals(3, mapper.selectAll().size());
        mapper.endConnectionById(query);
        mapper.endConnectionById(query);
        assertEquals(2, mapper.selectAll().size());
        assertEquals(Integer.valueOf(2), mapper.selectConnectionNumByCluster(query));
        assertEquals(2, mapper.selectToFrontByClusterId(query).size());
        assertEquals(2, mapper.selectByClusterIdSourceTypeAndSourceId(query).size());
        assertEquals(2, mapper.selectByClusterIdSinkTypeAndSinkId(query).size());
        assertEquals(0L, storedValue("SELECT status FROM connection WHERE id=1"));
        assertEquals(1L, storedValue("SELECT COUNT(*) FROM connection WHERE id=1 AND end_time IS NOT NULL"));
        assertEquals(3L, storedValue("SELECT COUNT(*) FROM connection"));
        mapper.batchEndConnectionById(List.of(connection(2), connection(3)));
        assertTrue(mapper.selectAll().isEmpty());
        assertEquals(3L, storedValue("SELECT COUNT(*) FROM connection"));
    }

    @Test
    public void legacyEndedConnectionIsHiddenAndReactivationClearsEndTime() throws Exception {
        seedConnections();
        db.sql("UPDATE connection SET end_time=CURRENT_TIMESTAMP WHERE id=1");
        ConnectionMapper mapper = db.session.getMapper(ConnectionMapper.class);
        assertEquals(2, mapper.selectAll().size());
        // Existing duplicate-key semantics: reuse an existing target binding, do not invent a production schema.
        db.sql("ALTER TABLE connection ADD UNIQUE (cluster_id,source_id,sink_id,runtime_id,topic)");
        ConnectionEntity row = connection(1);
        row.setTopic("one");
        row.setRuntimeId(9L);
        mapper.insert(row);
        assertEquals(3, mapper.selectAll().size());
        assertTrue(mapper.selectAll().stream().allMatch(v -> v.getEndTime() == null));
    }

    @Test
    public void logFrontQueryFiltersDeletionWithoutChangingBusinessState() throws Exception {
        db.seed("operation_log", LogEntity.class, 1, Map.of("cluster_id", 7, "state", 2, "is_delete", 0));
        db.seed("operation_log", LogEntity.class, 2, Map.of("cluster_id", 7, "state", 2, "is_delete", 1));
        OprLogMapper mapper = db.session.getMapper(OprLogMapper.class);
        LogEntity query = new LogEntity();
        query.setClusterId(7L);
        assertEquals(1, mapper.getLogListToFront(query).size());
        assertEquals(1, mapper.getLogList(query).size());
    }

    @Test
    public void aclDeletionPreservesInactiveStatusAndStoredTombstones() throws Exception {
        db.seed("acl", AclEntity.class, 1, Map.of("cluster_id", 7, "status", 0, "resource_type", 10));
        db.seed("acl", AclEntity.class, 2, Map.of("cluster_id", 7, "status", 1, "resource_type", 10));
        AclMapper mapper = db.session.getMapper(AclMapper.class);
        AclEntity row = new AclEntity();
        row.setId(1L);
        assertNotNull(mapper.selectById(row));
        mapper.deleteById(row);
        mapper.deleteById(row);
        assertNull(mapper.selectById(row));
        assertEquals(1, mapper.selectAll().size());
        assertEquals(0L, storedValue("SELECT status FROM acl WHERE id=1"));
        assertEquals(1L, storedValue("SELECT is_delete FROM acl WHERE id=1"));
        assertEquals(2L, storedValue("SELECT COUNT(*) FROM acl"));
    }

    @Test
    public void portLookupKeepsInactiveRecordsButExcludesDeletedRecords() throws Exception {
        db.seed("port", PortEntity.class, 1, Map.of("cluster_id", 7, "status", 0));
        PortMapper mapper = db.session.getMapper(PortMapper.class);
        PortEntity query = new PortEntity();
        query.setClusterId(7L);
        assertNotNull(mapper.lockPort(query));
        db.sql("UPDATE port SET is_delete=1 WHERE id=1");
        assertNull(mapper.lockPort(query));
    }

    private long storedValue(String sql) throws Exception {
        try (java.sql.Statement statement = db.session.getConnection().createStatement();
            java.sql.ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private void createRepositoryTable(String table) throws Exception {
        String ddl = new String(getClass().getResourceAsStream("/eventmesh-dashboard.sql").readAllBytes(), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("(?ism)create table `?" + table + "`?\\s*\\(.*?^\\)[^\\n]*;").matcher(ddl);
        assertTrue(table, matcher.find());
        db.sql(matcher.group());
    }

    private void seedConnections() throws Exception {
        db.sql("INSERT INTO connection (id,cluster_id,source_id,source_type,sink_id,sink_type,runtime_id,topic) VALUES"
            + "(1,7,10,1,20,1,9,'one'),(2,7,10,1,20,1,9,'two'),(3,7,10,1,20,1,9,'three')");
    }

    private ConnectionEntity connection(long id) {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(id);
        entity.setClusterId(7L);
        entity.setSourceId(10L);
        entity.setSinkId(20L);
        entity.setSourceType("1");
        entity.setSinkType("1");
        return entity;
    }
}
