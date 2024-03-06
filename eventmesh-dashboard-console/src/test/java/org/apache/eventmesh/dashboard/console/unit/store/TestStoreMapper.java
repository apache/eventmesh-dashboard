package org.apache.eventmesh.dashboard.console.unit.store;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;
import org.apache.eventmesh.dashboard.console.mapper.storage.StoreMapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestStoreMapper {

    @Autowired
    private StoreMapper storeMapper;

    @Test
    public void testAddStore() {
        StoreEntity storeEntity =
            new StoreEntity(null, 1l, 2, "rocketmq", "run1", 1l, "n,j", (short) -1, 1098, 1099, "nothing", (short) 1, null, null, "nothing", 1l);
        StoreEntity storeEntity1 =
            new StoreEntity(null, 1l, 1, "rocketmq", "run1", 1l, "n,j", (short) -1, 1098, 1099, "nothing", (short) 1, null, null, "nothing", 1l);
        storeMapper.addStorage(storeEntity);
        storeMapper.addStorage(storeEntity1);
        List<StoreEntity> storeEntities = storeMapper.selectStoreByCluster(storeEntity);
        storeEntities.forEach(n -> {
            n.setUpdateTime(null);
            n.setCreateTime(null);
        });
        Assert.assertEquals(storeEntities.get(1), storeEntity);
        Assert.assertEquals(storeEntities.get(0), storeEntity1);
    }

    @Test
    public void testDeleteStoreByUnique() {
        StoreEntity storeEntity =
            new StoreEntity(null, 1l, 2, "rocketmq", "run1", 1l, "n,j", (short) -1, 1098, 1099, "nothing", (short) 1, null, null, "nothing", 1l);
        storeMapper.addStorage(storeEntity);
        storeMapper.deleteStoreByUnique(storeEntity);
        List<StoreEntity> storeEntities = storeMapper.selectStoreByCluster(storeEntity);
        Assert.assertEquals(storeEntities.size(), 0);
    }

    @Test
    public void testUpdateStoreByUnique() {
        StoreEntity storeEntity =
            new StoreEntity(null, 1l, 2, "rocketmq", "run1", 1l, "n,j", (short) -1, 1098, 1099, "nothing", (short) 1, null, null, "nothing", 1l);
        storeMapper.addStorage(storeEntity);
        storeEntity.setStatus((short) 5);
        storeMapper.updateStoreByUnique(storeEntity);
        List<StoreEntity> storeEntities = storeMapper.selectStoreByCluster(storeEntity);
        Assert.assertEquals(storeEntities.size(), 1);
        Assert.assertEquals(storeEntities.get(0).getStatus(), storeEntity.getStatus());
    }
}
