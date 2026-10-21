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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.model.DO.cluster.ClusterRelationQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RefactoringMapperTest {
    private final BaseIdDatabaseTest db = new BaseIdDatabaseTest();
    private final LocalDateTime boundary = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Before
    public void open() throws Exception {
        db.database();
    }

    @After
    public void close() throws Exception {
        db.close();
    }

    @Test
    public void topicDeleteBatchDeleteSyncDeleteAndRecreationUseSameMarker() throws Exception {
        db.seed("topic", TopicEntity.class, 1, Map.of("topic_name", "orders"));
        TopicMapper mapper = db.session.getMapper(TopicMapper.class);
        TopicEntity query = new TopicEntity();
        query.setUpdateTime(boundary);
        TopicEntity row = mapper.syncGet(query).get(0);
        LocalDateTime before = row.getUpdateTime();
        mapper.deleteTopicById(row);
        assertNull(mapper.queryTopicById(row));
        TopicEntity deleted = mapper.syncGet(query).get(0);
        assertEquals(Integer.valueOf(1), deleted.getIsDelete());
        assertTrue(deleted.getUpdateTime().isAfter(before));
        mapper.deleteTopicById(row);
        mapper.insertTopic(row);
        assertNotNull(mapper.queryTopicById(row));
        mapper.deleteTopicByIds(List.of(row));
        assertNull(mapper.queryTopicById(row));
        mapper.batchInsert(List.of(row));
        assertNotNull(mapper.queryTopicById(row));
        mapper.syncDelete(List.of(row));
        assertNull(mapper.queryTopicById(row));
        assertEquals(1, mapper.syncGet(query).size());
    }

    @Test
    public void groupDeleteActuallyHidesRowAndRecreationClearsMarker() throws Exception {
        db.seed("group", GroupEntity.class, 2, Map.of("name", "consumers"));
        GroupMapper mapper = db.session.getMapper(GroupMapper.class);
        GroupEntity query = new GroupEntity();
        query.setUpdateTime(boundary);
        GroupEntity row = mapper.syncGet(query).get(0);
        mapper.deleteGroup(row);
        assertNull(mapper.selectGroupById(row));
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
        mapper.addGroup(row);
        assertNotNull(mapper.selectGroupById(row));
        mapper.syncDelete(List.of(row));
        assertNull(mapper.selectGroupById(row));
        mapper.batchInsert(List.of(row));
        assertNotNull(mapper.selectGroupById(row));
    }

    @Test
    public void memberDeletionIsSeparateFromInactiveState() throws Exception {
        db.seed("group_member", GroupMemberEntity.class, 3, Map.of("topic_name", "orders", "group_name", "consumers"));
        GroupMemberMapper mapper = db.session.getMapper(GroupMemberMapper.class);
        GroupMemberEntity query = new GroupMemberEntity();
        query.setUpdateTime(boundary);
        GroupMemberEntity row = mapper.syncGet(query).get(0);
        assertEquals(1, mapper.deleteGroupMember(row));
        assertNull(mapper.selectGroupMemberById(row));
        assertTrue(mapper.selectMember(new GroupMemberEntity()).isEmpty());
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
        mapper.addGroupMember(row);
        assertNotNull(mapper.selectGroupMemberById(row));
        mapper.syncDelete(List.of(row));
        mapper.batchInsert(List.of(row));
        assertNotNull(mapper.selectGroupMemberById(row));
        db.sql("update group_member set status=0, is_delete=0 where id=3");
        assertNotNull(mapper.selectGroupMemberById(row));
        assertEquals(1, mapper.syncGet(query).size());
    }

    @Test
    public void inactiveRecordsRemainStoredAndExplicitActiveQueriesStillFilterThem() throws Exception {
        db.seed("topic", TopicEntity.class, 5, Map.of("topic_name", "legacy-topic"));
        db.seed("group", GroupEntity.class, 6, Map.of("name", "legacy-group"));
        db.seed("config", ConfigEntity.class, 7, Map.of("config_name", "legacy-config", "is_default", 0,
            "instance_type", MetadataType.CLUSTER.name()));
        db.sql("update topic set status=0, is_delete=0 where id=5");
        db.sql("update `group` set status=0, is_delete=0 where id=6");
        db.sql("update config set status=0, is_delete=0 where id=7");

        TopicEntity topicQuery = new TopicEntity();
        topicQuery.setUpdateTime(boundary);
        TopicMapper topicMapper = db.session.getMapper(TopicMapper.class);
        TopicEntity topicId = new TopicEntity();
        topicId.setId(5L);
        assertNotNull(topicMapper.queryTopicById(topicId));
        assertEquals(Integer.valueOf(0), topicMapper.syncGet(topicQuery).get(0).getIsDelete());

        GroupMapper groupMapper = db.session.getMapper(GroupMapper.class);
        GroupEntity groupQuery = new GroupEntity();
        groupQuery.setId(6L);
        assertNull(groupMapper.selectGroupById(groupQuery));
        groupQuery.setId(null);
        groupQuery.setUpdateTime(boundary);
        assertEquals(Integer.valueOf(0), groupMapper.syncGet(groupQuery).get(0).getIsDelete());

        ConfigMapper configMapper = db.session.getMapper(ConfigMapper.class);
        assertTrue(configMapper.selectAll().isEmpty());
        ConfigEntity configQuery = new ConfigEntity();
        configQuery.setUpdateTime(boundary);
        assertEquals(Integer.valueOf(0), configMapper.syncGet(configQuery).get(0).getIsDelete());
    }

    @Test
    public void configDeletionAndRecreationPreserveIncrementalVisibility() throws Exception {
        db.seed("config", ConfigEntity.class, 4, Map.of("config_name", "retention", "is_default", 0,
            "instance_type", MetadataType.CLUSTER.name()));
        ConfigMapper mapper = db.session.getMapper(ConfigMapper.class);
        ConfigEntity query = new ConfigEntity();
        query.setUpdateTime(boundary);
        ConfigEntity row = mapper.syncGet(query).get(0);
        mapper.deleteConfig(row);
        assertTrue(mapper.selectAll().isEmpty());
        assertEquals(Integer.valueOf(1), mapper.syncGet(query).get(0).getIsDelete());
        mapper.batchInsert(List.of(row));
        assertEquals(1, mapper.selectAll().size());
        mapper.syncDelete(List.of(row));
        assertTrue(mapper.selectAll().isEmpty());
        mapper.addConfig(row);
        assertEquals(1, mapper.selectAll().size());
        assertEquals(1, mapper.syncGet(query).size());
    }

    @Test
    public void clusterNameAndBothRelationshipTypesAreIndependentFilters() throws Exception {
        db.seed("cluster", ClusterEntity.class, 10, Map.of("name", "exact", "organization_id", 1));
        db.seed("cluster", ClusterEntity.class, 20, Map.of("name", "exact-other", "organization_id", 1));
        ClusterMapper mapper = db.session.getMapper(ClusterMapper.class);
        ClusterEntity query = new ClusterEntity();
        query.setOrganizationId(1L);
        query.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        query.setName("exact");
        assertEquals(1, mapper.queryClusterByOrganizationIdAndType(query).size());
        query.setName(null);
        assertEquals(2, mapper.queryClusterByOrganizationIdAndType(query).size());
        db.seed("cluster_relationship", org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity.class,
            30, Map.of("cluster_id", 10, "relationship_id", 20, "cluster_type", "EVENTMESH_RUNTIME",
                "relationship_type", "STORAGE_KAFKA_BROKER"));
        ClusterRelationQuery relation = new ClusterRelationQuery();
        relation.setId(10L);
        relation.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        relation.setRelationshipType(ClusterType.STORAGE_KAFKA_BROKER);
        assertEquals(1, mapper.queryRelationClusterByClusterIdAndType(relation).size());
        relation.setRelationshipType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        assertTrue(mapper.queryRelationClusterByClusterIdAndType(relation).isEmpty());
        relation.setRelationshipType(ClusterType.STORAGE_KAFKA_BROKER);
        relation.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        assertTrue(mapper.queryRelationClusterByClusterIdAndType(relation).isEmpty());
    }
}
