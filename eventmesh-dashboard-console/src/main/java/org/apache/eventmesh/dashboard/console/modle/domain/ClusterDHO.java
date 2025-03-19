package org.apache.eventmesh.dashboard.console.modle.domain;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直接序列化，或则 使用 mapper
 */
public class ClusterDHO {

    private ColonyDO<ClusterEntityDO> colonyDO;


    public void setMainCluster(ClusterEntity clusterEntity) {
        this.colonyDO = ColonyDO.create(ClusterEntityDO.class, clusterEntity);
        this.colonyDO.setClusterId(clusterEntity.getClusterId());
        this.colonyDO.setClusterType(clusterEntity.getClusterType());
        this.colonyDO.setSuperiorId(-0L);
    }

    public void rootClusterDHO() {
        this.colonyDO = ColonyDO.create(ClusterEntityDO.class, null);
        this.colonyDO.setSuperiorId(-0L);
    }

    public List<ClusterEntity> setClusterEntityAndDefinitionCluster(List<ClusterEntity> clusterEntity,
        List<ClusterRelationshipEntity> clusterRelationshipEntity) {
        List<ClusterEntity> definitionClusteList = new ArrayList<>();
        Map<Long, ClusterEntity> clusterEntityMap = new HashMap<>();
        clusterEntity.forEach(v -> {
            if (v.getClusterType().isDefinition()) {
                definitionClusteList.add(v);
            }
        });

        clusterRelationshipEntity.forEach(v -> {
            ClusterEntity c = clusterEntityMap.get(v.getClusterId());
            this.colonyDO.relationship(v.getClusterId(), v.getRelationshipId(), c.getClusterType(), c);
        });

        return definitionClusteList;
    }

    public void setRuntimeEntity(List<RuntimeEntity> runtimeEntity) {

    }

    public void setResource() {

    }

    public void setConfigEntity() {

    }

    private List<ClusterEntity> getClusterEntity(Map<Long, ColonyDO<ClusterEntityDO>> storageColonyDOList) {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        storageColonyDOList.values().forEach(clusterEntityDOColonyDO -> {
            clusterEntityList.add(clusterEntityDOColonyDO.getClusterDO().getClusterInfo());
        });
        return clusterEntityList;
    }

    private List<RuntimeEntity> getRuntimeList(Map<Long, ColonyDO<ClusterEntityDO>> storageColonyDOList) {
        List<RuntimeEntity> runtimeIds = new ArrayList<>();
        storageColonyDOList.values().forEach(clusterEntityDOColonyDO -> {
            runtimeIds.addAll(clusterEntityDOColonyDO.getClusterDO().getRuntimeMap().values());
        });
        return runtimeIds;
    }

    public List<ClusterEntity> getMetaClusterList() {
        return this.getClusterEntity(colonyDO.getMetaColonyDOList());
    }

    public List<RuntimeEntity> getMetaRuntimeList() {
        return this.getRuntimeList(colonyDO.getMetaColonyDOList());
    }

    public List<ClusterEntity> getStorageClusterList() {
        return this.getClusterEntity(colonyDO.getStorageColonyDOList());
    }

    public List<ClusterEntity> getStorageRuntimeClusterIds() {
        colonyDO.getStorageColonyDOList().values().forEach(value -> {

        });
        return null;
    }


    public List<RuntimeEntity> getStorageRuntimeIds() {
        return this.getRuntimeList(colonyDO.getStorageColonyDOList());
    }

    public List<ClusterEntity> getStorageMetaClusterIds() {
        return null;
    }

    public List<RuntimeEntity> getStorageMetaRuntimeIds() {
        return this.getRuntimeList(colonyDO.getMetaColonyDOList());
    }


}
