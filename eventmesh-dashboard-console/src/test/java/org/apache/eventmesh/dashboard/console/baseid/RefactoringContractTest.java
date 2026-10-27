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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.InstanceUserMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.QueueMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.controller.cluster.RuntimeController;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryClusterByOrganizationIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RefactoringContractTest {

    @Test
    public void identityIsNeverCachedAcrossIdChanges() {
        ClusterMetadata metadata = new ClusterMetadata();
        assertThrows(IllegalStateException.class, metadata::getUnique);
        metadata.setId(10L);
        assertEquals("ClusterMetadata-10", metadata.getUnique());
        metadata.setId(11L);
        assertEquals("ClusterMetadata-11", metadata.getUnique());
        metadata.setId(null);
        assertThrows(IllegalStateException.class, metadata::getUnique);
    }

    @Test
    public void deletionAndInsertionAreDisjointAndStoppingIsNotDeleting() {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setStatus(0L);
        assertFalse(cluster.isDelete());
        cluster.setIsDelete(1);
        assertTrue(cluster.isDelete());
        TopicMetadata topic = new TopicMetadata();
        assertFalse(topic.isInsert());
        assertFalse(topic.isDelete());
        topic.setStatus(0L);
        LocalDateTime time = LocalDateTime.of(2026, 10, 21, 0, 0);
        topic.setCreateTime(time);
        topic.setUpdateTime(time);
        assertFalse(topic.isDelete());
        assertTrue(topic.isInsert());
        assertFalse(topic.isUpdate());
        topic.setStatus(1L);
        assertTrue(topic.isInsert());
        topic.setIsDelete(1);
        assertFalse(topic.isInsert());
        assertFalse(topic.isUpdate());
    }

    @Test
    public void namesAndRelationshipScopeMatchJsonContracts() throws Exception {
        ObjectMapper json = new ObjectMapper();
        InstanceUserMetadata user = json.readValue("{\"id\":1,\"name\":\"alice\"}", InstanceUserMetadata.class);
        assertEquals("alice", user.nodeUnique());
        assertEquals("alice", json.valueToTree(user).get("name").asText());
        assertFalse(json.valueToTree(user).has("userName"));
        QueueMetadata queue = new QueueMetadata();
        queue.setId(2L);
        queue.setTopicName("orders");
        queue.setName("0");
        assertEquals("orders-0", queue.nodeUnique());
        assertEquals("0", json.valueToTree(queue).get("name").asText());
        assertFalse(org.apache.ibatis.reflection.SystemMetaObject.forObject(new ClusterRelationshipMetadata()).hasGetter("runtimeId"));
        QueryClusterByOrganizationIdAndTypeDTO query = json.readValue("{\"name\":\"exact\"}",
            QueryClusterByOrganizationIdAndTypeDTO.class);
        assertEquals("exact", ClusterControllerMapper.INSTANCE.queryClusterByOrganizationIdAndType(query).getName());
    }

    @Test
    public void healthResponseDoesNotMutateBusinessState() {
        RuntimeEntity entity = new RuntimeEntity();
        entity.setId(999123L);
        entity.setStatus(7L);
        entity.setName("node");
        RuntimeService service = mock(RuntimeService.class);
        when(service.queryRuntimeToFrontByClusterId(any())).thenReturn(List.of(entity));
        RuntimeController controller = new RuntimeController();
        ReflectionTestUtils.setField(controller, "runtimeService", service);
        var result = controller.queryRuntimeListByClusterId(new ClusterIdDTO()).get(0);
        assertEquals(Long.valueOf(7), result.getStatus());
        assertEquals(Long.valueOf(7), entity.getStatus());
        assertEquals("node", result.getName());
        assertNotNull(result.getHealthStatus());
    }
}
