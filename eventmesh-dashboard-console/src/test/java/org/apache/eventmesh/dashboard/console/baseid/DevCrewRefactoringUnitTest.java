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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMemberMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.function.report.collect.CollectManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.Collect;
import org.apache.eventmesh.dashboard.console.function.report.collect.MetadataDataManage;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConfigConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.TopicConvertMetaData;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DevCrewRefactoringUnitTest {

    @Test
    public void renamedTopicAndConfigPropertiesRoundTripThroughJsonAndLegacySqlAccessors() throws Exception {
        ObjectMapper json = new ObjectMapper();
        TopicEntity topic = json.readValue("{\"id\":7,\"name\":\"orders\"}", TopicEntity.class);
        ConfigEntity config = json.readValue("{\"id\":8,\"name\":\"retention\"}", ConfigEntity.class);

        assertEquals("orders", topic.getName());
        assertEquals("orders", topic.getTopicName());
        assertEquals("retention", config.getName());
        assertEquals("retention", config.getConfigName());
        assertEquals("orders", SystemMetaObject.forObject(topic).getValue("topicName"));
        assertEquals("retention", SystemMetaObject.forObject(config).getValue("configName"));

        JsonNode topicJson = json.valueToTree(topic);
        JsonNode configJson = json.valueToTree(config);
        assertEquals("orders", topicJson.get("name").asText());
        assertFalse(topicJson.has("topicName"));
        assertEquals("retention", configJson.get("name").asText());
        assertFalse(configJson.has("configName"));
    }

    @Test
    public void metadataConvertersPreserveRenamedNamesAndSyncStateBothWays() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 20, 12, 30);
        LocalDateTime updated = created.plusMinutes(5);

        TopicMetadata topicMetadata = new TopicMetadata();
        topicMetadata.setId(21L);
        topicMetadata.setName("payments");
        topicMetadata.setRuntimeId(31L);
        topicMetadata.setStatus(0L);
        topicMetadata.setIsDelete(0);
        topicMetadata.setCreateTime(created);
        topicMetadata.setUpdateTime(updated);
        TopicEntity topicEntity = TopicConvertMetaData.INSTANCE.toEntity(topicMetadata);
        assertNotNull(topicEntity);
        assertEquals("payments", topicEntity.getName());
        assertEquals(Long.valueOf(31L), topicEntity.getRuntimeId());
        assertEquals(Long.valueOf(0L), topicEntity.getStatus());
        assertEquals(Integer.valueOf(0), topicEntity.getIsDelete());
        assertEquals(created, topicEntity.getCreateTime());
        assertEquals(updated, topicEntity.getUpdateTime());
        TopicMetadata topicCopy = TopicConvertMetaData.INSTANCE.toMetaData(topicEntity);
        assertEquals("payments", topicCopy.getName());
        assertEquals(Long.valueOf(0L), topicCopy.getStatus());
        assertFalse("Inactive records are not logically deleted", topicCopy.isDelete());

        ConfigMetadata configMetadata = new ConfigMetadata();
        configMetadata.setId(22L);
        configMetadata.setName("max.in.flight");
        configMetadata.setConfigValue("5");
        configMetadata.setRuntimeId(32L);
        configMetadata.setInstanceType(MetadataType.TOPIC);
        configMetadata.setInstanceId(99L);
        configMetadata.setStatus(1L);
        configMetadata.setIsDelete(1);
        configMetadata.setCreateTime(created);
        configMetadata.setUpdateTime(updated);
        ConfigEntity configEntity = ConfigConvertMetaData.INSTANCE.toEntity(configMetadata);
        assertNotNull(configEntity);
        assertEquals("max.in.flight", configEntity.getName());
        assertEquals("max.in.flight", configEntity.getConfigName());
        assertEquals("5", configEntity.getConfigValue());
        assertEquals(Long.valueOf(32L), configEntity.getRuntimeId());
        assertEquals(MetadataType.TOPIC, configEntity.getInstanceType());
        assertEquals(Long.valueOf(99L), configEntity.getInstanceId());
        assertEquals(Integer.valueOf(1), configEntity.getIsDelete());
        ConfigMetadata configCopy = ConfigConvertMetaData.INSTANCE.toMetaData(configEntity);
        assertEquals("max.in.flight", configCopy.getName());
        assertEquals(Long.valueOf(1L), configCopy.getStatus());
        assertEquals(Integer.valueOf(1), configCopy.getIsDelete());
        assertEquals(created, configCopy.getCreateTime());
        assertEquals(updated, configCopy.getUpdateTime());
        assertTrue(configCopy.isDelete());
        assertNull("Non-persisted instance name must not be invented by the DB converter", configCopy.getInstanceName());
    }

    @Test
    public void businessStatusIsIndependentOfDeletionAcrossMetadataTypes() {
        TopicMetadata topic = new TopicMetadata();
        ConfigMetadata config = new ConfigMetadata();
        GroupMetadata group = new GroupMetadata();
        GroupMemberMetadata member = new GroupMemberMetadata();
        for (var metadata : List.of(topic, config, group, member)) {
            metadata.setStatus(1L);
            metadata.setIsDelete(0);
            assertFalse(metadata.getClass().getSimpleName(), metadata.isDelete());
            metadata.setStatus(0L);
            assertFalse(metadata.getClass().getSimpleName(), metadata.isDelete());
            metadata.setIsDelete(1);
            metadata.setStatus(1L);
            assertTrue(metadata.getClass().getSimpleName(), metadata.isDelete());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reportCollectorRemovesStoppedAndDeletedRowsButRetainsOtherBusinessStates() {
        CollectManage manager = new CollectManage();
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(20L);
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
        cluster.setStatus(1L);
        manager.handlerData(List.of(cluster), List.of(), List.of());

        RuntimeMetadata active = runtime(30L, 20L, "active", 1L, 0);
        RuntimeMetadata stopped = runtime(31L, 20L, "stopped", 0L, 0);
        RuntimeMetadata deleted = runtime(32L, 20L, "deleted", 1L, 1);
        RuntimeMetadata otherState = runtime(33L, 20L, "other-state", 2L, 0);
        manager.handlerData(List.of(), List.of(active, stopped, deleted, otherState), List.of());

        Map<Long, Map<String, RuntimeMetadata>> runtimeRows =
            (Map<Long, Map<String, RuntimeMetadata>>) ReflectionTestUtils.getField(manager, "runtimeMetadataMap");
        assertTrue(runtimeRows.get(20L).containsKey("active"));
        assertFalse(runtimeRows.get(20L).containsKey("stopped"));
        assertFalse(runtimeRows.get(20L).containsKey("deleted"));
        assertTrue(runtimeRows.get(20L).containsKey("other-state"));

        cluster.setStatus(0L);
        manager.handlerData(List.of(cluster), List.of(), List.of());
        Map<Long, ClusterMetadata> clusters = (Map<Long, ClusterMetadata>) ReflectionTestUtils.getField(manager, "clusterMetadataMap");
        assertFalse(clusters.containsKey(20L));

        ClusterMetadata deletedCluster = new ClusterMetadata();
        deletedCluster.setId(40L);
        deletedCluster.setStatus(1L);
        deletedCluster.setIsDelete(1);
        manager.handlerData(List.of(deletedCluster), List.of(), List.of());
        assertFalse(clusters.containsKey(40L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void metadataReportIndexDropsStatusZeroTombstones() throws Exception {
        MetadataDataManage manager = new MetadataDataManage();
        Class<?> rowType = Class.forName(MetadataDataManage.class.getName() + "$NameAndId");
        Constructor<?> constructor = rowType.getDeclaredConstructor(MetadataDataManage.class);
        constructor.setAccessible(true);
        Object stored = constructor.newInstance(manager);
        setReportIndexKey(stored, 1L);
        ReflectionTestUtils.setField(stored, "status", 1L);
        ReflectionTestUtils.setField(stored, "isDelete", 0);
        Map<Object, Object> index = (Map<Object, Object>) ReflectionTestUtils.getField(manager, "nameAndIdMap");
        index.put(stored, stored);

        Object tombstone = constructor.newInstance(manager);
        setReportIndexKey(tombstone, 1L);
        ReflectionTestUtils.setField(tombstone, "status", 0L);
        ReflectionTestUtils.setField(tombstone, "isDelete", 0);
        List<Object> rows = (List<Object>) ReflectionTestUtils.getField(manager, "nameAndIdList");
        rows.add(tombstone);
        manager.handlerMetadata();

        assertNull(manager.getId(MetadataType.TOPIC, 50L, "orders"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void stoppedDeletedAndStatusTwoCollectsRemoveTheExecutingCollector() {
        assertCollectIsRemoved(0L, 0);
        assertCollectIsRemoved(1L, 1);
        assertCollectIsRemoved(2L, 0);
    }

    private static RuntimeMetadata runtime(Long id, Long clusterId, String name, Long status, Integer isDelete) {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(id);
        runtime.setClusterId(clusterId);
        runtime.setName(name);
        runtime.setStatus(status);
        runtime.setIsDelete(isDelete);
        runtime.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        return runtime;
    }

    private static void setReportIndexKey(Object row, Long id) {
        ReflectionTestUtils.setField(row, "metadataType", MetadataType.TOPIC);
        ReflectionTestUtils.setField(row, "superId", 50L);
        ReflectionTestUtils.setField(row, "name", "orders");
        ReflectionTestUtils.setField(row, "id", id);
    }

    @SuppressWarnings("unchecked")
    private static void assertCollectIsRemoved(Long status, Integer isDelete) {
        CollectManage manager = new CollectManage();
        CollectMetadata existing = collect(20L, 90L, 1L, 0);
        CollectMetadata incoming = collect(20L, 91L, status, isDelete);
        String key = "CollectMetadata-" + incoming.getId();
        Map<Long, CollectMetadata> metadata =
            (Map<Long, CollectMetadata>) ReflectionTestUtils.getField(manager, "collectMetadataMap");
        Map<String, Collect> executing = (Map<String, Collect>) ReflectionTestUtils.getField(manager, "executeingCollectMap");
        metadata.put(20L, existing);
        executing.put(key, org.mockito.Mockito.mock(Collect.class));

        manager.handlerData(List.of(), List.of(), List.of(incoming));

        assertFalse("stopped/deleted collect remains cached", metadata.containsKey(20L));
        assertFalse("stopped/deleted collector remains executable", executing.containsKey(key));
    }

    private static CollectMetadata collect(Long clusterId, Long id, Long status, Integer isDelete) {
        CollectMetadata metadata = new CollectMetadata();
        metadata.setClusterId(clusterId);
        metadata.setId(id);
        metadata.setStatus(status);
        metadata.setIsDelete(isDelete);
        metadata.setCollectType(CollectType.PROMETHEUS);
        return metadata;
    }
}
