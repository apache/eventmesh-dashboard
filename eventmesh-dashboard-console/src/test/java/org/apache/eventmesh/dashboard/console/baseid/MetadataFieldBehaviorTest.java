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
import static org.junit.Assert.assertSame;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RocketMQAclAccountMetadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MetadataFieldBehaviorTest {

    @Test
    public void aclKeepsLegacyPrincipalAndAcl2ResourceIdentitiesDistinct() {
        AclMetadata legacy = new AclMetadata();
        assertNull(legacy.nodeUnique());
        legacy.setPrincipal("User:alice");
        legacy.setOperation(4);
        legacy.setPermissionType("PUB");
        legacy.setHost("127.0.0.1");
        legacy.setResourceType("Topic");
        legacy.setResourceName("orders");
        legacy.setPatternType(1);
        assertEquals("User:alice", legacy.nodeUnique());
        assertEquals(Integer.valueOf(4), legacy.getOperation());
        assertEquals("PUB", legacy.getPermissionType());
        assertEquals("127.0.0.1", legacy.getHost());
        assertEquals(Integer.valueOf(1), legacy.getPatternType());

        AclMetadata implicitCustom = acl2("subject", null, "Topic", "orders");
        implicitCustom.setActions(new ArrayList<String>());
        AclMetadata explicitCustom = acl2("subject", "Custom", "Topic", "orders");
        assertEquals(implicitCustom.nodeUnique(), explicitCustom.nodeUnique());
        assertNotEquals(implicitCustom.nodeUnique(), acl2("subject", "Custom", "Group", "orders").nodeUnique());
        assertNotEquals(implicitCustom.nodeUnique(), acl2("subject", "Custom", "Topic", "payments").nodeUnique());
        assertNotEquals(implicitCustom.nodeUnique(), acl2("other", "Custom", "Topic", "orders").nodeUnique());

        AclMetadata delimiterLeft = acl2("a|1:b", "Custom", "Topic", "x");
        AclMetadata delimiterRight = acl2("a", "1:b|Custom", "Topic", "x");
        assertNotEquals("length-prefixed ACL identity parts must not collide", delimiterLeft.nodeUnique(), delimiterRight.nodeUnique());
        AclMetadata nullParts = acl2(null, null, null, null);
        nullParts.setActions(new ArrayList<String>());
        AclMetadata emptyParts = acl2("", "", "", "");
        assertEquals("-1:6:Custom-1:-1:", nullParts.nodeUnique());
        assertEquals("0:0:0:0:", emptyParts.nodeUnique());
        assertNotEquals(nullParts.nodeUnique(), emptyParts.nodeUnique());
        assertEquals("Custom", explicitCustom.getPolicyType());
        assertEquals("orders", explicitCustom.getResourceName());
    }

    @Test
    public void connectionIdentityAndInheritedScopeRemainAvailable() {
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setClusterId(12L);
        metadata.setRuntimeId(13L);
        metadata.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        metadata.setSourceType("client");
        metadata.setSourceId(20L);
        metadata.setSourceHost("source-host");
        metadata.setSourcePort(9090);
        metadata.setSourceName("source");
        metadata.setSinkType("connector");
        metadata.setSinkId(30L);
        metadata.setSinkHost("sink-host");
        metadata.setSinkPort(9091);
        metadata.setSinkName("sink");
        metadata.setTopic("orders");
        metadata.setGroupId(40L);
        metadata.setDescription("description");

        assertEquals("12/20/30/orders", metadata.nodeUnique());
        assertEquals(Long.valueOf(12L), metadata.getClusterId());
        assertEquals(Long.valueOf(13L), metadata.getRuntimeId());
        assertSame(ClusterType.STORAGE_ROCKETMQ_CLUSTER, metadata.getClusterType());
        assertEquals("client", metadata.getSourceType());
        assertEquals(Integer.valueOf(9090), metadata.getSourcePort());
        assertEquals("connector", metadata.getSinkType());
        assertEquals(Integer.valueOf(9091), metadata.getSinkPort());
        assertEquals(Long.valueOf(40L), metadata.getGroupId());
        assertEquals("description", metadata.getDescription());
        assertSingleFieldInHierarchy(ConnectionMetadata.class, "clusterId", BaseClusterIdBase.class);
        assertSingleFieldInHierarchy(ConnectionMetadata.class, "clusterType", BaseClusterIdBase.class);
        assertSingleFieldInHierarchy(ConnectionMetadata.class, "runtimeId", BaseRuntimeIdBase.class);
    }

    @Test
    public void relationshipEndpointsAndRelationshipAttributesRemainIndependent() {
        ClusterRelationshipMetadata relationship = new ClusterRelationshipMetadata();
        relationship.setClusterId(51L);
        relationship.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        relationship.setRelationshipId(61L);
        relationship.setRelationshipType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);

        assertEquals("61-51", relationship.nodeUnique());
        assertEquals(Long.valueOf(51L), relationship.getClusterId());
        assertSame(ClusterType.EVENTMESH_CLUSTER, relationship.getClusterType());
        assertEquals(Long.valueOf(61L), relationship.getRelationshipId());
        assertSame(ClusterType.STORAGE_ROCKETMQ_CLUSTER, relationship.getRelationshipType());
        assertSingleFieldInHierarchy(ClusterRelationshipMetadata.class, "clusterId", BaseClusterIdBase.class);
        assertSingleFieldInHierarchy(ClusterRelationshipMetadata.class, "clusterType", BaseClusterIdBase.class);
        assertEquals(0, fieldsInHierarchy(ClusterRelationshipMetadata.class, "runtimeId").size());
        assertEquals(1, fieldsInHierarchy(ClusterRelationshipMetadata.class, "relationshipId").size());
        assertEquals(1, fieldsInHierarchy(ClusterRelationshipMetadata.class, "relationshipType").size());
    }

    @Test
    public void rocketMqAclSubclassStillUsesItsAccountIdentity() {
        RocketMQAclAccountMetadata account = new RocketMQAclAccountMetadata();
        account.setAccessKey("access");
        account.setClusterId(71L);
        account.setRuntimeId(72L);
        account.setPolicyType("Custom");
        assertEquals("RocketMQAclV1:access", account.nodeUnique());
        assertEquals(Long.valueOf(71L), account.getClusterId());
        assertEquals(Long.valueOf(72L), account.getRuntimeId());
        assertEquals("Custom", account.getPolicyType());
    }

    private static AclMetadata acl2(String principal, String policyType, String resourceType, String resourceName) {
        AclMetadata metadata = new AclMetadata();
        metadata.setPrincipal(principal);
        metadata.setPolicyType(policyType);
        metadata.setResourceType(resourceType);
        metadata.setResourceName(resourceName);
        return metadata;
    }

    private static void assertSingleFieldInHierarchy(Class<?> type, String fieldName, Class<?> declaringClass) {
        List<Field> fields = fieldsInHierarchy(type, fieldName);
        assertEquals(fieldName, 1, fields.size());
        assertSame(fieldName, declaringClass, fields.get(0).getDeclaringClass());
    }

    private static List<Field> fieldsInHierarchy(Class<?> type, String fieldName) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (fieldName.equals(field.getName())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}
