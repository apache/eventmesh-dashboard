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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.eventmesh.dashboard.console.controller.cluster.AclController;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.AclServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/** Exercises the original Acl SQL and HTTP handlers against an isolated MySQL schema. */
public class AclMysqlChainTest extends EntityMysqlChainSupportTest {

    @Test
    public void mapperInsertReturnsGeneratedIdAndReadsInheritedIdentityAndFields() throws Exception {
        try (Fixture fixture = fixture("acl")) {
            AclMapper mapper = fixture.mapper(AclMapper.class);
            AclEntity inserted = acl(4101L, "mapper-orders", "broker-a");

            mapper.insert(inserted);

            assertNotNull("MySQL generated key should be written back to BaseIdEntity.id", inserted.getId());
            AclEntity loaded = mapper.selectById(inserted);
            assertEquals(inserted.getId(), loaded.getId());
            assertEquals(inserted.getClusterId(), loaded.getClusterId());
            assertEquals(inserted.getPattern(), loaded.getPattern());
            assertEquals(inserted.getOperation(), loaded.getOperation());
            assertEquals(inserted.getPermissionType(), loaded.getPermissionType());
            assertEquals(inserted.getHost(), loaded.getHost());
            assertEquals(inserted.getResourceType(), loaded.getResourceType());
            assertEquals(inserted.getResourceName(), loaded.getResourceName());
            assertEquals(inserted.getPatternType(), loaded.getPatternType());
        }
    }

    @Test
    public void mapperRowsKeepDifferentClustersAndGeneratedKeysDistinct() throws Exception {
        try (Fixture fixture = fixture("acl")) {
            AclMapper mapper = fixture.mapper(AclMapper.class);
            AclEntity first = acl(5101L, "shared-pattern", "broker-a");
            AclEntity second = acl(5102L, "shared-pattern", "broker-a");

            mapper.insert(first);
            mapper.insert(second);

            assertNotNull(first.getId());
            assertNotNull(second.getId());
            assertNotEquals(first.getId(), second.getId());
            assertEquals(Long.valueOf(5101L), mapper.selectById(first).getClusterId());
            assertEquals(Long.valueOf(5102L), mapper.selectById(second).getClusterId());
            assertEquals(2, mapper.selectAll().size());
        }
    }

    @Test
    public void httpInsertUpdateAndDeleteChangeMysqlRow() throws Exception {
        try (Fixture fixture = fixture("acl")) {
            fixture.mapper(AclMapper.class);
            fixture.startHttp(AclController.class, AclServiceImpl.class);
            String insertJson = "{\"clusterId\":6101,\"pattern\":\"http-orders\",\"operation\":1,"
                    + "\"permissionType\":2,\"host\":\"broker-http\",\"resourceType\":3,"
                    + "\"resourceName\":\"orders\",\"patternType\":4}";
            assertEquals(200, fixture.post("/acl/insertAcl", insertJson).statusCode());

            Long id = fixture.jdbc().queryForObject(
                    "SELECT id FROM acl WHERE cluster_id = ? AND pattern = ?", Long.class, 6101L, "http-orders");
            assertNotNull(id);
            assertEquals(Integer.valueOf(3), fixture.jdbc().queryForObject(
                    "SELECT resource_type FROM acl WHERE id = ?", Integer.class, id));

            String updateJson = "{\"id\":" + id + ",\"resourceType\":9}";
            assertEquals(200, fixture.post("/acl/updateAcl", updateJson).statusCode());
            assertEquals(Integer.valueOf(9), fixture.jdbc().queryForObject(
                    "SELECT resource_type FROM acl WHERE id = ?", Integer.class, id));

            assertEquals(200, fixture.post("/acl/deleteAcl", "{\"id\":" + id + "}").statusCode());
            assertEquals(Long.valueOf(0L), fixture.jdbc().queryForObject(
                    "SELECT status FROM acl WHERE id = ?", Long.class, id));
            assertEquals(Integer.valueOf(1), fixture.jdbc().queryForObject(
                    "SELECT COUNT(*) FROM acl WHERE id = ?", Integer.class, id));
        }
    }

    @Test
    public void injectedControllerAndServicePersistAclWithoutHttpConversion() throws Exception {
        try (Fixture fixture = fixture("acl")) {
            fixture.mapper(AclMapper.class);
            fixture.startHttp(AclController.class, AclServiceImpl.class);
            AclController controller = fixture.bean(AclController.class);
            AclServiceImpl service = fixture.bean(AclServiceImpl.class);
            AclEntity entity = acl(8101L, "direct-controller", "broker-direct");

            controller.insertAcl(entity);

            assertNotNull("The real mapper should return the generated key to the entity", entity.getId());
            assertEquals(Long.valueOf(8101L), fixture.jdbc().queryForObject(
                    "SELECT cluster_id FROM acl WHERE id = ?", Long.class, entity.getId()));
            AclEntity loaded = service.selectById(entity);
            assertEquals("direct-controller", loaded.getPattern());
            assertEquals(Integer.valueOf(3), loaded.getResourceType());

            entity.setResourceType(10);
            controller.updateAcl(entity);
            assertEquals(Integer.valueOf(10), fixture.jdbc().queryForObject(
                    "SELECT resource_type FROM acl WHERE id = ?", Integer.class, entity.getId()));

            controller.deleteAcl(entity);
            assertEquals(Integer.valueOf(0), fixture.jdbc().queryForObject(
                    "SELECT status FROM acl WHERE id = ?", Integer.class, entity.getId()));
        }
    }

    @Test
    public void httpSelectReturnsTheSelectedRecord() throws Exception {
        try (Fixture fixture = fixture("acl")) {
            AclMapper mapper = fixture.mapper(AclMapper.class);
            AclEntity inserted = acl(7101L, "http-read", "broker-read");
            mapper.insert(inserted);
            fixture.startHttp(AclController.class, AclServiceImpl.class);

            String response = fixture.post("/acl/selectAcl", "{\"id\":" + inserted.getId() + "}").body();
            JsonNode selected = new ObjectMapper().readTree(response);

            assertEquals(inserted.getId().longValue(), selected.get("id").longValue());
            assertEquals(7101L, selected.get("clusterId").longValue());
            assertEquals("http-read", selected.get("pattern").asText());
            assertEquals("broker-read", selected.get("host").asText());
            assertEquals(1, selected.get("operation").intValue());
            assertEquals(2, selected.get("permissionType").intValue());
            assertEquals(3, selected.get("resourceType").intValue());
            assertEquals("resource-http-read", selected.get("resourceName").asText());
            assertEquals(4, selected.get("patternType").intValue());
        }
    }

    private static AclEntity acl(long clusterId, String pattern, String host) {
        AclEntity entity = new AclEntity();
        entity.setClusterId(clusterId);
        entity.setPattern(pattern);
        entity.setOperation(1);
        entity.setPermissionType(2);
        entity.setHost(host);
        entity.setResourceType(3);
        entity.setResourceName("resource-" + pattern);
        entity.setPatternType(4);
        return entity;
    }
}
