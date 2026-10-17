/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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

import org.apache.eventmesh.dashboard.console.controller.ConnectionController;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.ConnectionDataService;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ConnectionDataServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Exercises Connection's real controller, service and MyBatis paths against isolated schemas. */
public class ConnectionMysqlChainTest extends EntityMysqlChainSupportTest {

    @Test
    public void mapperInsertPersistsGeneratedIdAndInheritedClusterRuntimeIds() throws Exception {
        try (Fixture fixture = fixture("connection")) {
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            ConnectionEntity connection = connection(101L, 1001L, 11L, 22L, "orders");

            mapper.insert(connection);

            assertNotNull("MyBatis should populate the generated primary key", connection.getId());
            Map<String, Object> row = fixture.jdbc().queryForMap(
                "SELECT cluster_id, runtime_id, source_id, sink_id, topic, status FROM connection WHERE id = ?",
                connection.getId());
            assertEquals(101L, ((Number) row.get("cluster_id")).longValue());
            assertEquals(1001L, ((Number) row.get("runtime_id")).longValue());
            assertEquals(11L, ((Number) row.get("source_id")).longValue());
            assertEquals(22L, ((Number) row.get("sink_id")).longValue());
            assertEquals("orders", row.get("topic"));
            assertEquals(1, ((Number) row.get("status")).intValue());
        }
    }

    @Test
    public void batchInsertWritesEveryConnectionAndItsRuntimeScope() throws Exception {
        try (Fixture fixture = fixture("connection")) {
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            mapper.batchInsert(List.of(connection(201L, 2001L, 11L, 22L, "payments"),
                connection(201L, 2002L, 11L, 22L, "invoices")));

            List<Map<String, Object>> rows = fixture.jdbc().queryForList(
                "SELECT cluster_id, runtime_id, topic FROM connection WHERE cluster_id = ? ORDER BY runtime_id", 201L);
            assertEquals(2, rows.size());
            assertEquals(2001L, ((Number) rows.get(0).get("runtime_id")).longValue());
            assertEquals("payments", rows.get(0).get("topic"));
            assertEquals(2002L, ((Number) rows.get(1).get("runtime_id")).longValue());
            assertEquals("invoices", rows.get(1).get("topic"));
        }
    }

    @Test
    public void clusterQueriesStayScopedAndHonorActiveStatus() throws Exception {
        try (Fixture fixture = fixture("connection")) {
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            mapper.batchInsert(List.of(connection(301L, 3001L, 11L, 22L, "inactive"),
                connection(302L, 3002L, 11L, 22L, "other-cluster")));
            fixture.jdbc().update("UPDATE connection SET status = 0 WHERE cluster_id = ?", 301L);
            mapper.insert(connection(301L, 3003L, 11L, 22L, "visible"));

            ConnectionEntity query = new ConnectionEntity();
            query.setClusterId(301L);
            List<ConnectionEntity> selected = mapper.selectByClusterId(query);
            assertEquals(1, selected.size());
            assertEquals("visible", selected.get(0).getTopic());
            assertEquals(Long.valueOf(3003L), selected.get(0).getRuntimeId());
            assertEquals(Integer.valueOf(1), mapper.selectConnectionNumByCluster(query));
        }
    }

    @Test
    public void createEndpointPersistsResolvedRuntimeAndExistingConnectors() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            fixture.seedRuntime(401L, 40L);
            long sourceId = insertConnector(fixture, 40L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            long sinkId = insertConnector(fixture, 40L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);
            String connectorRowsBeforeCreate = connectorSnapshot(fixture);

            HttpResponse<String> response = fixture.post("/cluster/connection/createConnection",
                createRequest(40L, 401L, "http-create", "[]", "[]"));

            assertEquals(200, response.statusCode());
            assertEquals("success", response.body());
            Map<String, Object> row = fixture.jdbc().queryForMap(
                "SELECT cluster_id, runtime_id, source_type, source_id, sink_type, sink_id, topic, "
                    + "group_id, description "
                    + "FROM connection WHERE cluster_id = ? AND topic = ?", 40L, "http-create");
            assertEquals(40L, ((Number) row.get("cluster_id")).longValue());
            assertEquals(401L, ((Number) row.get("runtime_id")).longValue());
            assertEquals("connector", row.get("source_type"));
            assertEquals(sourceId, ((Number) row.get("source_id")).longValue());
            assertEquals("connector", row.get("sink_type"));
            assertEquals(sinkId, ((Number) row.get("sink_id")).longValue());
            assertEquals("http-create", row.get("topic"));
            assertEquals(33L, ((Number) row.get("group_id")).longValue());
            assertEquals("description-http-create", row.get("description"));
            HttpResponse<String> nullConfigResponse = fixture.post("/cluster/connection/createConnection",
                "{\"clusterId\":40,\"runtimeId\":401,\"addConnectionDTO\":{"
                    + "\"sinkName\":\"sink\",\"sourceName\":\"source\",\"topicName\":\"null-config\","
                    + "\"connectionDescription\":\"description-null-config\",\"sourceClass\":\"SourceClass\","
                    + "\"sinkClass\":\"SinkClass\",\"sourceHost\":\"127.0.0.1\",\"sourcePort\":7001,"
                    + "\"sinkHost\":\"127.0.0.2\",\"sinkPort\":7002},\"addConnectorConfigDTO\":null}");
            assertEquals(200, nullConfigResponse.statusCode());
            assertEquals("success", nullConfigResponse.body());
            assertEquals(2, fixture.jdbc().queryForObject("SELECT COUNT(*) FROM connection", Integer.class).intValue());
            assertEquals(connectorRowsBeforeCreate, connectorSnapshot(fixture));
        }
    }

    @Test
    public void createRejectsInvalidRuntimeConnectorsAndConfigChangesWithoutWrites() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            fixture.seedRuntime(501L, 50L);
            fixture.seedRuntime(502L, 51L);
            fixture.jdbc().update("UPDATE runtime SET status = 0 WHERE id = ?", 502L);
            insertConnector(fixture, 50L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 50L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);

            assertBadRequestWithoutWrites(fixture, createRequest(50L, 999L, "missing-runtime", "[]", "[]"));
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 502L, "inactive-runtime", "[]", "[]"));
            assertBadRequestWithoutWrites(fixture, createRequest(51L, 501L, "wrong-cluster-runtime", "[]", "[]"));
            assertBadRequestWithoutWrites(fixture,
                createRequest(50L, 501L, "missing-source", "absent", "sink", "[]", "[]"));
            fixture.jdbc().update("UPDATE connector SET status = 0 WHERE cluster_id = ? AND name = ?", 50L, "source");
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "inactive-source", "[]", "[]"));
            fixture.jdbc().update("UPDATE connector SET status = 1 WHERE cluster_id = ? AND name = ?", 50L, "source");
            assertBadRequestWithoutWrites(fixture,
                createRequest(50L, 501L, "missing-sink", "source", "absent", "[]", "[]"));
            fixture.jdbc().update("UPDATE connector SET status = 0 WHERE cluster_id = ? AND name = ?", 50L, "sink");
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "inactive-sink", "[]", "[]"));
            fixture.jdbc().update("UPDATE connector SET status = 1 WHERE cluster_id = ? AND name = ?", 50L, "sink");

            insertConnector(fixture, 50L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "ambiguous-source", "[]", "[]"));
            insertConnector(fixture, 50L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "ambiguous-sink", "[]", "[]"));

            fixture.jdbc().update("DELETE FROM connector WHERE cluster_id = ?", 50L);
            insertConnector(fixture, 50L, "source", "Sink", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 50L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "wrong-role-source", "[]", "[]"));

            fixture.jdbc().update("DELETE FROM connector WHERE cluster_id = ?", 50L);
            insertConnector(fixture, 50L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 50L, "sink", "Source", "SinkClass", "127.0.0.2", 7002);
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "wrong-role-sink", "[]", "[]"));

            fixture.jdbc().update("DELETE FROM connector WHERE cluster_id = ?", 50L);
            insertConnector(fixture, 51L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 51L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            assertBadRequestWithoutWrites(fixture, createRequest(50L, 501L, "cross-cluster-connectors", "[]", "[]"));

            fixture.jdbc().update("DELETE FROM connector WHERE cluster_id = ?", 51L);
            insertConnector(fixture, 50L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 50L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            assertBadRequestWithoutWrites(fixture,
                createRequest(50L, 501L, "nonempty-config", "[{\"name\":\"x\",\"value\":\"y\"}]", "[]"));
        }
    }

    @Test
    public void listEndpointHydratesSameClusterConnectorsAndFiltersByClusterAndTopic() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            long sourceId = insertConnector(fixture, 601L, "source-601", "Source", "SourceClass", "127.0.0.1", 7001);
            long sinkId = insertConnector(fixture, 601L, "sink-601", "Sink", "SinkClass", "127.0.0.2", 7002);
            long otherSourceId = insertConnector(fixture, 602L, "source-602", "Source", "OtherSourceClass",
                "127.0.0.3", 7003);
            long otherSinkId = insertConnector(fixture, 602L, "sink-602", "Sink", "OtherSinkClass", "127.0.0.4", 7004);
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            mapper.insert(connection(601L, 6001L, sourceId, sinkId, "visible-topic"));
            mapper.insert(connection(602L, 6002L, otherSourceId, otherSinkId, "other-topic"));
            mapper.insert(connection(601L, 6001L, otherSourceId, sinkId, "cross-cluster-reference"));
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);

            HttpResponse<String> response = fixture.post("/cluster/connection/getConnectionList",
                "{\"clusterId\":601,\"topicName\":\"visible-topic\"}");

            assertEquals(200, response.statusCode());
            JsonNode filtered = new ObjectMapper().readTree(response.body());
            assertEquals(1, filtered.size());
            assertEquals(Long.valueOf(sinkId).longValue(), filtered.get(0).get("sinkConnectorId").longValue());
            assertEquals("sink-601", filtered.get(0).get("sinkConnectorName").asText());
            assertEquals("SinkClass", filtered.get(0).get("sinkClass").asText());
            assertEquals(Long.valueOf(sourceId).longValue(), filtered.get(0).get("sourceConnectorId").longValue());
            assertEquals("source-601", filtered.get(0).get("sourceConnectorName").asText());
            assertEquals("SourceClass", filtered.get(0).get("sourceClass").asText());
            assertEquals("visible-topic", filtered.get(0).get("topicName").asText());

            JsonNode unfiltered = new ObjectMapper().readTree(fixture.post(
                "/cluster/connection/getConnectionList", "{\"clusterId\":601}").body());
            assertEquals(2, unfiltered.size());
            JsonNode crossClusterReference = new ObjectMapper().readTree(fixture.post(
                "/cluster/connection/getConnectionList",
                "{\"clusterId\":601,\"topicName\":\"cross-cluster-reference\"}").body()).get(0);
            assertTrue("Source from another cluster must not be hydrated: " + crossClusterReference,
                crossClusterReference.get("sourceConnectorId").isNull()
                    && crossClusterReference.get("sourceConnectorName").isNull()
                    && crossClusterReference.get("sourceClass").isNull());
            assertEquals(sinkId, crossClusterReference.get("sinkConnectorId").longValue());
        }
    }

    @Test
    public void listDoesNotHydrateNonConnectorIdsAsConnectorRows() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            long connectorId = insertConnector(fixture, 701L, "same-id", "Source", "SourceClass", "127.0.0.1", 7001);
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            ConnectionEntity row = connection(701L, 7001L, connectorId, connectorId, "legacy-types");
            row.setSourceType("client");
            row.setSinkType("connector-v2");
            mapper.insert(row);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);

            HttpResponse<String> response = fixture.post("/cluster/connection/getConnectionList",
                "{\"clusterId\":701}");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().startsWith("[") && response.body().contains("legacy-types"));
            assertTrue("Non-connector resource IDs must not hydrate connector names: " + response.body(),
                !response.body().contains("same-id") && !response.body().contains("SourceClass"));
        }
    }

    @Test
    public void serviceInsertParticipatesInOuterTransactionRollback() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);
            ConnectionEntity inserted = connection(801L, 8001L, 81L, 82L, "rollback-check");

            try {
                fixture.transactions().execute(status -> {
                    fixture.bean(ConnectionDataService.class).insert(inserted);
                    throw new IllegalStateException("force outer transaction rollback");
                });
            } catch (IllegalStateException expected) {
                assertEquals("force outer transaction rollback", expected.getMessage());
            }

            assertEquals(0, fixture.jdbc().queryForObject(
                "SELECT COUNT(*) FROM connection WHERE cluster_id = ? AND topic = ?", Integer.class,
                801L, "rollback-check").intValue());
            assertNotNull("The mapper may have assigned an ID before the transaction rolled back", inserted.getId());
        }
    }

    @Test
    public void createStorageFailureReturnsServerErrorAndDoesNotReportSuccess() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            fixture.seedRuntime(901L, 90L);
            insertConnector(fixture, 90L, "source", "Source", "SourceClass", "127.0.0.1", 7001);
            insertConnector(fixture, 90L, "sink", "Sink", "SinkClass", "127.0.0.2", 7002);
            fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);
            fixture.jdbc().execute("CREATE TRIGGER fail_connection_insert BEFORE INSERT ON connection "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced storage failure'");
            String connectorRowsBeforeRequest = connectorSnapshot(fixture);

            HttpResponse<String> response = fixture.post("/cluster/connection/createConnection",
                createRequest(90L, 901L, "storage-failure", "[]", "[]"));

            assertEquals("A persistence failure must not be reported as success: " + response.body(),
                500, response.statusCode());
            assertFalse(response.body().contains("success"));
            assertEquals(0, fixture.jdbc().queryForObject("SELECT COUNT(*) FROM connection", Integer.class).intValue());
            assertEquals(connectorRowsBeforeRequest, connectorSnapshot(fixture));
        }
    }

    private static void assertBadRequestWithoutWrites(Fixture fixture, String request) throws Exception {
        String connectorRowsBeforeRequest = connectorSnapshot(fixture);
        HttpResponse<String> response = fixture.post("/cluster/connection/createConnection", request);
        assertEquals("Expected rejected request: " + request + ", body: " + response.body(),
            400, response.statusCode());
        assertEquals(0, fixture.jdbc().queryForObject("SELECT COUNT(*) FROM connection", Integer.class).intValue());
        assertEquals("Rejected creation must not change connector records", connectorRowsBeforeRequest,
            connectorSnapshot(fixture));
    }

    private static String connectorSnapshot(Fixture fixture) {
        return fixture.jdbc().queryForObject("SELECT GROUP_CONCAT(CONCAT_WS(':', id, cluster_id, name, class_name, "
            + "type, "
            + "host, port, status, config_ids) ORDER BY id SEPARATOR '|') FROM connector", String.class);
    }

    private static long insertConnector(Fixture fixture, long clusterId, String name, String role,
        String className, String host, int port) {
        fixture.jdbc().update("INSERT INTO connector (cluster_id, name, class_name, type, host, port, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, 1)", clusterId, name, className, role, host, port);
        return fixture.jdbc().queryForObject(
            "SELECT id FROM connector WHERE cluster_id = ? AND name = ? ORDER BY id DESC LIMIT 1",
            Long.class, clusterId, name);
    }

    private static String createRequest(long clusterId, long runtimeId, String topic, String sourceConfigs,
        String sinkConfigs) {
        return createRequest(clusterId, runtimeId, topic, "source", "sink", sourceConfigs, sinkConfigs);
    }

    private static String createRequest(long clusterId, long runtimeId, String topic, String sourceName,
        String sinkName, String sourceConfigs, String sinkConfigs) {
        return "{\"clusterId\":" + clusterId + ",\"runtimeId\":" + runtimeId + ",\"addConnectionDTO\":{"
            + "\"sinkName\":\"" + sinkName + "\",\"sourceName\":\"" + sourceName + "\",\"topicName\":\"" + topic + "\","
            + "\"connectionDescription\":\"description-" + topic + "\",\"sourceClass\":\"SourceClass\","
            + "\"sinkClass\":\"SinkClass\",\"sourceHost\":\"127.0.0.1\",\"sourcePort\":7001,"
            + "\"sinkHost\":\"127.0.0.2\",\"sinkPort\":7002,\"groupId\":33},"
            + "\"addConnectorConfigDTO\":{\"sourceConnectorConfigs\":" + sourceConfigs
            + ",\"sinkConnectorConfigs\":" + sinkConfigs + "}}";
    }

    private static ConnectionEntity connection(long clusterId, long runtimeId, long sourceId, long sinkId,
        String topic) {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setClusterId(clusterId);
        entity.setRuntimeId(runtimeId);
        entity.setSourceType("connector");
        entity.setSourceId(sourceId);
        entity.setSinkType("connector");
        entity.setSinkId(sinkId);
        entity.setTopic(topic);
        entity.setGroupId(33L);
        entity.setDescription("description-" + topic);
        return entity;
    }
}
