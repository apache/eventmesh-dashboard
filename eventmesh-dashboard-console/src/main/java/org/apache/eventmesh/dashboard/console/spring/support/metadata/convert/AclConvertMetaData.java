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


package org.apache.eventmesh.dashboard.console.spring.support.metadata.convert;

import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/** Converts existing persisted columns without introducing a schema migration. */
@Mapper
public interface AclConvertMetaData extends ConvertMetaData<AclEntity, AclMetadata> {

    AclConvertMetaData INSTANCE = Mappers.getMapper(AclConvertMetaData.class);

    @Override
    @Mapping(target = "principal", source = "pattern")
    @Mapping(target = "resourceType", source = "resourceType", qualifiedByName = "resourceName")
    @Mapping(target = "permissionType", source = "permissionType", qualifiedByName = "permissionName")
    AclMetadata toMetaData(AclEntity entity);

    @Override
    @Mapping(target = "pattern", source = "principal")
    @Mapping(target = "resourceType", source = "resourceType", qualifiedByName = "resourceCode")
    @Mapping(target = "permissionType", source = "permissionType", qualifiedByName = "permissionCode")
    AclEntity toEntity(AclMetadata metadata);

    @BeforeMapping
    default void validatePersistable(AclMetadata metadata) {
        if (metadata != null && (metadata.getPolicyType() != null || metadata.getActions() != null || metadata.getSourceIps() != null)) {
            throw new IllegalArgumentException("RocketMQ ACL policies cannot be stored in the Kafka ACL table");
        }
    }

    @Named("resourceName")
    default String resourceName(Integer code) {
        if (Integer.valueOf(10).equals(code)) {
            return "TOPIC";
        }
        if (Integer.valueOf(11).equals(code)) {
            return "GROUP";
        }
        throw new IllegalArgumentException("Unsupported persisted Kafka resource type: " + code);
    }

    @Named("resourceCode")
    default Integer resourceCode(String name) {
        if ("TOPIC".equals(name)) {
            return 10;
        }
        if ("GROUP".equals(name)) {
            return 11;
        }
        throw new IllegalArgumentException("Unsupported persisted Kafka resource type: " + name);
    }

    @Named("permissionName")
    default String permissionName(Integer code) {
        if (Integer.valueOf(2).equals(code)) {
            return "DENY";
        }
        if (Integer.valueOf(3).equals(code)) {
            return "ALLOW";
        }
        throw new IllegalArgumentException("Unsupported persisted Kafka permission: " + code);
    }

    @Named("permissionCode")
    default Integer permissionCode(String name) {
        if ("DENY".equals(name)) {
            return 2;
        }
        if ("ALLOW".equals(name)) {
            return 3;
        }
        throw new IllegalArgumentException("Unsupported persisted Kafka permission: " + name);
    }
}
