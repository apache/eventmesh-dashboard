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

package org.apache.eventmesh.dashboard.console.mapper.message;


import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
@Sql(scripts = {"classpath:eventmesh-dashboard.sql"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class TopicEntityMapperTest {

    @Autowired
    private TopicMapper topicMapper;

    private static AtomicLong nameIndex = new AtomicLong();

    @Test
    public void test_batch_delete() {
        this.test_batch_insert();
        List<TopicEntity> topicEntityList = this.topicMapper.selectAll();
        int num = this.topicMapper.deleteTopicByIds(topicEntityList);
        Assert.assertEquals(num, topicEntityList.size());
        
        List<TopicEntity> newTopicEntityList = this.topicMapper.selectAll();
        Assert.assertEquals(0, newTopicEntityList.size());

    }

    @Test
    public void test_batch_insert() {
        List<TopicEntity> topicEntityList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            topicEntityList.add(createTopicEntity());
        }
        topicMapper.batchInsert(topicEntityList);

        List<TopicEntity> newTopicEntityList = this.topicMapper.selectAll();
        Assert.assertEquals(topicEntityList.size(), newTopicEntityList.size());
    }

    @Test
    public void test_insertTopic() {
        TopicEntity topicEntity = createTopicEntity();
        topicMapper.insertTopic(topicEntity);

        topicEntity = topicMapper.queryTopicById(topicEntity);
        Assert.assertNotNull(topicEntity);

    }


    public static TopicEntity createTopicEntity() {
        return createTopicEntity(1L, 2L);
    }

    public static TopicEntity createTopicEntity(Long clusterId, Long topicId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(1L);
        topicEntity.setRuntimeId(2L);
        topicEntity.setTopicName("test-" + nameIndex.getAndIncrement());
        topicEntity.setTopicType("topicType");
        topicEntity.setWriteQueueNum(8);
        topicEntity.setReadQueueNum(8);
        topicEntity.setReplicationFactor(2);
        topicEntity.setTopicFilterType("topicFilterType");
        topicEntity.setAttributes("1");
        topicEntity.setOrder(1);
        topicEntity.setRetentionMs(1L);
        topicEntity.setDescription("desc");
        topicEntity.setCreateProgress(1);
        return topicEntity;
    }


}
