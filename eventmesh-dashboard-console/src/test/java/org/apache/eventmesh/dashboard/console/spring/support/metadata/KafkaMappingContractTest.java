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

package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.console.service.metadata.ConfigDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.GroupDataMetadataHandler;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaMappingContractTest {

    private static final Set<ClusterType> KAFKA_CLUSTER_TYPES = EnumSet.of(ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT);

    @Test
    @DisplayName("Kafka Topic, Group and Config mappings override only their remote contract")
    void kafkaOverridesPreserveDatabaseAndConversionContracts() {
        assertKafkaOverride(DatabaseAndMetadataType.TOPIC, MetadataType.TOPIC, TopicDataMetadataHandler.class,
            TopicRemotingService.class, org.apache.eventmesh.dashboard.service.remoting.kafka.TopicRemotingService.class);
        assertKafkaOverride(DatabaseAndMetadataType.GROUP, MetadataType.GROUP, GroupDataMetadataHandler.class,
            GroupRemotingService.class, org.apache.eventmesh.dashboard.service.remoting.kafka.GroupRemotingService.class);
        assertKafkaOverride(DatabaseAndMetadataType.CONFIG, MetadataType.CONFIG, ConfigDataMetadataHandler.class,
            ConfigRemotingService.class, org.apache.eventmesh.dashboard.service.remoting.kafka.ConfigRemotingService.class);
    }

    @Test
    @DisplayName("Kafka-specific metadata contracts remain limited to Topic, Group and Config")
    void unrelatedMetadataKeepsItsDefaultHandler() {
        for (DatabaseAndMetadataType type : DatabaseAndMetadataType.values()) {
            DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
            if (type == DatabaseAndMetadataType.TOPIC || type == DatabaseAndMetadataType.GROUP || type == DatabaseAndMetadataType.CONFIG) {
                continue;
            }
            Assertions.assertNull(mapper.getMetadataHandlerClassByClusterType(), type.name());
            for (ClusterType clusterType : KAFKA_CLUSTER_TYPES) {
                Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(clusterType),
                    type + " / " + clusterType);
            }
            Assertions.assertSame(mapper.getMetadataHandlerClass(), mapper.resolveMetadataHandlerClass(null), type.name());
        }
    }

    @Test
    @DisplayName("构造 DatabaseAndMetadataMapper 时 builder 返回可用实例")
    void mapperBuilderCreatesConfiguredMapper() {
        Assertions.assertNotNull(DatabaseAndMetadataMapper.builder());
        DatabaseAndMetadataMapper mapper = DatabaseAndMetadataMapper.builder()
            .metaType(MetadataType.TOPIC)
            .databaseHandlerClass(TopicDataMetadataHandler.class)
            .metadataHandlerClass(TopicRemotingService.class)
            .build();

        Assertions.assertNotNull(mapper);
        Assertions.assertEquals(MetadataType.TOPIC, mapper.getMetaType());
        Assertions.assertSame(TopicDataMetadataHandler.class, mapper.getDatabaseHandlerClass());
        Assertions.assertSame(TopicRemotingService.class, mapper.getMetadataHandlerClass());
    }

    private void assertKafkaOverride(DatabaseAndMetadataType type, MetadataType metadataType, Class<?> databaseHandler,
        Class<?> genericRemoteHandler, Class<?> kafkaRemoteHandler) {
        DatabaseAndMetadataMapper mapper = type.getDatabaseAndMetadataMapper();
        Assertions.assertEquals(metadataType, mapper.getMetaType());
        Assertions.assertSame(databaseHandler, mapper.getDatabaseHandlerClass());
        Assertions.assertNotNull(mapper.getConvertMetaData());
        Assertions.assertSame(genericRemoteHandler, mapper.getMetadataHandlerClass());
        Assertions.assertEquals(KAFKA_CLUSTER_TYPES, mapper.getMetadataHandlerClassByClusterType().keySet());
        for (ClusterType clusterType : KAFKA_CLUSTER_TYPES) {
            Assertions.assertSame(kafkaRemoteHandler, mapper.resolveMetadataHandlerClass(clusterType), clusterType.name());
        }
        Assertions.assertSame(genericRemoteHandler, mapper.resolveMetadataHandlerClass(null));
        for (ClusterType clusterType : ClusterType.values()) {
            if (!KAFKA_CLUSTER_TYPES.contains(clusterType)) {
                Assertions.assertSame(genericRemoteHandler, mapper.resolveMetadataHandlerClass(clusterType), clusterType.name());
            }
        }
    }
}
