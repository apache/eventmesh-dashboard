package org.apache.eventmesh.dashboard.console.service.function;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.MetadataSyncResultEntity;

import java.util.List;

public interface MetadataSyncResultService {


    void bachMetadataSyncResult(List<MetadataSyncResultEntity> healthCheckResultEntityList, List<RuntimeEntity> runtimeList,
        List<ClusterEntity> clusterEntityList);


    List<MetadataSyncResultEntity> queueHealthCheckResultEntityList(MetadataSyncResultEntity healthCheckResultEntity);
}
