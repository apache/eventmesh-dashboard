package org.apache.eventmesh.dashboard.console.domain;


import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.List;

public interface ClusterAndRuntimeDomain {



    List<ClusterEntity> getClusterByCLusterId(ClusterEntity clusterEntity);





}
