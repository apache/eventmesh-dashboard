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
package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.factory.KafkaCollectFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KafkaOwnershipProbeTest {
    private ClusterMetadata cluster(long id, ClusterType type) {
        ClusterMetadata result = new ClusterMetadata();
        result.setId(id); result.setOrganizationId(1L); result.setName("cluster-" + id);
        result.setClusterType(type); result.setStatus(1L); result.setIsDelete(0);
        return result;
    }
    private RuntimeMetadata node(long id, long owner, ClusterType type, Integer jmx) {
        RuntimeMetadata result = new RuntimeMetadata();
        result.setId(id); result.setClusterId(owner); result.setName("node-" + id);
        result.setClusterType(type); result.setHost("127.0.0.1"); result.setPort(9092); result.setJmxPort(jmx);
        result.setStatus(1L); result.setIsDelete(0);
        return result;
    }
    private CollectMetadata config(long id, long owner, ClusterType type) {
        CollectMetadata result = new CollectMetadata();
        result.setId(id); result.setClusterId(owner); result.setClusterType(type);
        result.setCollectType(CollectType.EVENTMESH); result.setEnable(true); result.setStatus(1L); result.setIsDelete(0);
        return result;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Collect> tasks(CollectManage manager) throws IllegalAccessException {
        return (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
    }
    private CollectManage stoppedLegacy() throws IllegalAccessException {
        CollectManage manager = new CollectManage();
        ClusterMetadata legacy = cluster(21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ClusterMetadata kafka = cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER);
        RuntimeMetadata node = node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null);
        manager.handlerData(List.of(legacy, kafka), List.of(node),
            List.of(config(23L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                config(13L, 11L, ClusterType.STORAGE_KAFKA_CLUSTER)));
        Collect old = tasks(manager).get("RuntimeMetadata-12");
        assertNotNull(old);
        RuntimeMetadata stopped = node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null);
        stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        assertSame(old, tasks(manager).get("RuntimeMetadata-12"));
        return manager;
    }
    @Test
    public void migratingStoppedLegacyNodeWithoutJmxRemovesOldTask() throws Exception {
        CollectManage manager = stoppedLegacy();
        RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null);
        manager.handlerData(List.of(), List.of(migrated), List.of());
        assertTrue(tasks(manager).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"), "stale legacy task survives node migration");
    }
    @Test
    public void recreatingFormerClusterDoesNotRemoveNewKafkaTask() throws Exception {
        CollectManage manager = stoppedLegacy();
        RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, 9999);
        manager.handlerData(List.of(), List.of(migrated), List.of());
        Collect newTask = tasks(manager).get("RuntimeMetadata-12");
        assertTrue(newTask instanceof KafkaJmxCollect);
        manager.handlerData(List.of(cluster(21L, ClusterType.STORAGE_KAFKA_CLUSTER)), List.of(), List.of());
        assertSame(newTask, tasks(manager).get("RuntimeMetadata-12"));
    }
    @Test
    public void migrationFailureRetryDoesNotRestoreOldTask() throws Exception {
        CollectManage manager = stoppedLegacy();
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("fixture failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(), List.of(migrated), List.of()));
        manager.handlerData(List.of(), List.of(node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null)), List.of());
        assertTrue(tasks(manager).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
    }
    @Test
    public void migratingStoppedLegacyNodeDoesNotDisturbOtherLegacyNode() throws Exception {
        CollectManage manager = new CollectManage();
        manager.handlerData(List.of(cluster(21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER)),
            List.of(node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null),
                node(14L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null)),
            List.of(config(23L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                config(13L, 11L, ClusterType.STORAGE_KAFKA_CLUSTER)));
        Collect other = tasks(manager).get("RuntimeMetadata-14");
        RuntimeMetadata stopped = node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null); stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        manager.handlerData(List.of(), List.of(node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null)), List.of());
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
        assertSame(other, tasks(manager).get("RuntimeMetadata-14"));
    }
    @Test
    public void migratingWithoutKafkaConfigurationRemovesOldTask() throws Exception {
        CollectManage manager = new CollectManage();
        manager.handlerData(List.of(cluster(21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER)),
            List.of(node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null)),
            List.of(config(23L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE)));
        RuntimeMetadata stopped = node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null); stopped.setStatus(0L);
        manager.handlerData(List.of(), List.of(stopped), List.of());
        assertNotNull(tasks(manager).get("RuntimeMetadata-12"));
        manager.handlerData(List.of(), List.of(node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null)), List.of());
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
        assertFalse(tasks(manager).containsKey("ClusterMetadata-11"));
    }
    @Test
    public void migratingMutableNodeStillUsesOriginalTaskOwner() throws Exception {
        CollectManage manager = new CollectManage();
        RuntimeMetadata mutable = node(12L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null);
        manager.handlerData(List.of(cluster(21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER)), List.of(mutable),
            List.of(config(23L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE),
                config(13L, 11L, ClusterType.STORAGE_KAFKA_CLUSTER)));
        mutable.setStatus(0L);
        manager.handlerData(List.of(), List.of(mutable), List.of());
        assertNotNull(tasks(manager).get("RuntimeMetadata-12"));
        mutable.setStatus(1L); mutable.setClusterId(11L); mutable.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
        manager.handlerData(List.of(), List.of(mutable), List.of());
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
    }
    @Test
    public void unknownClusterOwnerIsNotTreatedAsProvenMigration() throws Exception {
        CollectManage manager = new CollectManage();
        manager.handlerData(List.of(cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER)), List.of(),
            List.of(config(13L, 11L, ClusterType.STORAGE_KAFKA_CLUSTER)));
        Collect unknownOwner = new AbstractCollect() { @Override protected void doCollect() { } };
        tasks(manager).put("RuntimeMetadata-12", unknownOwner);
        manager.handlerData(List.of(), List.of(node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null)), List.of());
        assertSame(unknownOwner, tasks(manager).get("RuntimeMetadata-12"));
    }

    @Test
    public void stoppedMigrationCleanupSurvivesFactoryFailureAndRetry() throws Exception {
        CollectManage manager = stoppedLegacy();
        KafkaCollectFactory factory = mock(KafkaCollectFactory.class);
        doThrow(new IllegalStateException("fixture failure")).doCallRealMethod().when(factory).createCollectors(any(), any(), any());
        FieldUtils.writeField(manager, "kafkaCollectFactory", factory, true);
        ClusterMetadata changed = cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER); changed.setName("changed");
        RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null); migrated.setStatus(0L);
        assertThrows(IllegalStateException.class, () -> manager.handlerData(List.of(changed), List.of(migrated), List.of()));
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
        manager.handlerData(List.of(), List.of(), List.of());
        assertTrue(tasks(manager).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
    }
    @Test
    public void stoppedOrDeletedKafkaRowKeepsUnknownTaskOwner() throws Exception {
        for (boolean deleted : List.of(false, true)) {
            for (boolean isAbstract : List.of(false, true)) {
                CollectManage manager = new CollectManage();
                manager.handlerData(List.of(cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER)), List.of(),
                    List.of(config(13L, 11L, ClusterType.STORAGE_KAFKA_CLUSTER)));
                Collect unknownOwner = isAbstract ? new AbstractCollect() { @Override protected void doCollect() { } }
                    : (index, wrapper) -> { };
                tasks(manager).put("RuntimeMetadata-12", unknownOwner);
                RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null);
                if (deleted) { migrated.setIsDelete(1); } else { migrated.setStatus(0L); }
                manager.handlerData(List.of(), List.of(migrated), List.of());
                assertSame(unknownOwner, tasks(manager).get("RuntimeMetadata-12"));
            }
        }
    }

    @Test
    public void stoppedKafkaMigrationRemovesFormerTask() throws Exception {
        verifyInactiveMigration(false);
    }
    @Test
    public void deletedKafkaMigrationRemovesFormerTask() throws Exception {
        verifyInactiveMigration(true);
    }
    private void verifyInactiveMigration(boolean deleted) throws Exception {
        CollectManage manager = stoppedLegacy();
        manager.handlerData(List.of(), List.of(node(14L, 21L, ClusterType.STORAGE_ROCKETMQ_BROKER, null)), List.of());
        Collect other = tasks(manager).get("RuntimeMetadata-14");
        assertNotNull(other);
        RuntimeMetadata migrated = node(12L, 11L, ClusterType.STORAGE_KAFKA_RAFT, null);
        if (deleted) { migrated.setIsDelete(1); } else { migrated.setStatus(0L); }
        manager.handlerData(List.of(), List.of(migrated), List.of());
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
        assertSame(other, tasks(manager).get("RuntimeMetadata-14"));
        manager.handlerData(List.of(), List.of(migrated), List.of());
        ClusterMetadata changed = cluster(11L, ClusterType.STORAGE_KAFKA_CLUSTER); changed.setName("changed");
        manager.handlerData(List.of(changed), List.of(), List.of());
        assertFalse(tasks(manager).containsKey("RuntimeMetadata-12"));
        assertSame(other, tasks(manager).get("RuntimeMetadata-14"));
    }

}
