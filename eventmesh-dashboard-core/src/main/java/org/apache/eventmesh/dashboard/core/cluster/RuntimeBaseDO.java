package org.apache.eventmesh.dashboard.core.cluster;


import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeBaseDO<R, RE, CM> extends BaseDataDO<RE, CM> {

    private R runtimeMetadata;

    private AbstractCreateSDKConfig createSDKConfig;


    private ClusterSyncMetadata clusterSyncMetadata;


}
