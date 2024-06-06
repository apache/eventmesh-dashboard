/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import lombok.Builder;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestConfigMapper {

    @Autowired
    private ConfigMapper configMapper;

    private ConfigEntity config;

    private ConfigEntity config1;

    @Builder
    public void init(){
        this.config = new ConfigEntity();
        this.config.setClusterId(1L);
        this.config.setBusinessType("rocketmq");
        this.config.setInstanceType(2);
        this.config.setInstanceId(2L);
        this.config.setConfigName("port");
        this.config.setConfigValue("127.0.0.1");
        this.config.setStartVersion("1.0.0");
        this.config.setEndVersion("1.1.1");
        this.config.setDiffType(1);
        this.config.setDescription("1.1.1.1.1");
        this.config.setEdit(-1);

        this.config1 = new ConfigEntity();
        this.config1.setClusterId(1L);
        this.config1.setBusinessType("rocketmq");
        this.config1.setInstanceType(2);
        this.config1.setInstanceId(2L);
        this.config1.setConfigName("port");
        this.config1.setConfigValue("127.0.0.1");
        this.config1.setStartVersion("1.0.0");
        this.config1.setEndVersion("1.1.1");
        this.config1.setDiffType(1);
        this.config1.setDescription("1.1.1.1.1");
        this.config1.setEdit(-1);
    }

    @Test
    public void testAddConfig() throws IllegalAccessException {
        configMapper.addConfig(config);
        ConfigEntity configEntity = configMapper.selectByUnique(config);
        configEntity.setUpdateTime(null);
        configEntity.setCreateTime(null);
        Assert.assertEquals(config.getId(), configEntity.getId());
        Assert.assertEquals(config, configEntity);
    }

    @Test
    public void testDeleteConfig() {
        configMapper.addConfig(config);
        configMapper.deleteConfig(config);
        ConfigEntity config1 = configMapper.selectByUnique(config);
        Assert.assertEquals(config1, null);
    }

    @Test
    public void testSelectByInstanceId() {
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
        configMapper.addConfig(config);
        config.setConfigValue("127.1.1.1");
        configMapper.updateConfig(config);
        ConfigEntity configEntity = configMapper.selectByUnique(config);
        configEntity.setUpdateTime(null);
        configEntity.setCreateTime(null);
        Assert.assertEquals(configEntity, config);
    }
}

