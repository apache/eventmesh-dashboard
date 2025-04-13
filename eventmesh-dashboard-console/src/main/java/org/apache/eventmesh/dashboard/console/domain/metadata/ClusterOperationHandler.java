package org.apache.eventmesh.dashboard.console.domain.metadata;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;

public interface ClusterOperationHandler {


    void handler(RuntimeMetadata baseSyncBase);


    void handler(ClusterMetadata clusterDO);


}
