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
import static org.junit.Assert.assertNotNull;

import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Exercises Connection field and status mappings against isolated schemas. */
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
