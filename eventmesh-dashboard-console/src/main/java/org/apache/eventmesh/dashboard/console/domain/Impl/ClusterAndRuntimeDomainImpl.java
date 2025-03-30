package org.apache.eventmesh.dashboard.console.domain.Impl;

import org.apache.eventmesh.dashboard.console.domain.ClusterAndRuntimeDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.Collections;
import java.util.List;

public class ClusterAndRuntimeDomainImpl implements ClusterAndRuntimeDomain {

    @Override
    public List<ClusterEntity> getClusterByCLusterId(ClusterEntity clusterEntity) {
        return Collections.emptyList();
    }
}
