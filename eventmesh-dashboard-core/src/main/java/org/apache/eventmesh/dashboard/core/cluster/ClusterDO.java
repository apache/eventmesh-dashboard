package org.apache.eventmesh.dashboard.core.cluster;

import lombok.Data;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ClusterDO {

    private ClusterMetadata clusterInfo;

    private Map<Long , RuntimeMetadata> runtimeMap = new ConcurrentHashMap<>();

}
