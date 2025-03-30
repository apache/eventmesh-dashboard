package org.apache.eventmesh.dashboard.console.domain.metadata;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetadataAllDO {

    private List<ClusterEntity> clusterEntityList;

    private List<ClusterRelationshipEntity> clusterRelationshipEntityList;

    private List<RuntimeEntity> runtimeEntityList;

    private List<AbstractMultiCreateSDKConfig> abstractMultiCreateSDKConfigList;
}
