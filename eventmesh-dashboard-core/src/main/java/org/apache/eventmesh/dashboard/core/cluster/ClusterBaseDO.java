package org.apache.eventmesh.dashboard.core.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

@Data
public class ClusterBaseDO<C, R, RE, CM> extends BaseDataDO<RE, CM> {


    private C clusterInfo;

    private Map<Long, R> runtimeMap = new ConcurrentHashMap<>();
}
