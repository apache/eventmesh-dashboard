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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.AclConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterRelationshipConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConnectionConvertMetaData;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.Test;

/** Regression contracts for metadata conversion. */
public class MetadataPipelineRegressionTest {

    @Test
    public void aclConverterMapsOnlyPersistableKafkaResourceAndPermissionCodes() {
        AclEntity entity = new AclEntity();
        entity.setId(101L);
        entity.setClusterId(12L);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 10, 1, 2, 3));
        entity.setCreateUserId(501L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 11, 4, 5, 6));
        entity.setUpdateUserId(502L);
        entity.setStatus(1L);
        entity.setIsDelete(0);
        entity.setPattern("User:orders-reader");
        entity.setHost("*");
        entity.setResourceType(10);
        entity.setResourceName("orders");
        entity.setOperation(3);
        entity.setPermissionType(2);
        entity.setPatternType(1);

        AclMetadata metadata = AclConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals("User:orders-reader", metadata.getPrincipal());
        assertEquals("TOPIC", metadata.getResourceType());
        assertEquals("DENY", metadata.getPermissionType());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        AclEntity roundTrip = AclConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getPattern(), roundTrip.getPattern());
        assertEquals(entity.getResourceType(), roundTrip.getResourceType());
        assertEquals(entity.getPermissionType(), roundTrip.getPermissionType());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
        assertEquals(entity.getOperation(), roundTrip.getOperation());
        assertEquals(entity.getHost(), roundTrip.getHost());
        assertEquals(entity.getResourceName(), roundTrip.getResourceName());
        assertEquals(entity.getPatternType(), roundTrip.getPatternType());

        entity.setResourceType(11);
        entity.setPermissionType(3);
        metadata = AclConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals("GROUP", metadata.getResourceType());
        assertEquals("ALLOW", metadata.getPermissionType());
        assertThrows(IllegalArgumentException.class, () -> {
            entity.setResourceType(12);
            AclConvertMetaData.INSTANCE.toMetaData(entity);
        });
        AclMetadata invalidPermission = AclConvertMetaData.INSTANCE.toMetaData(roundTrip);
        invalidPermission.setPermissionType("ADMIN");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(invalidPermission));
        AclMetadata rocketMqPolicy = new AclMetadata();
        rocketMqPolicy.setPolicyType("CUSTOM");
        assertThrows(IllegalArgumentException.class, () -> AclConvertMetaData.INSTANCE.toEntity(rocketMqPolicy));
    }

    @Test
    public void connectionConverterPreservesClusterRuntimeAndPersistedEndTime() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(202L);
        entity.setClusterId(20L);
        entity.setRuntimeId(2001L);
        entity.setSourceType("connector");
        entity.setSourceId(31L);
        entity.setSinkType("connector");
        entity.setSinkId(32L);
        entity.setTopic("orders");
        entity.setGroupId(40L);
        entity.setDescription("persisted connection");
        entity.setEndTime(Timestamp.valueOf("2026-10-16 13:14:15"));

        ConnectionMetadata metadata = ConnectionConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getRuntimeId(), metadata.getRuntimeId());
        assertEquals("orders", metadata.getTopic());
        assertEquals(LocalDateTime.of(2026, 10, 16, 13, 14, 15), metadata.getEndTime());
        ConnectionEntity roundTrip = ConnectionConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getEndTime(), roundTrip.getEndTime());
        assertEquals(entity.getSourceId(), roundTrip.getSourceId());
        assertEquals(entity.getSinkId(), roundTrip.getSinkId());

        entity.setEndTime(null);
        metadata = ConnectionConvertMetaData.INSTANCE.toMetaData(entity);
        assertNull(metadata.getEndTime());
    }

    @Test
    public void relationshipConverterPreservesEndpointTypesAndRoles() {
        ClusterRelationshipEntity entity = new ClusterRelationshipEntity();
        entity.setId(303L);
        entity.setOrganizationId(7L);
        entity.setCreateTime(LocalDateTime.of(2026, 10, 12, 1, 2, 3));
        entity.setCreateUserId(601L);
        entity.setUpdateTime(LocalDateTime.of(2026, 10, 13, 4, 5, 6));
        entity.setUpdateUserId(602L);
        entity.setClusterId(30L);
        entity.setRelationshipId(3001L);
        entity.setClusterType(ClusterType.EVENTMESH);
        entity.setRelationshipType(ClusterType.STORAGE_KAFKA_RAFT);
        entity.setStatus(1L);
        entity.setIsDelete(0);

        ClusterRelationshipMetadata metadata = ClusterRelationshipConvertMetaData.INSTANCE.toMetaData(entity);
        assertEquals(entity.getId(), metadata.getId());
        assertEquals(entity.getClusterId(), metadata.getClusterId());
        assertEquals(entity.getClusterType(), metadata.getClusterType());
        assertEquals(entity.getRelationshipId(), metadata.getRelationshipId());
        assertEquals(entity.getRelationshipType(), metadata.getRelationshipType());
        assertEquals(entity.getStatus(), metadata.getStatus());
        assertEquals(entity.getOrganizationId(), metadata.getOrganizationId());
        assertEquals(entity.getCreateTime(), metadata.getCreateTime());
        assertEquals(entity.getCreateUserId(), metadata.getCreateUserId());
        assertEquals(entity.getUpdateTime(), metadata.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), metadata.getUpdateUserId());
        assertEquals(entity.getIsDelete(), metadata.getIsDelete());
        ClusterRelationshipEntity roundTrip = ClusterRelationshipConvertMetaData.INSTANCE.toEntity(metadata);
        assertEquals(entity.getId(), roundTrip.getId());
        assertEquals(entity.getClusterId(), roundTrip.getClusterId());
        assertEquals(entity.getClusterType(), roundTrip.getClusterType());
        assertEquals(entity.getRelationshipId(), roundTrip.getRelationshipId());
        assertEquals(entity.getRelationshipType(), roundTrip.getRelationshipType());
        assertEquals(entity.getOrganizationId(), roundTrip.getOrganizationId());
        assertEquals(entity.getCreateTime(), roundTrip.getCreateTime());
        assertEquals(entity.getCreateUserId(), roundTrip.getCreateUserId());
        assertEquals(entity.getUpdateTime(), roundTrip.getUpdateTime());
        assertEquals(entity.getUpdateUserId(), roundTrip.getUpdateUserId());
        assertEquals(entity.getIsDelete(), roundTrip.getIsDelete());
    }

}
