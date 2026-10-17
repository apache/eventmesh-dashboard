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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.console.controller.cluster.AclController;
import org.apache.eventmesh.dashboard.console.controller.ConnectionController;
import org.apache.eventmesh.dashboard.console.controller.cluster.ClusterRelationshipController;
import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.AclMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ConnectionMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.connector.ConnectorMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.AclServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ConnectionDataServiceImpl;
import org.apache.eventmesh.dashboard.console.service.cluster.impl.ClusterRelationshipServiceImpl;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ClusterRelationshipSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.ConnectionSnapshotHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.KafkaAclSnapshotHandler;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.SnapshotMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.time.Duration;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assume;
import org.junit.Test;

/** Real HTTP/MySQL-to-Kafka acceptance; skipped unless isolated local services are explicitly enabled. */
public class MetadataPipelineMysqlTest extends EntityMysqlChainSupportTest {

    @Test
    public void httpAclSnapshotFlowsThroughRegisteredMetadataSyncManagerToKafka() throws Exception {
        Assume.assumeTrue("Enable local dedicated Kafka ACL acceptance with -Dmetadata.pipeline.kafka=true",
            Boolean.getBoolean("metadata.pipeline.kafka"));
        try (Fixture fixture = fixture("acl")) {
            AclMapper mapper = fixture.mapper(AclMapper.class);
            fixture.startHttp(AclController.class, AclServiceImpl.class);
            long clusterId = Math.abs(System.nanoTime());
            String principal = "User:dashboard-pipeline-" + UUID.randomUUID();
            String resource = "dashboard-pipeline-" + UUID.randomUUID();
            String payload = "{\"clusterId\":" + clusterId + ",\"pattern\":\"" + principal
                + "\",\"operation\":4,\"permissionType\":3,\"host\":\"*\",\"resourceType\":10,"
                + "\"resourceName\":\"" + resource + "\",\"patternType\":3}";
            assertEquals(200, fixture.post("/acl/insertAcl", payload).statusCode());
            Long id = fixture.jdbc().queryForObject("SELECT id FROM acl WHERE cluster_id = ? AND pattern = ?",
                Long.class, clusterId, principal);
            assertNotNull(id);
            fixture.jdbc().update("INSERT INTO acl (cluster_id, pattern, operation, permission_type, host, "
                + "resource_type, resource_name, pattern_type) VALUES (?, ?, 0, 0, '*', 21, 'rocket-topic', 3)",
                clusterId, "User:rocketmq-independent");
            assertEquals(Integer.valueOf(1), fixture.jdbc().queryForObject(
                "SELECT COUNT(*) FROM acl WHERE cluster_id = ? AND resource_type = 21", Integer.class, clusterId));

            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(clusterId);
            cluster.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
            String bootstrap = System.getProperty("metadata.pipeline.kafka.bootstrap", "127.0.0.1:39092");
            ColonyDO<ClusterDO> colony = colony(cluster);
            var config = ConfigManage.getInstance().getMultiCreateSdkConfig(cluster.getClusterType(), SDKTypeEnum.ADMIN);
            config.setKey(cluster.getUnique());
            String[] address = bootstrap.split(":", 2);
            config.addNetAddress(NetAddress.create(address[0], Integer.parseInt(address[1])));
            colony.getClusterDO().setMultiCreateSDKConfig(config);
            SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, cluster, config, cluster.getClusterType());
            AdminClient managedClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
            AdminClient pingClient = SDKManage.getInstance().getClient(SDKTypeEnum.PING, cluster.getUnique());
            try (AdminClient verifier = AdminClient.create(adminProperties(bootstrap))) {
                var bindingFilter = new AclBindingFilter(
                    new org.apache.kafka.common.resource.ResourcePatternFilter(ResourceType.TOPIC, resource,
                        PatternType.LITERAL),
                    new org.apache.kafka.common.acl.AccessControlEntryFilter(principal, "*", AclOperation.WRITE,
                        AclPermissionType.ALLOW));
                AclBinding desired = new AclBinding(
                    new org.apache.kafka.common.resource.ResourcePattern(ResourceType.TOPIC, resource, PatternType.LITERAL),
                    new org.apache.kafka.common.acl.AccessControlEntry(principal, "*", AclOperation.WRITE,
                        AclPermissionType.ALLOW));
                AclBinding unrelated = new AclBinding(
                    new org.apache.kafka.common.resource.ResourcePattern(ResourceType.TOPIC, "unrelated-" + resource,
                        PatternType.LITERAL),
                    new org.apache.kafka.common.acl.AccessControlEntry("User:unrelated", "*", AclOperation.READ,
                        AclPermissionType.ALLOW));
                AclBindingFilter unrelatedFilter = unrelated.toFilter();
                try {
                    verifier.createAcls(List.of(unrelated)).all().get();
                    DatabaseAndMetadataMapper registration = registration(MetadataType.ACL);
                    KafkaAclSnapshotHandler target = (KafkaAclSnapshotHandler) registration.getSnapshotHandlerClass()
                        .getConstructor().newInstance();
                    MetadataSyncManage sync = manager(registration, AclMapper.class, mapper, colony, target);
                    try {
                        sync.run();
                        assertEquals(List.of(desired), verifier.describeAcls(bindingFilter).values().get().stream().toList());
                        assertTrue(verifier.describeAcls(unrelatedFilter).values().get().contains(unrelated));

                        sync.run();
                        assertEquals(List.of(desired), verifier.describeAcls(bindingFilter).values().get().stream().toList());
                        fixture.jdbc().update("UPDATE acl SET status = 0 WHERE id = ?", id);
                        sync.run();
                        assertEquals(0, verifier.describeAcls(bindingFilter).values().get().size());
                        assertTrue(verifier.describeAcls(unrelatedFilter).values().get().contains(unrelated));
                    } finally {
                        sync.close();
                    }
                } finally {
                    verifier.deleteAcls(List.of(bindingFilter, unrelatedFilter)).all().get();
                }
            } finally {
                try {
                    if (managedClient != null) {
                        managedClient.close(Duration.ofSeconds(5));
                    }
                } finally {
                    try {
                        if (pingClient != null && pingClient != managedClient) {
                            pingClient.close(Duration.ofSeconds(5));
                        }
                    } finally {
                        SDKManage.getInstance().deleteClient(null, cluster.getUnique());
                    }
                }
            }
        }
    }

    @Test
    public void httpConnectionConfigurationFlowsThroughRegisteredMetadataSyncManager() throws Exception {
        try (Fixture fixture = fixture("connection", "connector", "runtime")) {
            long clusterId = 440L;
            long runtimeId = 4401L;
            fixture.seedRuntime(runtimeId, clusterId);
            long sourceId = insertConnector(fixture, clusterId, "source", "Source", "SourceClass", "127.0.0.1", 7441);
            long sinkId = insertConnector(fixture, clusterId, "sink", "Sink", "SinkClass", "127.0.0.2", 7442);
            ConnectionMapper mapper = fixture.mapper(ConnectionMapper.class);
            fixture.mapper(ConnectorMapper.class);
            fixture.mapper(RuntimeMapper.class);
            fixture.startHttp(ConnectionController.class, ConnectionDataServiceImpl.class);
            String request = "{\"clusterId\":440,\"runtimeId\":4401,\"addConnectionDTO\":{"
                + "\"sinkName\":\"sink\",\"sourceName\":\"source\",\"topicName\":\"pipeline-orders\","
                + "\"connectionDescription\":\"from-http\",\"sourceClass\":\"SourceClass\","
                + "\"sinkClass\":\"SinkClass\",\"sourceHost\":\"127.0.0.1\",\"sourcePort\":7441,"
                + "\"sinkHost\":\"127.0.0.2\",\"sinkPort\":7442},\"addConnectorConfigDTO\":null}";
            var response = fixture.post("/cluster/connection/createConnection", request);
            assertEquals(200, response.statusCode());
            assertEquals("success", response.body());

            ConnectionEntity stored = mapper.selectSnapshot().get(0);
            assertEquals(Long.valueOf(clusterId), stored.getClusterId());
            assertEquals(Long.valueOf(runtimeId), stored.getRuntimeId());
            assertEquals(Long.valueOf(sourceId), stored.getSourceId());
            assertEquals(Long.valueOf(sinkId), stored.getSinkId());
            DatabaseAndMetadataMapper registration = registration(MetadataType.CONNECTION);
            ConnectionSnapshotHandler target = (ConnectionSnapshotHandler) registration.getSnapshotHandlerClass()
                .getConstructor().newInstance();
            ColonyDO<ClusterDO> colony = topology(clusterId, runtimeId);
            MetadataSyncManage sync = manager(registration, ConnectionMapper.class, mapper, colony, target);
            try {
                sync.run();

                List<ConnectionMetadata> applied = target.list(clusterId, runtimeId);
                assertEquals(1, applied.size());
                assertEquals(stored.getId(), applied.get(0).getId());
                assertEquals("pipeline-orders", applied.get(0).getTopic());
                assertEquals(Long.valueOf(sourceId), applied.get(0).getSourceId());
                assertEquals(Long.valueOf(sinkId), applied.get(0).getSinkId());
                assertTrue("Fixture uses synthetic Connection DDL because no production DDL exists",
                    fixture.jdbc().queryForObject("SELECT COUNT(*) FROM connection", Integer.class) == 1);

                fixture.jdbc().update("UPDATE connection SET end_time = CURRENT_TIMESTAMP WHERE id = ?", stored.getId());
                sync.run();
                assertTrue(target.list(clusterId, runtimeId).isEmpty());
            } finally {
                sync.close();
            }
        }
    }

    @Test
    public void clusterRelationshipRowsFlowThroughRegisteredMetadataSyncManagerAndTopology() throws Exception {
        try (Fixture fixture = fixture("cluster_relationship")) {
            ClusterRelationshipMapper mapper = fixture.mapper(ClusterRelationshipMapper.class);
            fixture.startHttp(ClusterRelationshipController.class, ClusterRelationshipServiceImpl.class);
            var inserted = fixture.post("/clusterRelationship/addClusterRelationshipEntry",
                "{\"organizationId\":1,\"clusterId\":510,\"clusterType\":\"EVENTMESH_CLUSTER\","
                    + "\"relationshipType\":\"STORAGE_KAFKA_BROKER\",\"relationshipId\":520}");
            assertEquals(200, inserted.statusCode());
            Long relationshipId = fixture.jdbc().queryForObject(
                "SELECT id FROM cluster_relationship WHERE cluster_id = ? AND relationship_id = ?", Long.class, 510L, 520L);
            assertNotNull(relationshipId);

            ColonyDO<ClusterDO> topology = topology(510L, 520L);
            DatabaseAndMetadataMapper registration = registration(MetadataType.CLUSTER_RELATIONSHIP);
            ClusterRelationshipSnapshotHandler target = (ClusterRelationshipSnapshotHandler) registration.getSnapshotHandlerClass()
                .getConstructor().newInstance();
            MetadataSyncManage sync = manager(registration, ClusterRelationshipMapper.class, mapper, topology, target);
            try {
                sync.run();
                assertTrue(topology.getAllColonyDO().get(510L).getStorageColonyDOMap().containsKey(520L));
                assertEquals(Long.valueOf(510L), topology.getAllColonyDO().get(520L).getSuperiorId());

                var deleted = fixture.post("/clusterRelationship/relieveRelationship", "{\"id\":" + relationshipId + "}");
                assertEquals(200, deleted.statusCode());
                assertEquals("1", deleted.body());
                sync.run();
                assertTrue(topology.getAllColonyDO().get(510L).getStorageColonyDOMap().isEmpty());
                assertEquals(null, topology.getAllColonyDO().get(520L).getSuperiorId());
            } finally {
                sync.close();
            }
        }
    }

    private static long insertConnector(Fixture fixture, long clusterId, String name, String type, String className,
        String host, int port) {
        fixture.jdbc().update("INSERT INTO connector (cluster_id, name, class_name, type, host, port, status, pod_state, config_ids) "
            + "VALUES (?, ?, ?, ?, ?, ?, 1, 0, '')", clusterId, name, className, type, host, port);
        return fixture.jdbc().queryForObject("SELECT id FROM connector WHERE cluster_id = ? AND name = ?", Long.class,
            clusterId, name);
    }

    private static ColonyDO<ClusterDO> topology(long parentId, long childId) {
        ColonyDO<ClusterDO> parent = colony(parentId, ClusterType.EVENTMESH_CLUSTER);
        ColonyDO<ClusterDO> child = colony(childId, ClusterType.STORAGE_KAFKA_BROKER);
        Map<Long, ColonyDO<ClusterDO>> all = new HashMap<>();
        all.put(parentId, parent);
        all.put(childId, child);
        parent.setAllColonyDO(all);
        child.setAllColonyDO(all);
        return parent;
    }

    private static ColonyDO<ClusterDO> colony(long id, ClusterType type) {
        ClusterMetadata metadata = new ClusterMetadata();
        metadata.setId(id);
        metadata.setClusterType(type);
        ClusterDO clusterDO = new ClusterDO();
        clusterDO.setClusterInfo(metadata);
        ColonyDO<ClusterDO> colony = new ColonyDO<>();
        colony.setClusterId(id);
        colony.setClusterType(type);
        colony.setClusterDO(clusterDO);
        return colony;
    }

    private static Properties adminProperties(String bootstrap) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrap);
        properties.put("request.timeout.ms", "10000");
        return properties;
    }

    private static DatabaseAndMetadataMapper registration(MetadataType metadataType) {
        return java.util.Arrays.stream(DatabaseAndMetadataType.values())
            .map(DatabaseAndMetadataType::getDatabaseAndMetadataMapper)
            .filter(mapper -> mapper.getMetaType() == metadataType)
            .findFirst().orElseThrow();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MetadataSyncManage manager(DatabaseAndMetadataMapper registration, Class<?> mapperType,
        Object mapper, ColonyDO<ClusterDO> colony, Object target) throws Exception {
        Object dataHandler = registration.getDatabaseHandlerClass().getConstructor(mapperType).newInstance(mapper);
        MetadataSyncManage manager = new MetadataSyncManage();
        manager.setDataMetadataHandlerList(List.of((DataMetadataHandler<Object>) dataHandler));
        manager.setSnapshotMetadataHandlerList(List.of((SnapshotMetadataHandler<?>) target));
        manager.setMetadataSyncResultHandler(mock(MetadataSyncResultHandler.class));
        manager.setColonyDO(colony);
        manager.init(3_600_000, 3_600_000, List.of(registration));
        return manager;
    }

    private static ColonyDO<ClusterDO> colony(ClusterMetadata cluster) {
        ColonyDO<ClusterDO> colony = new ColonyDO<>();
        ClusterDO clusterDO = new ClusterDO();
        clusterDO.setClusterInfo(cluster);
        colony.setClusterDO(clusterDO);
        colony.setClusterId(cluster.getId());
        colony.setClusterType(cluster.getClusterType());
        Map<Long, ColonyDO<ClusterDO>> all = new HashMap<>();
        all.put(cluster.getId(), colony);
        colony.setAllColonyDO(all);
        return colony;
    }
}
