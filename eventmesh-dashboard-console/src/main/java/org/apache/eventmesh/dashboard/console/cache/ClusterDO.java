package org.apache.eventmesh.dashboard.console.cache;

import lombok.Data;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;

import java.util.List;

@Data
public class ClusterDO {

    private ClusterEntity clusterInfo;

    private List<RuntimeEntity> runtimeEntityList;

    private ClusterDO storageCluster;

    private List<ClusterEntity> useClusterList;

    private ClusterEntity  k8sCLuster;

    private Object RegistrationCenter;

}
