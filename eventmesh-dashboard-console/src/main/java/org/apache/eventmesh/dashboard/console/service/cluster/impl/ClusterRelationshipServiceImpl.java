package org.apache.eventmesh.dashboard.console.service.cluster.impl;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClusterRelationshipServiceImpl implements ClusterRelationshipService {

    @Autowired
    private ClusterRelationshipMapper clusterRelationshipMapper;

    @Override
    public Integer addClusterRelationshipEntry(ClusterRelationshipEntity clusterRelationshipEntity) {
        return this.clusterRelationshipMapper.addClusterRelationshipEntry(clusterRelationshipEntity);
    }

    @Override
    public Integer relieveRelationship(ClusterRelationshipEntity clusterRelationshipEntity) {
        return clusterRelationshipMapper.relieveRelationship(clusterRelationshipEntity);
    }

    @Override
    public List<ClusterRelationshipEntity> selectAll() {
        return this.clusterRelationshipMapper.selectAll();
    }

    @Override
    public List<ClusterRelationshipEntity> selectNewlyIncreased(ClusterRelationshipEntity clusterRelationshipEntity) {
        return this.clusterRelationshipMapper.selectNewlyIncreased();
    }
}
