package org.apache.eventmesh.dashboard.console.controller.deploy.handler;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class AbstractMetadataExampleHandler<T> implements UpdateHandler<T> {

    protected ReplicationType replicationType;

    protected ClusterType clusterType;

    protected ClusterFramework clusterFramework;

    protected ClusterEntity k8sClusterEntity;

    protected KubernetesClient kubernetesClient;


    protected  void handlerMetadata(ClusterEntity clusterEntity) {
        this.clusterType = clusterEntity.getClusterType();
        this.replicationType = clusterEntity.getReplicationType();
        this.clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(this.clusterType);

    }

}
