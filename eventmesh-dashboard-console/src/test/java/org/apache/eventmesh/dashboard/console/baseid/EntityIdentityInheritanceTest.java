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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.common.model.metadata.NetConnectionMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseClusterIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.NetConnectionEntity;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.NetConnectionMetaData;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.junit.Test;

public class EntityIdentityInheritanceTest {

    @Test
    public void identityStorageExistsOnlyOnTheBaseEntities() {
        assertEquals(1, fieldsInHierarchy(AclEntity.class, "id").size());
        assertSame(BaseIdEntity.class, fieldsInHierarchy(AclEntity.class, "id").get(0).getDeclaringClass());
        assertEquals(1, fieldsInHierarchy(ConnectionEntity.class, "clusterId").size());
        assertSame(BaseClusterIdEntity.class, fieldsInHierarchy(ConnectionEntity.class, "clusterId").get(0).getDeclaringClass());
        assertEquals(1, fieldsInHierarchy(ConnectionEntity.class, "runtimeId").size());
        assertSame(BaseRuntimeIdEntity.class, fieldsInHierarchy(ConnectionEntity.class, "runtimeId").get(0).getDeclaringClass());
        assertEquals(1, fieldsInHierarchy(NetConnectionEntity.class, "clusterId").size());
        assertSame(BaseClusterIdEntity.class, fieldsInHierarchy(NetConnectionEntity.class, "clusterId").get(0).getDeclaringClass());
        assertEquals(1, fieldsInHierarchy(NetConnectionEntity.class, "runtimeId").size());
        assertSame(BaseRuntimeIdEntity.class, fieldsInHierarchy(NetConnectionEntity.class, "runtimeId").get(0).getDeclaringClass());
        assertEquals(1, fieldsInHierarchy(AclEntity.class, "clusterId").size());
        assertSame(AclEntity.class, fieldsInHierarchy(AclEntity.class, "clusterId").get(0).getDeclaringClass());
    }

    @Test
    public void inheritedPropertiesRemainAvailableToBeansJacksonAndMyBatis() throws Exception {
        AclEntity acl = new AclEntity();
        assertNull(acl.getId());
        acl.setId(7L);
        assertEquals(Long.valueOf(7L), acl.getId());

        ConnectionEntity connection = new ConnectionEntity();
        assertNull(connection.getClusterId());
        assertNull(connection.getRuntimeId());
        connection.setClusterId(11L);
        connection.setRuntimeId(13L);
        assertEquals(Long.valueOf(11L), connection.getClusterId());
        assertEquals(Long.valueOf(13L), connection.getRuntimeId());
        connection.setClusterId(null);
        connection.setRuntimeId(null);
        assertNull(connection.getClusterId());
        assertNull(connection.getRuntimeId());
        connection.setClusterId(11L);
        connection.setRuntimeId(13L);

        NetConnectionEntity netConnection = new NetConnectionEntity();
        netConnection.setClusterId(17L);
        netConnection.setRuntimeId(19L);
        assertEquals(Long.valueOf(17L), netConnection.getClusterId());
        assertEquals(Long.valueOf(19L), netConnection.getRuntimeId());

        MetaClass metaClass = MetaClass.forClass(ConnectionEntity.class, new DefaultReflectorFactory());
        assertTrue(metaClass.hasGetter("clusterId"));
        assertTrue(metaClass.hasSetter("clusterId"));
        assertTrue(metaClass.hasGetter("runtimeId"));
        assertTrue(metaClass.hasSetter("runtimeId"));
        assertTrue(MetaClass.forClass(NetConnectionEntity.class, new DefaultReflectorFactory()).hasGetter("clusterId"));
        assertTrue(MetaClass.forClass(AclEntity.class, new DefaultReflectorFactory()).hasSetter("id"));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(connection);
        assertEquals(11L, json.get("clusterId").asLong());
        assertEquals(13L, json.get("runtimeId").asLong());
        ConnectionEntity populated = mapper.readValue("{\"clusterId\":11,\"runtimeId\":13}", ConnectionEntity.class);
        assertEquals(Long.valueOf(11L), populated.getClusterId());
        assertEquals(Long.valueOf(13L), populated.getRuntimeId());
        ConnectionEntity restored = mapper.readValue("{\"clusterId\":null,\"runtimeId\":null}", ConnectionEntity.class);
        assertNull(restored.getClusterId());
        assertNull(restored.getRuntimeId());
        assertEquals(7L, mapper.valueToTree(acl).get("id").asLong());
        assertEquals(17L, mapper.valueToTree(netConnection).get("clusterId").asLong());
    }

    @Test
    public void inheritedIdentityParticipatesInEqualityAndHashing() {
        ConnectionEntity first = connection(23L, 29L, "same");
        ConnectionEntity equal = connection(23L, 29L, "same");
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());

        ConnectionEntity changedCluster = connection(31L, 29L, "same");
        ConnectionEntity changedRuntime = connection(23L, 37L, "same");
        assertNotEquals(first, changedCluster);
        assertNotEquals(first, changedRuntime);
        // These selected values show inherited identities affect hash computation; they do not imply collision-free hashing.
        assertNotEquals(Integer.valueOf(first.hashCode()), Integer.valueOf(changedCluster.hashCode()));
        assertNotEquals(Integer.valueOf(first.hashCode()), Integer.valueOf(changedRuntime.hashCode()));

        NetConnectionEntity net = new NetConnectionEntity();
        NetConnectionEntity netEqual = new NetConnectionEntity();
        net.setClusterId(41L);
        netEqual.setClusterId(41L);
        net.setRuntimeId(43L);
        netEqual.setRuntimeId(43L);
        assertEquals(net, netEqual);
        assertEquals(net.hashCode(), netEqual.hashCode());
        NetConnectionEntity changedNetCluster = new NetConnectionEntity();
        changedNetCluster.setClusterId(47L);
        changedNetCluster.setRuntimeId(43L);
        assertNotEquals(net, changedNetCluster);
        assertNotEquals(Integer.valueOf(net.hashCode()), Integer.valueOf(changedNetCluster.hashCode()));
        NetConnectionEntity changedNetRuntime = new NetConnectionEntity();
        changedNetRuntime.setClusterId(41L);
        changedNetRuntime.setRuntimeId(47L);
        assertNotEquals(net, changedNetRuntime);
        assertNotEquals(Integer.valueOf(net.hashCode()), Integer.valueOf(changedNetRuntime.hashCode()));
        netEqual.setRuntimeId(47L);
        assertNotEquals(net, netEqual);

        AclEntity acl = new AclEntity();
        AclEntity aclEqual = new AclEntity();
        acl.setId(53L);
        aclEqual.setId(53L);
        acl.setClusterId(59L);
        aclEqual.setClusterId(59L);
        assertEquals(acl, aclEqual);
        aclEqual.setId(61L);
        assertNotEquals(acl, aclEqual);
        assertNotEquals(Integer.valueOf(acl.hashCode()), Integer.valueOf(aclEqual.hashCode()));
    }

    @Test
    public void netConnectionMetadataMappingPreservesInheritedIdentityAndNullInput() {
        NetConnectionEntity entity = new NetConnectionEntity();
        entity.setId(67L);
        entity.setClusterId(71L);
        entity.setRuntimeId(73L);

        NetConnectionMetadata metadata = NetConnectionMetaData.INSTANCE.toMetaData(entity);
        assertEquals(Long.valueOf(67L), metadata.getId());
        assertEquals(Long.valueOf(71L), metadata.getClusterId());
        assertEquals(Long.valueOf(73L), metadata.getRuntimeId());

        NetConnectionEntity restored = NetConnectionMetaData.INSTANCE.toEntity(metadata);
        assertEquals(Long.valueOf(67L), restored.getId());
        assertEquals(Long.valueOf(71L), restored.getClusterId());
        assertEquals(Long.valueOf(73L), restored.getRuntimeId());
        assertNull(NetConnectionMetaData.INSTANCE.toMetaData(null));
        assertNull(NetConnectionMetaData.INSTANCE.toEntity(null));
    }

    @Test
    public void connectionClusterIdSchemaDescriptionRemainsPresent() {
        Schema schema = null;
        for (Class<?> type = ConnectionEntity.class; type != null; type = type.getSuperclass()) {
            try {
                schema = type.getDeclaredField("clusterId").getAnnotation(Schema.class);
            } catch (NoSuchFieldException ignored) {
                // The inherited field may carry its annotation on the base property.
            }
            if (schema != null) {
                break;
            }
        }
        if (schema == null) {
            schema = Arrays.stream(ConnectionEntity.class.getMethods())
                .filter(method -> "getClusterId".equals(method.getName()))
                .map(method -> method.getAnnotation(Schema.class))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
        }
        assertNotNull("clusterId OpenAPI schema metadata must remain available", schema);
        assertEquals("runtime cluster id", schema.description());
    }

    private static ConnectionEntity connection(Long clusterId, Long runtimeId, String topic) {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setClusterId(clusterId);
        entity.setRuntimeId(runtimeId);
        entity.setTopic(topic);
        return entity;
    }

    private static List<Field> fieldsInHierarchy(Class<?> type, String name) {
        java.util.ArrayList<Field> fields = new java.util.ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Arrays.stream(current.getDeclaredFields())
                .filter(field -> name.equals(field.getName()))
                .forEach(fields::add);
        }
        return fields;
    }
}
