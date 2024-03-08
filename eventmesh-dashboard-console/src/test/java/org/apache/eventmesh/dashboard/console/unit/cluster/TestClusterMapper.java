package org.apache.eventmesh.dashboard.console.unit.cluster;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestClusterMapper {

    @Autowired
    private ClusterMapper clusterMapper;

    @Test
    public void testAddCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        clusterMapper.addCluster(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        clusterEntity1.setUpdateTime(null);
        clusterEntity1.setCreateTime(null);
        Assert.assertEquals(clusterEntity1, clusterEntity);
    }

    @Test
    public void testSelectAllCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        ClusterEntity clusterEntity1 =
            new ClusterEntity(null, "c1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        clusterMapper.addCluster(clusterEntity);
        clusterMapper.addCluster(clusterEntity1);
        List<ClusterEntity> clusterEntities = clusterMapper.selectAllCluster();
        Assert.assertEquals(clusterEntities.size(), 2);
    }

    @Test
    public void testSelectClusterById() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        clusterMapper.addCluster(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        clusterEntity1.setCreateTime(null);
        clusterEntity1.setUpdateTime(null);
        Assert.assertEquals(clusterEntity1, clusterEntity);
    }

    @Test
    public void testUpdateCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        clusterMapper.addCluster(clusterEntity);
        clusterEntity.setDescription("nothing");
        clusterEntity.setName("cl2");
        clusterEntity.setAuthType(1);
        clusterEntity.setBootstrapServers("1999");
        clusterEntity.setClientProperties("nothing");
        clusterEntity.setEventmeshVersion("1.10.0");
        clusterEntity.setJmxProperties("nothing");
        clusterEntity.setRegisterNameList("1.23.18");
        clusterEntity.setRunState(1);
        clusterEntity.setRegProperties("nothing");
        clusterMapper.updateClusterById(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        clusterEntity1.setCreateTime(null);
        clusterEntity1.setUpdateTime(null);
        Assert.assertEquals(clusterEntity1, clusterEntity);
    }

    @Test
    public void testDeleteCluster() {
        ClusterEntity clusterEntity =
            new ClusterEntity(null, "cl1", "registerList", "server", "1.7.0", "null", "null", "null", "no", 0, 0, null, null);
        clusterMapper.addCluster(clusterEntity);
        clusterMapper.deleteClusterById(clusterEntity);
        ClusterEntity clusterEntity1 = clusterMapper.selectClusterById(clusterEntity);
        Assert.assertEquals(clusterEntity1, null);
    }
}
