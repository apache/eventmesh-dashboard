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

package org.apache.eventmesh.dashboard.console.unit.topic;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.topic.TopicMapper;

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
public class TopicMapperTest {

    @Autowired
    private TopicMapper topicMapper;

    public List<TopicEntity> insertGroupData(String topicName) {
        List<TopicEntity> topicEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TopicEntity topicEntity = new TopicEntity(null, (long) i, topicName, "10", "10", 100L, 1, "testTopic", null, null);
            topicMapper.addTopic(topicEntity);
            topicEntities.add(topicEntity);
        }
        return topicEntities;
    }

    public List<TopicEntity> getRemovedTimeList(String topicName, Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicName(topicName);
        topicEntity.setClusterId(clusterId);
        List<TopicEntity> topicEntities = topicMapper.getTopicList(topicEntity);
        for (TopicEntity topic : topicEntities) {
            topic.setCreateTime(null);
            topic.setUpdateTime(null);
        }
        return topicEntities;
    }

    @Test
    public void testSelectTopicByClusterId() {
        List<TopicEntity> topicEntities = this.insertGroupData("SelectById111");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(topicEntities.get(9).getClusterId());
        List<TopicEntity> topicEntity1 = topicMapper.getTopicList(topicEntity);
        topicEntity1.get(0).setCreateTime(null);
        topicEntity1.get(0).setUpdateTime(null);
        Assert.assertEquals(topicEntity1.get(0), topicEntities.get(9));
        Assert.assertEquals(1, topicEntity1.size());
    }

    @Test
    public void testAddTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("add111");
        List<TopicEntity> add111 = this.getRemovedTimeList("add111", null);
        Assert.assertEquals(add111, topicEntities);
    }

    @Test
    public void testUpdateTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("update2");
        topicEntities.get(5).setDescription("updateTest1");
        topicEntities.get(5).setType(-1);
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setDescription("updateTest1");
        topicEntity.setType(-1);
        topicEntity.setId(topicEntities.get(5).getId());
        topicMapper.updateTopic(topicEntity);
        TopicEntity topicEntity1 = topicMapper.selectTopicById(topicEntity);
        topicEntity1.setUpdateTime(null);
        topicEntity1.setCreateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(5));
    }

    @Test
    public void testDeleteTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("update72");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicEntities.get(5).getId());
        topicEntity.setClusterId(topicEntities.get(5).getClusterId());
        topicEntity.setTopicName("update72");
        topicMapper.deleteTopic(topicEntity);
        List<TopicEntity> topicEntity1 = topicMapper.getTopicList(topicEntity);
        Assert.assertEquals(true, topicEntity1.isEmpty());
    }

    @Test
    public void testSelectTopicByUnique() {
        List<TopicEntity> topicEntities = this.insertGroupData("unique11");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicName("unique11");
        topicEntity.setClusterId(topicEntities.get(1).getClusterId());
        TopicEntity topicEntity1 = topicMapper.selectTopicByUnique(topicEntity);
        topicEntity1.setUpdateTime(null);
        topicEntity1.setCreateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(1));
    }

    @Test
    public void testSelectTopicById() {
        List<TopicEntity> topicEntities = this.insertGroupData("id1");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicEntities.get(2).getId());
        TopicEntity topicEntity1 = topicMapper.selectTopicById(topicEntity);
        topicEntity1.setCreateTime(null);
        topicEntity1.setUpdateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(2));
    }

}
