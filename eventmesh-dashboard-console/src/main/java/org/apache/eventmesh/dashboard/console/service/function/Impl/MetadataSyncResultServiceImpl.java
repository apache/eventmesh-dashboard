package org.apache.eventmesh.dashboard.console.service.function.Impl;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.MetadataSyncResultEntity;
import org.apache.eventmesh.dashboard.console.service.function.MetadataSyncResultService;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MetadataSyncResultServiceImpl implements MetadataSyncResultService {

    @Override
    public void bachMetadataSyncResult(List<MetadataSyncResultEntity> healthCheckResultEntityList, List<RuntimeEntity> runtimeList,
        List<ClusterEntity> clusterEntityList) {

    }

    @Override
    public List<MetadataSyncResultEntity> queueHealthCheckResultEntityList(MetadataSyncResultEntity healthCheckResultEntity) {
        return Collections.emptyList();
    }
}
