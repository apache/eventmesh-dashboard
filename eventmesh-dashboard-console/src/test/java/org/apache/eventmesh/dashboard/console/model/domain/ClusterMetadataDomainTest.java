package org.apache.eventmesh.dashboard.console.model.domain;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.metadata.HandlerMetadataDO;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

public class ClusterMetadataDomainTest {

    private ClusterMetadataDomain clusterMetadataDomain = new ClusterMetadataDomain();

    private ClusterEntity clusterEntity = new ClusterEntity();

    private List<ClusterEntity> clusterEntityList = new ArrayList<>();

    private List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();

    private List<RuntimeEntity> runtimeEntityList = new ArrayList<>();

    private AtomicLong atomicLong = new AtomicLong(1L);


    @Before
    public void init() {
        clusterMetadataDomain.rootClusterDHO();
        this.test_EventMesh_Cluster();
    }

    public Long getId() {
        return atomicLong.getAndIncrement();
    }

    private void handler() {
        clusterMetadataDomain.setClusterEntityAndDefinitionCluster(this.clusterEntityList, this.clusterRelationshipEntityList);
        HandlerMetadataDO handlerMetadataDO = new HandlerMetadataDO();
        clusterMetadataDomain.setRuntimeEntity(this.runtimeEntityList, handlerMetadataDO);
    }

    private void createClusterRelationshipEntity(ClusterEntity mainCluster, ClusterEntity clusterEntity) {
        ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterRelationshipEntity.setClusterId(mainCluster.getId());
        clusterRelationshipEntity.setClusterType(mainCluster.getClusterType());

        clusterRelationshipEntity.setRelationshipId(clusterEntity.getId());
        clusterRelationshipEntity.setRelationshipType(clusterEntity.getClusterType());
        this.clusterRelationshipEntityList.add(clusterRelationshipEntity);
    }

    @Test
    public void test_EventMesh_Cluster() {

        clusterEntity.setId(this.getId());
        clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        clusterEntityList.add(clusterEntity);

        ClusterEntity meshCluster = new ClusterEntity();
        meshCluster.setId(this.getId());
        meshCluster.setClusterType(ClusterType.EVENTMESH_META_NACOS);
        clusterEntityList.add(meshCluster);
        this.createClusterRelationshipEntity(clusterEntity, meshCluster);
        this.createRuntime(meshCluster);

        meshCluster = new ClusterEntity();
        meshCluster.setId(this.getId());
        meshCluster.setClusterType(ClusterType.EVENTMESH_META_NACOS);
        clusterEntityList.add(meshCluster);
        this.createClusterRelationshipEntity(clusterEntity, meshCluster);
        this.createRuntime(meshCluster);

        ClusterEntity runtimeCluster = new ClusterEntity();
        runtimeCluster.setId(this.getId());
        runtimeCluster.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        clusterEntityList.add(runtimeCluster);
        this.createClusterRelationshipEntity(clusterEntity, runtimeCluster);
        this.createRuntime(runtimeCluster);

        runtimeCluster = new ClusterEntity();
        runtimeCluster.setId(this.getId());
        runtimeCluster.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        clusterEntityList.add(runtimeCluster);
        this.createClusterRelationshipEntity(clusterEntity, runtimeCluster);
        this.createRuntime(runtimeCluster);

    }

    public void createRuntime(ClusterEntity cluster) {
        for (int i = 0; i < 5; i++) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setId(this.getId());
            runtimeEntity.setClusterId(cluster.getId());
            runtimeEntity.setClusterType(cluster.getClusterType());
            this.runtimeEntityList.add(runtimeEntity);
        }
    }

    @Test
    public void test_RockerMQ_Cluster() {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(this.getId());
        clusterEntity.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        clusterEntityList.add(clusterEntity);
        this.createClusterRelationshipEntity(this.clusterEntity, clusterEntity);

        ClusterEntity meshCluster = new ClusterEntity();
        meshCluster.setId(this.getId());
        meshCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        clusterEntityList.add(meshCluster);
        this.createClusterRelationshipEntity(clusterEntity, meshCluster);
        this.createRuntime(meshCluster);

        meshCluster = new ClusterEntity();
        meshCluster.setId(this.getId());
        meshCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        clusterEntityList.add(meshCluster);
        this.createClusterRelationshipEntity(clusterEntity, meshCluster);
        this.createRuntime(meshCluster);

        // 创建 broker cluster
        ClusterEntity runtimeCluster = new ClusterEntity();
        runtimeCluster.setId(this.getId());
        runtimeCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        clusterEntityList.add(runtimeCluster);
        this.createClusterRelationshipEntity(clusterEntity, runtimeCluster);

        ClusterEntity mainSlaveCluster = new ClusterEntity();
        mainSlaveCluster.setId(this.getId());
        mainSlaveCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        clusterEntityList.add(mainSlaveCluster);
        this.createClusterRelationshipEntity(runtimeCluster, mainSlaveCluster);
        this.createRuntime(mainSlaveCluster);

        mainSlaveCluster = new ClusterEntity();
        mainSlaveCluster.setId(this.getId());
        mainSlaveCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        clusterEntityList.add(mainSlaveCluster);
        this.createClusterRelationshipEntity(runtimeCluster, mainSlaveCluster);
        this.createRuntime(mainSlaveCluster);

        // 创建 broker cluster
        runtimeCluster = new ClusterEntity();
        runtimeCluster.setId(this.getId());
        runtimeCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        clusterEntityList.add(runtimeCluster);
        this.createClusterRelationshipEntity(clusterEntity, runtimeCluster);

        mainSlaveCluster = new ClusterEntity();
        mainSlaveCluster.setId(this.getId());
        mainSlaveCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        clusterEntityList.add(mainSlaveCluster);
        this.createClusterRelationshipEntity(runtimeCluster, mainSlaveCluster);
        this.createRuntime(mainSlaveCluster);

        mainSlaveCluster = new ClusterEntity();
        mainSlaveCluster.setId(this.getId());
        mainSlaveCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        clusterEntityList.add(mainSlaveCluster);
        this.createClusterRelationshipEntity(runtimeCluster, mainSlaveCluster);
        this.createRuntime(mainSlaveCluster);

        this.handler();
        System.out.println("111");
    }

}
