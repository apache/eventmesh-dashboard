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


package org.apache.eventmesh.dashboard.common.enums;

import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

/**
 * 集群的特性
 */
@Getter
public enum ClusterSyncMetadataEnum {


    EVENTMESH_RUNTIME(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.INDEPENDENCE).metadataTypeList(ClusterSyncMetadata.STORAGE).build()),

    EVENTMESH_JVM_CLUSTER(EVENTMESH_RUNTIME),

    EVENTMESH_META_ETCD(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.RAFT).metadataTypeList(ClusterSyncMetadata.META).build()),

    EVENTMESH_META_NACOS(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.AP).metadataTypeList(ClusterSyncMetadata.META).build()),

    EVENTMESH_JVM_RUNTIME(EVENTMESH_RUNTIME),

    EVENTMESH_JVM_META(EVENTMESH_META_NACOS),


    STORAGE_ROCKETMQ_NAMESERVER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.AP).metadataTypeList(ClusterSyncMetadata.META).build()),

    STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.MAIN_SLAVE).metadataTypeList(ClusterSyncMetadata.STORAGE).build()),

    STORAGE_ROCKETMQ_BROKER_RFT(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.RAFT).metadataTypeList(ClusterSyncMetadata.STORAGE).build()),

    STORAGE_KAFKA_ZK(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.ZK).metadataTypeList(ClusterSyncMetadata.META).build()),

    STORAGE_KAFKA_RAFT(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.RAFT).metadataTypeList(ClusterSyncMetadata.META).build()),

    STORAGE_KAFKA_BROKER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.CAP)
            .metadataTypeList(ListWrapper.build().add(ClusterSyncMetadata.STORAGE).add(ClusterSyncMetadata.AUTH).list)
            .replicationDimension(ReplicationDimension.TOPIC).build()),


    STORAGE_JVM_META(EVENTMESH_META_NACOS),

    STORAGE_JVM_BROKER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.MAIN_SLAVE).metadataTypeList(ClusterSyncMetadata.TEST_ONE).build()),


    STORAGE_JVM_CAP_META(STORAGE_KAFKA_ZK),

    STORAGE_JVM_CAP_BROKER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.CAP).metadataTypeList(ClusterSyncMetadata.STORAGE)
            .replicationDimension(ReplicationDimension.TOPIC).build()),
    ;

    private static final Map<ClusterType, ClusterSyncMetadata> SYNC_METADATA_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
    private ClusterSyncMetadata clusterSyncMetadata;

    ClusterSyncMetadataEnum(ClusterSyncMetadata clusterSyncMetadata) {
        this.clusterSyncMetadata = clusterSyncMetadata;
    }

    ClusterSyncMetadataEnum(ClusterSyncMetadataEnum clusterSyncMetadataEnum) {
        this.clusterSyncMetadata = clusterSyncMetadataEnum.clusterSyncMetadata;
    }

    public static ClusterSyncMetadata getClusterSyncMetadata(ClusterType clusterType) {
        ClusterSyncMetadata clusterSyncMetadata = SYNC_METADATA_CONCURRENT_HASH_MAP.get(clusterType);
        if (Objects.nonNull(clusterSyncMetadata)) {
            return clusterSyncMetadata;
        }
        try {
            ClusterSyncMetadataEnum clusterSyncMetadataEnum = ClusterSyncMetadataEnum.valueOf(clusterType.toString());
            clusterSyncMetadata = clusterSyncMetadataEnum.getClusterSyncMetadata();
        } catch (Exception e) {

            clusterSyncMetadata = ClusterSyncMetadata.EMPTY_OBJECT;
        }
        SYNC_METADATA_CONCURRENT_HASH_MAP.put(clusterType, clusterSyncMetadata);
        return clusterSyncMetadata;
    }

    public static ClusterFramework getClusterFramework(ClusterType clusterType) {
        return getClusterSyncMetadata(clusterType).getClusterFramework();
    }

    static class ListWrapper {

        private final List<MetadataType> list = new ArrayList<>();

        static ListWrapper build() {
            return new ListWrapper();
        }

        ListWrapper add(List<MetadataType> list) {
            this.list.addAll(list);
            return this;
        }
    }
}
