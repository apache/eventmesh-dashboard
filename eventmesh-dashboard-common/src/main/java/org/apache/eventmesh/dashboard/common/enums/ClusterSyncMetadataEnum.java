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
public enum ClusterSyncMetadataEnum {


    EVENTMESH_RUNTIME(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.INDEPENDENCE).metadataTypeList(ClusterSyncMetadata.STORAGE).build()),

    EVENTMESH_META_ETCD(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.RAFT).metadataTypeList(ClusterSyncMetadata.META).build()),

    EVENTMESH_META_NACOS(ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.AP).metadataTypeList(ClusterSyncMetadata.META).build()),

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

    STORAGE_JVM_BROKER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.MAIN_SLAVE).metadataTypeList(ClusterSyncMetadata.TEST_ONE).build()),

    STORAGE_JVM_CAP_BROKER(
        ClusterSyncMetadata.builder().clusterFramework(ClusterFramework.CAP).metadataTypeList(ClusterSyncMetadata.STORAGE)
            .replicationDimension(ReplicationDimension.TOPIC).build()),
    ;

    private static final Map<ClusterType, ClusterSyncMetadata> SYNC_METADATA_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public static ClusterSyncMetadata getClusterSyncMetadata(ClusterType clusterType) {
        ClusterSyncMetadata clusterSyncMetadata = SYNC_METADATA_CONCURRENT_HASH_MAP.get(clusterType);
        try {
            if (Objects.isNull(clusterSyncMetadata)) {
                ClusterSyncMetadataEnum clusterSyncMetadataEnum = ClusterSyncMetadataEnum.valueOf(clusterType.toString());
                SYNC_METADATA_CONCURRENT_HASH_MAP.put(clusterType, clusterSyncMetadataEnum.getClusterSyncMetadata());
                clusterSyncMetadata = clusterSyncMetadataEnum.getClusterSyncMetadata();
            }
        } catch (Exception e) {
            clusterSyncMetadata = ClusterSyncMetadata.EMPTY_OBJECT;
            SYNC_METADATA_CONCURRENT_HASH_MAP.put(clusterType, clusterSyncMetadata);
        }
        return clusterSyncMetadata;
    }

    public static ClusterFramework getClusterFramework(ClusterType clusterType) {
        return getClusterSyncMetadata(clusterType).getClusterFramework();
    }

    static class ListWrapper {

        static ListWrapper build() {
            return new ListWrapper();
        }

        private List<MetadataType> list = new ArrayList<>();

        ListWrapper add(List<MetadataType> list) {
            list.addAll(list);
            return this;
        }
    }

    @Getter
    private ClusterSyncMetadata clusterSyncMetadata;

    ClusterSyncMetadataEnum(ClusterSyncMetadata clusterSyncMetadata) {
        this.clusterSyncMetadata = clusterSyncMetadata;
    }


}
