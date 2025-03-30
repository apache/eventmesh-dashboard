package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ClusterBaseDO<C, R extends  RuntimeBaseDO, RE, CM> extends BaseDataDO<RE, CM> {


    private C clusterInfo;

    private Map<Long, R> runtimeMap = new ConcurrentHashMap<>();

    private AbstractMultiCreateSDKConfig multiCreateSDKConfig;



}
