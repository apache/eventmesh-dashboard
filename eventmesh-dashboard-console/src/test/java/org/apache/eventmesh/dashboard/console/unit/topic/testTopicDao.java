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
public class testTopicDao {

    @Autowired
    private TopicMapper topicDao;

    public List<TopicEntity> insertGroupData(String topicName) {
        List<TopicEntity> topicEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TopicEntity topicEntity = new TopicEntity(null, (long) i, topicName, "10", "10", 100L, 1, "testTopic", null, null);
            topicDao.addTopic(topicEntity);
            topicEntities.add(topicEntity);
        }
        return topicEntities;
    }

    public List<TopicEntity> getRemovedTimeList(String topicName, Long clusterId) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicName(topicName);
        topicEntity.setClusterId(clusterId);
        List<TopicEntity> topicEntities = topicDao.getTopicListByDynamic(topicEntity);
        for (TopicEntity topic : topicEntities) {
            topic.setCreateTime(null);
            topic.setUpdateTime(null);
        }
        return topicEntities;
    }

    @Test
    public void test_selectTopicByClusterId() {
        List<TopicEntity> topicEntities = this.insertGroupData("SelectById111");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(topicEntities.get(9).getClusterId());
        List<TopicEntity> topicEntity1 = topicDao.getTopicListByDynamic(topicEntity);
        topicEntity1.get(0).setCreateTime(null);
        topicEntity1.get(0).setUpdateTime(null);
        Assert.assertEquals(topicEntity1.get(0), topicEntities.get(9));
        Assert.assertEquals(1, topicEntity1.size());
    }

    @Test
    public void test_addTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("add111");
        List<TopicEntity> add111 = this.getRemovedTimeList("add111", null);
        Assert.assertEquals(add111, topicEntities);
    }

    @Test
    public void test_UpdateTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("update2");
        topicEntities.get(5).setDescription("updateTest1");
        topicEntities.get(5).setType(-1);
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setDescription("updateTest1");
        topicEntity.setType(-1);
        topicEntity.setId(topicEntities.get(5).getId());
        topicDao.updateTopic(topicEntity);
        TopicEntity topicEntity1 = topicDao.selectTopicById(topicEntity);
        topicEntity1.setUpdateTime(null);
        topicEntity1.setCreateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(5));
    }

    @Test
    public void test_DeleteTopic() {
        List<TopicEntity> topicEntities = this.insertGroupData("update72");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicEntities.get(5).getId());
        topicEntity.setClusterId(topicEntities.get(5).getClusterId());
        topicEntity.setTopicName("update72");
        topicDao.deleteTopic(topicEntity);
        List<TopicEntity> topicEntity1 = topicDao.getTopicListByDynamic(topicEntity);
        Assert.assertEquals(true, topicEntity1.isEmpty());
    }

    @Test
    public void test_selectTopicByUnique() {
        List<TopicEntity> topicEntities = this.insertGroupData("unique11");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopicName("unique11");
        topicEntity.setClusterId(topicEntities.get(1).getClusterId());
        TopicEntity topicEntity1 = topicDao.selectTopicByUnique(topicEntity);
        topicEntity1.setUpdateTime(null);
        topicEntity1.setCreateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(1));
    }

    @Test
    public void test_selectTopicById() {
        List<TopicEntity> topicEntities = this.insertGroupData("id1");
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(topicEntities.get(2).getId());
        TopicEntity topicEntity1 = topicDao.selectTopicById(topicEntity);
        topicEntity1.setCreateTime(null);
        topicEntity1.setUpdateTime(null);
        Assert.assertEquals(topicEntity1, topicEntities.get(2));
    }

}
