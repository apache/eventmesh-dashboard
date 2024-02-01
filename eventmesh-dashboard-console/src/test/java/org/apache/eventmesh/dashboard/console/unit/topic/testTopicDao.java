package org.apache.eventmesh.dashboard.console.unit.topic;

import org.apache.eventmesh.dashboard.console.EventmeshConsoleApplication;
import org.apache.eventmesh.dashboard.console.entity.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.TopicDao;

import java.sql.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventmeshConsoleApplication.class)
public class testTopicDao {

    @Autowired
    private TopicDao topicDao;
    @Test
    public void testSelectTopicByClusterId(){
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(1L);
        System.out.println(topicDao.getTopicListByClusterId(topicEntity));
    }
    @Test
    public void testAddTopic(){
        TopicEntity topicEntity = new TopicEntity(1L,1L,"z1","10","10",100L,1,"testTopic",new Timestamp(System.currentTimeMillis()),new Timestamp(System.currentTimeMillis()));
        topicDao.addTopic(topicEntity);
    }
    @Test
    public void testUpdateTopic(){
        TopicEntity topicEntity = new TopicEntity(1L,1L,"z1","10","10",100L,1,"testTopic",new Timestamp(System.currentTimeMillis()),new Timestamp(System.currentTimeMillis()));
        topicDao.updateTopic(topicEntity);
    }
    @Test
    public void testDeleteTopic(){
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(1L);
        topicDao.deleteTopic(topicEntity);
    }
    @Test
    public void testSelectTopicByUnique(){
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(1L);
        topicEntity.setTopicName("z1");
        System.out.println((topicDao.selectTopicByUnique(topicEntity)));
    }
    @Test
    public void testSelectTopicById(){
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setId(1L);
        System.out.println(topicDao.selectTopicById(topicEntity));
    }

}
