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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.InstanceUserEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.entity.function.LogEntity;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RemainingEntityIdentityTest {

    @Test
    public void identityFieldsAreDeclaredOnceOnTheirBaseTypes() {
        assertFieldOwner(InstanceUserEntity.class, "id", BaseIdEntity.class);
        assertFieldOwner(InstanceUserEntity.class, "clusterId", BaseRuntimeIdEntity.class.getSuperclass());
        assertFieldOwner(InstanceUserEntity.class, "runtimeId", BaseRuntimeIdEntity.class);
        assertFieldOwner(ConnectorEntity.class, "id", BaseIdEntity.class);
        assertFieldOwner(ConnectorEntity.class, "clusterId", BaseRuntimeIdEntity.class.getSuperclass());
        assertFieldOwner(ConnectorEntity.class, "runtimeId", BaseRuntimeIdEntity.class);
        assertFieldOwner(LogEntity.class, "id", BaseIdEntity.class);
        assertFieldOwner(LogEntity.class, "clusterId", BaseRuntimeIdEntity.class.getSuperclass());
        assertFieldOwner(LogEntity.class, "runtimeId", BaseRuntimeIdEntity.class);
    }

    @Test
    public void inheritedIdentityRetainsBeanJacksonAndMyBatisProperties() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InstanceUserEntity instanceUser = mapper.readValue("{\"id\":1,\"clusterId\":2,\"runtimeId\":3}",
            InstanceUserEntity.class);
        assertEquals(Long.valueOf(1L), instanceUser.getId());
        assertEquals(Long.valueOf(2L), instanceUser.getClusterId());
        assertEquals(Long.valueOf(3L), instanceUser.getRuntimeId());
        instanceUser.setClusterId(null);
        assertNull(instanceUser.getClusterId());
        instanceUser.setClusterId(2L);

        ConnectorEntity connector = mapper.readValue("{\"id\":4,\"clusterId\":5,\"runtimeId\":6}",
            ConnectorEntity.class);
        assertEquals(Long.valueOf(4L), connector.getId());
        assertEquals(Long.valueOf(5L), connector.getClusterId());
        assertEquals(Long.valueOf(6L), connector.getRuntimeId());
        JsonNode connectorJson = mapper.valueToTree(connector);
        assertEquals(4L, connectorJson.get("id").asLong());
        assertEquals(5L, connectorJson.get("clusterId").asLong());
        assertEquals(6L, connectorJson.get("runtimeId").asLong());

        LogEntity log = log(7L, 8L, 9L);
        JsonNode logJson = mapper.valueToTree(log);
        assertEquals(7L, logJson.get("id").asLong());
        assertEquals(8L, logJson.get("clusterId").asLong());
        assertEquals(9L, logJson.get("runtimeId").asLong());
        LogEntity restored = mapper.readValue("{\"id\":7,\"clusterId\":8,\"runtimeId\":9}", LogEntity.class);
        assertEquals(Long.valueOf(7L), restored.getId());
        assertEquals(Long.valueOf(8L), restored.getClusterId());
        assertEquals(Long.valueOf(9L), restored.getRuntimeId());

        for (Class<?> type : Arrays.<Class<?>>asList(InstanceUserEntity.class, ConnectorEntity.class, LogEntity.class)) {
            MetaClass metaClass = MetaClass.forClass(type, new DefaultReflectorFactory());
            List<String> beanProperties = Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(PropertyDescriptor::getName).collect(java.util.stream.Collectors.toList());
            for (String property : Arrays.asList("id", "clusterId", "runtimeId")) {
                assertTrue(type.getSimpleName() + " must expose getter " + property, metaClass.hasGetter(property));
                assertTrue(type.getSimpleName() + " must expose setter " + property, metaClass.hasSetter(property));
                assertTrue(type.getSimpleName() + " JavaBean property missing: " + property, beanProperties.contains(property));
            }
        }
    }

    @Test
    public void parentStorageAndChildAccessStayConsistentForNullAndValues() throws Exception {
        InstanceUserEntity user = instanceUser(1L, null, null);
        ConnectorEntity connector = connector(1L, null, null);
        LogEntity log = log(1L, null, null);
        assertParentClusterValue(user, null);
        assertParentClusterValue(connector, null);
        assertParentClusterValue(log, null);
        assertNull(user.getRuntimeId());
        assertNull(connector.getRuntimeId());
        assertNull(log.getRuntimeId());

        user.setClusterId(10L);
        connector.setClusterId(20L);
        log.setClusterId(30L);
        user.setRuntimeId(11L);
        connector.setRuntimeId(21L);
        log.setRuntimeId(31L);
        assertParentClusterValue(user, 10L);
        assertParentClusterValue(connector, 20L);
        assertParentClusterValue(log, 30L);
        assertEquals(Long.valueOf(11L), user.getRuntimeId());
        assertEquals(Long.valueOf(21L), connector.getRuntimeId());
        assertEquals(Long.valueOf(31L), log.getRuntimeId());
    }

    @Test
    public void inheritedIdentityParticipatesInEqualityAndHashCode() {
        InstanceUserEntity user1 = instanceUser(1L, 2L, 3L);
        InstanceUserEntity user2 = instanceUser(1L, 2L, 3L);
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        user2.setClusterId(4L);
        assertNotEquals(user1, user2);
        user2.setClusterId(2L);
        user2.setRuntimeId(4L);
        assertNotEquals(user1, user2);
        InstanceUserEntity userDifferentCluster = instanceUser(1L, 4L, 3L);
        assertNotEquals(user1.hashCode(), userDifferentCluster.hashCode());

        ConnectorEntity connector1 = connector(11L, 12L, 13L);
        ConnectorEntity connector2 = connector(11L, 12L, 13L);
        assertEquals(connector1, connector2);
        assertEquals(connector1.hashCode(), connector2.hashCode());
        connector2.setId(14L);
        assertNotEquals(connector1, connector2);
        ConnectorEntity connectorDifferentCluster = connector(11L, 14L, 13L);
        assertNotEquals(connector1.hashCode(), connectorDifferentCluster.hashCode());

        LogEntity log1 = log(21L, 22L, 23L);
        LogEntity log2 = log(21L, 22L, 23L);
        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());
        log2.setClusterId(24L);
        assertNotEquals(log1, log2);
        LogEntity logDifferentCluster = log(21L, 24L, 23L);
        LogEntity logDifferentId = log(24L, 22L, 23L);
        assertNotEquals(log1.hashCode(), logDifferentCluster.hashCode());
        assertNotEquals(log1, logDifferentId);
        assertNotEquals(log1.hashCode(), logDifferentId.hashCode());
    }

    @Test
    public void logEntityKeepsNoArgumentAndNineArgumentConstructors() {
        LogEntity empty = new LogEntity();
        assertNull(empty.getId());
        Timestamp endTime = new Timestamp(123456L);
        LogEntity populated = new LogEntity(31L, 32L, "create", "connector", 1, "content", endTime, "operator", "ok");
        assertEquals(Long.valueOf(31L), populated.getId());
        assertEquals(Long.valueOf(32L), populated.getClusterId());
        assertEquals("create", populated.getOperationType());
        assertEquals("connector", populated.getTargetType());
        assertEquals(Integer.valueOf(1), populated.getState());
        assertEquals("content", populated.getContent());
        assertEquals(endTime, populated.getEndTime());
        assertEquals("operator", populated.getOperationUser());
        assertEquals("ok", populated.getResult());
        LogEntity allNull = new LogEntity(null, null, null, null, null, null, null, null, null);
        assertNull(allNull.getId());
        assertNull(allNull.getClusterId());
        assertNull(allNull.getRuntimeId());
    }

    private static InstanceUserEntity instanceUser(Long id, Long clusterId, Long runtimeId) {
        InstanceUserEntity entity = new InstanceUserEntity();
        entity.setId(id);
        entity.setClusterId(clusterId);
        entity.setRuntimeId(runtimeId);
        return entity;
    }

    private static ConnectorEntity connector(Long id, Long clusterId, Long runtimeId) {
        ConnectorEntity entity = new ConnectorEntity();
        entity.setId(id);
        entity.setClusterId(clusterId);
        entity.setRuntimeId(runtimeId);
        return entity;
    }

    private static LogEntity log(Long id, Long clusterId, Long runtimeId) {
        LogEntity entity = new LogEntity(id, clusterId, "create", "connector", 1, "content",
            new Timestamp(123456L), "operator", "ok");
        entity.setRuntimeId(runtimeId);
        return entity;
    }

    private static void assertParentClusterValue(Object entity, Long expected) throws Exception {
        Field clusterId = entity.getClass().getSuperclass().getSuperclass().getDeclaredField("clusterId");
        clusterId.setAccessible(true);
        assertEquals(expected, clusterId.get(entity));
        assertEquals(expected, entity.getClass().getMethod("getClusterId").invoke(entity));
    }

    private static void assertFieldOwner(Class<?> type, String name, Class<?> owner) {
        List<Field> matches = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (name.equals(field.getName())) {
                    matches.add(field);
                }
            }
        }
        assertEquals(type.getSimpleName() + "." + name + " must be stored once", 1, matches.size());
        assertEquals(owner, matches.get(0).getDeclaringClass());
    }
}
