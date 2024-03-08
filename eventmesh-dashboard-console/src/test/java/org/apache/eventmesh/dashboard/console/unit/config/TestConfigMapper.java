package org.apache.eventmesh.dashboard.console.unit.config;


import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.config.ConfigMapper;


import java.util.ArrayList;

import java.util.List;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestConfigMapper {

    @Autowired
    private ConfigMapper configMapper;

    @Test
    public void testAddConfig() throws IllegalAccessException {
        ConfigEntity config = new ConfigEntity(null, 1L, "rocketmq", 2, 2L, "port",
            "127.0.0.1", "1.7.0", "1.8.0", 1, "1.10.0", -1, "666", 0, null, null, 0, 0);
        configMapper.addConfig(config);
        ConfigEntity configEntity = configMapper.selectByUnique(config);
        configEntity.setUpdateTime(null);
        configEntity.setCreateTime(null);
        Assert.assertEquals(config.getId(), configEntity.getId());
        Assert.assertEquals(config, configEntity);
    }

    @Test
    public void testDeleteConfig() {
        ConfigEntity config = new ConfigEntity(null, 1L, "rocketmq", 2, 2L, "port",
            "127.0.0.1", "1.7.0", "1.8.0", 1, "1.10.0", -1, "666", 0, null, null, 0, 0);
        configMapper.addConfig(config);
        configMapper.deleteConfig(config);
        ConfigEntity config1 = configMapper.selectByUnique(config);
        Assert.assertEquals(config1, null);
    }

    @Test
    public void testSelectByInstanceId() {
        ConfigEntity config = new ConfigEntity(null, 1L, "rocketmq", 2, 2L, "port",
            "127.0.0.1", "1.7.0", "1.8.0", 1, "1.10.0", -1, "666", 0, null, null, 0, 0);
        ConfigEntity config1 = new ConfigEntity(null, 1L, "rocketmq", 2, 2L, "name",
            "127.0.0.1", "1.7.0", "1.8.0", 1, "1.10.0", -1, "666", 0, null, null, 0, 0);
        configMapper.addConfig(config1);
        configMapper.addConfig(config);
        List<ConfigEntity> configEntityList = new ArrayList<>();
        configEntityList.add(config1);
        configEntityList.add(config);
        List<ConfigEntity> configEntityList1 = configMapper.selectByInstanceId(config1);
        configEntityList1.forEach(n -> {
            n.setCreateTime(null);
            n.setUpdateTime(null);
        });
        Assert.assertEquals(configEntityList, configEntityList1);
    }

    @Test
    public void testSelectDefaultConfig() {
        ConfigEntity config = new ConfigEntity();
        config.setBusinessType("rocketmq");
        config.setInstanceType(1);
        List<ConfigEntity> configEntityList = configMapper.selectDefaultConfig(config);
        Assert.assertNotEquals(configEntityList.size(), 0);
    }

    @Test
    public void testUpdateConfig() {
        ConfigEntity config = new ConfigEntity(null, 1L, "rocketmq", 2, 2L, "port",
            "127.0.0.1", "1.7.0", "1.8.0", 1, "1.10.0", -1, "666", 2, null, null, 0, 0);
        configMapper.addConfig(config);
        config.setConfigValue("127.1.1.1");
        configMapper.updateConfig(config);
        ConfigEntity configEntity = configMapper.selectByUnique(config);
        configEntity.setUpdateTime(null);
        configEntity.setCreateTime(null);
        Assert.assertEquals(configEntity, config);
    }
}

