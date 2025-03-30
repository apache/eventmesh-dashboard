package org.apache.eventmesh.dashboard.console.domain.metadata;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class HandlerMetadataDO {

    private Map<Long, AbstractMultiCreateSDKConfig> multiCreateSDKConfigMap = new HashMap<>();

    private Map<Long, BaseSyncBase> syncBaseMap = new HashMap<>();

    private Map<Long, ClusterType> clusterTypeMap = new HashMap<>();

}
