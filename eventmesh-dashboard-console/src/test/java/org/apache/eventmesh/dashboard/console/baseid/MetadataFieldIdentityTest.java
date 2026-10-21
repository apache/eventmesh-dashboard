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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RocketMQAclAccountMetadata;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataFieldIdentityTest {

    @Test
    public void duplicatedIdentityFieldsHaveOneInheritedStorageLocation() throws Exception {
        assertSame(BaseRuntimeIdBase.class, AclMetadata.class.getSuperclass());
        assertSame(BaseRuntimeIdBase.class, ConnectionMetadata.class.getSuperclass());
        assertSame(BaseClusterIdBase.class, ClusterRelationshipMetadata.class.getSuperclass());
        assertFieldOwners(AclMetadata.class, "clusterId", BaseClusterIdBase.class);
        assertFieldOwners(ConnectionMetadata.class, "runtimeId", BaseRuntimeIdBase.class);
        assertFieldOwners(ClusterRelationshipMetadata.class, "clusterType", BaseClusterIdBase.class);
        assertFieldOwners(ConnectionMetadata.class, "clusterId", BaseClusterIdBase.class);
        assertFalse(org.apache.ibatis.reflection.SystemMetaObject.forObject(new ClusterRelationshipMetadata()).hasGetter("runtimeId"));
        assertFieldOwners(ClusterRelationshipMetadata.class, "relationshipId", ClusterRelationshipMetadata.class);
        assertFieldOwners(ClusterRelationshipMetadata.class, "relationshipType", ClusterRelationshipMetadata.class);
    }

    @Test
    public void inheritedFieldsKeepBeanMyBatisAndJacksonContracts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AclMetadata acl = mapper.readValue("{\"id\":1,\"status\":1,\"clusterId\":2,\"runtimeId\":3,"
            + "\"principal\":\"alice\"}", AclMetadata.class);
        assertEquals(Long.valueOf(2L), acl.getClusterId());
        assertEquals(Long.valueOf(3L), acl.getRuntimeId());
        JsonNode aclJson = mapper.valueToTree(acl);
        assertEquals(2L, aclJson.get("clusterId").asLong());
        assertEquals(3L, aclJson.get("runtimeId").asLong());

        ConnectionMetadata connection = mapper.readValue("{\"id\":4,\"status\":1,\"clusterId\":5,"
            + "\"runtimeId\":6,\"topic\":\"orders\"}", ConnectionMetadata.class);
        assertEquals(Long.valueOf(5L), connection.getClusterId());
        assertEquals(Long.valueOf(6L), connection.getRuntimeId());
        assertEquals("orders", connection.getTopic());
        JsonNode connectionJson = mapper.valueToTree(connection);
        assertEquals(5L, connectionJson.get("clusterId").asLong());
        assertEquals(6L, connectionJson.get("runtimeId").asLong());

        ClusterRelationshipMetadata relationship = mapper.readValue("{\"id\":7,\"status\":1,"
            + "\"clusterId\":8,\"clusterType\":\"EVENTMESH_CLUSTER\","
            + "\"relationshipId\":10,\"relationshipType\":\"EVENTMESH_RUNTIME\"}",
            ClusterRelationshipMetadata.class);
        assertEquals(ClusterType.EVENTMESH_CLUSTER, relationship.getClusterType());
        assertEquals(ClusterType.EVENTMESH_RUNTIME, relationship.getRelationshipType());
        JsonNode relationshipJson = mapper.valueToTree(relationship);
        assertEquals("EVENTMESH_CLUSTER", relationshipJson.get("clusterType").asText());
        assertEquals("EVENTMESH_RUNTIME", relationshipJson.get("relationshipType").asText());

        for (Class<?> type : Arrays.<Class<?>>asList(AclMetadata.class, ConnectionMetadata.class,
            ClusterRelationshipMetadata.class)) {
            MetaClass metaClass = MetaClass.forClass(type, new DefaultReflectorFactory());
            List<String> beanProperties = Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(PropertyDescriptor::getName).collect(Collectors.toList());
            for (String property : type == ClusterRelationshipMetadata.class
                ? Arrays.asList("clusterId", "clusterType") : Arrays.asList("clusterId", "clusterType", "runtimeId")) {
                assertTrue(type.getSimpleName() + " missing MyBatis getter " + property,
                    metaClass.hasGetter(property));
                assertTrue(type.getSimpleName() + " missing MyBatis setter " + property,
                    metaClass.hasSetter(property));
                assertTrue(type.getSimpleName() + " missing JavaBean property " + property,
                    beanProperties.contains(property));
            }
        }
    }

    @Test
    public void inheritedStorageAndAccessorsAgreeForNullAndNonNullValues() throws Exception {
        AclMetadata acl = new AclMetadata();
        ConnectionMetadata connection = new ConnectionMetadata();
        ClusterRelationshipMetadata relationship = new ClusterRelationshipMetadata();
        assertNull(readField(BaseClusterIdBase.class, "clusterId", acl));
        assertNull(readField(BaseRuntimeIdBase.class, "runtimeId", connection));
        assertNull(readField(BaseClusterIdBase.class, "clusterType", relationship));

        acl.setClusterId(12L);
        connection.setRuntimeId(23L);
        relationship.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        assertEquals(acl.getClusterId(), readField(BaseClusterIdBase.class, "clusterId", acl));
        assertEquals(connection.getRuntimeId(), readField(BaseRuntimeIdBase.class, "runtimeId", connection));
        assertEquals(relationship.getClusterType(), readField(BaseClusterIdBase.class, "clusterType", relationship));

        acl.setClusterId(null);
        connection.setRuntimeId(null);
        relationship.setClusterType(null);
        assertNull(readField(BaseClusterIdBase.class, "clusterId", acl));
        assertNull(readField(BaseRuntimeIdBase.class, "runtimeId", connection));
        assertNull(readField(BaseClusterIdBase.class, "clusterType", relationship));
    }

    @Test
    public void clusterAndRelationshipTypesRemainIndependentAndNodeIdentityIsPreserved() {
        ClusterRelationshipMetadata relationship = new ClusterRelationshipMetadata();
        relationship.setClusterId(42L);
        relationship.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        relationship.setRelationshipId(9L);
        relationship.setRelationshipType(ClusterType.EVENTMESH_RUNTIME);
        assertNotEquals(relationship.getClusterType(), relationship.getRelationshipType());
        assertEquals("9-42", relationship.nodeUnique());

        ConnectionMetadata connection = new ConnectionMetadata();
        connection.setClusterId(42L);
        connection.setSourceId(3L);
        connection.setSinkId(4L);
        connection.setTopic("orders");
        assertEquals("42/3/4/orders", connection.nodeUnique());

        AclMetadata acl = new AclMetadata();
        acl.setPrincipal("alice");
        assertEquals("alice", acl.nodeUnique());
    }

    @Test
    public void rocketMqAclSubclassInheritsSingleClusterIdContract() throws Exception {
        RocketMQAclAccountMetadata account = new RocketMQAclAccountMetadata();
        account.setClusterId(51L);
        account.setAccessKey("account");
        assertEquals(Long.valueOf(51L), account.getClusterId());
        assertEquals(Long.valueOf(51L), readField(BaseClusterIdBase.class, "clusterId", account));
        assertEquals("RocketMQAclV1:account", account.nodeUnique());
        assertEquals(1, countField(RocketMQAclAccountMetadata.class, "clusterId"));
    }

    @Test
    public void metadataEqualityRetainsObjectIdentityContract() {
        AclMetadata firstAcl = new AclMetadata();
        firstAcl.setId(1L);
        firstAcl.setClusterId(2L);
        AclMetadata secondAcl = new AclMetadata();
        secondAcl.setId(1L);
        secondAcl.setClusterId(2L);
        assertTrue(firstAcl.equals(firstAcl));
        assertFalse(firstAcl.equals(secondAcl));
        assertFalse(firstAcl.equals(null));
        assertFalse(firstAcl.equals(new Object()));

        ConnectionMetadata firstConnection = new ConnectionMetadata();
        firstConnection.setId(3L);
        firstConnection.setRuntimeId(4L);
        ConnectionMetadata secondConnection = new ConnectionMetadata();
        secondConnection.setId(3L);
        secondConnection.setRuntimeId(4L);
        assertTrue(firstConnection.equals(firstConnection));
        assertFalse(firstConnection.equals(secondConnection));
        assertFalse(firstConnection.equals(null));
        assertFalse(firstConnection.equals(new Object()));

        ClusterRelationshipMetadata firstRelationship = new ClusterRelationshipMetadata();
        firstRelationship.setId(5L);
        firstRelationship.setRelationshipId(6L);
        ClusterRelationshipMetadata secondRelationship = new ClusterRelationshipMetadata();
        secondRelationship.setId(5L);
        secondRelationship.setRelationshipId(6L);
        assertTrue(firstRelationship.equals(firstRelationship));
        assertFalse(firstRelationship.equals(secondRelationship));
        assertFalse(firstRelationship.equals(null));
        assertFalse(firstRelationship.equals(new Object()));
    }

    @Test
    public void hashCodeAndToStringRetainMetadataContracts() {
        AclMetadata emptyAcl = new AclMetadata();
        assertHashCodeIsStable(emptyAcl);
        assertTrue(emptyAcl.toString().contains("principal=null"));

        AclMetadata acl = new AclMetadata();
        acl.setPrincipal("alice");
        acl.setOperation(1);
        acl.setPermissionType("PUB");
        acl.setHost("127.0.0.1");
        acl.setResourceType("TOPIC");
        acl.setResourceName("orders");
        acl.setPatternType(1);
        acl.setPolicyType("Custom");
        acl.setActions(new ArrayList<>(Arrays.asList("PUB", "SUB")));
        acl.setSourceIps(new ArrayList<>(Arrays.asList("10.0.0.1")));
        assertHashCodeIsStable(acl);
        int aclHash = acl.hashCode();
        assertTrue(acl.toString().contains("principal=alice"));
        acl.setClusterId(11L);
        assertEquals(aclHash, acl.hashCode());
        acl.setClusterId(null);
        assertEquals(aclHash, acl.hashCode());

        ConnectionMetadata emptyConnection = new ConnectionMetadata();
        assertHashCodeIsStable(emptyConnection);
        assertTrue(emptyConnection.toString().contains("topic=null"));

        ConnectionMetadata connection = new ConnectionMetadata();
        connection.setSourceType("connector");
        connection.setSourceId(12L);
        connection.setSourceHost("source.local");
        connection.setSourcePort(1234);
        connection.setSourceName("source");
        connection.setSinkType("client");
        connection.setSinkHost("sink.local");
        connection.setSinkPort(4321);
        connection.setSinkName("sink");
        connection.setSinkId(13L);
        connection.setTopic("orders");
        connection.setGroupId(14L);
        connection.setDescription("route description");
        assertHashCodeIsStable(connection);
        int connectionHash = connection.hashCode();
        assertTrue(connection.toString().contains("topic=orders"));
        connection.setRuntimeId(15L);
        assertEquals(connectionHash, connection.hashCode());
        connection.setRuntimeId(null);
        assertEquals(connectionHash, connection.hashCode());

        ClusterRelationshipMetadata emptyRelationship = new ClusterRelationshipMetadata();
        assertHashCodeIsStable(emptyRelationship);
        assertTrue(emptyRelationship.toString().contains("relationshipId=null"));

        ClusterRelationshipMetadata relationship = new ClusterRelationshipMetadata();
        relationship.setRelationshipId(16L);
        relationship.setRelationshipType(ClusterType.EVENTMESH_RUNTIME);
        assertHashCodeIsStable(relationship);
        int relationshipHash = relationship.hashCode();
        assertTrue(relationship.toString().contains("relationshipType=EVENTMESH_RUNTIME"));
        relationship.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        assertEquals(relationshipHash, relationship.hashCode());
        relationship.setClusterType(null);
        assertEquals(relationshipHash, relationship.hashCode());
    }

    private void assertHashCodeIsStable(Object value) {
        int first = value.hashCode();
        assertEquals(first, value.hashCode());
    }

    private void assertFieldOwners(Class<?> type, String name, Class<?> expectedOwner) throws Exception {
        assertSame("Unexpected declaration owner for " + type.getSimpleName() + "." + name,
            expectedOwner, declaringClass(type, name));
        assertEquals("Duplicate declaration for " + type.getSimpleName() + "." + name, 1,
            countField(type, name));
    }

    private Class<?> declaringClass(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                current.getDeclaredField(name);
                return current;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private int countField(Class<?> type, String name) {
        int count = 0;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private Object readField(Class<?> owner, String name, Object instance) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

}
