package org.apache.eventmesh.dashboard.common.enums;

import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

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
    ;

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
