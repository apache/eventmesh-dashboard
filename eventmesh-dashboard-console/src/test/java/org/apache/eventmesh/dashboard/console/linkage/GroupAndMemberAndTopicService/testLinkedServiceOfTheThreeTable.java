package org.apache.eventmesh.dashboard.console.linkage.GroupAndMemberAndTopicService;

import org.apache.eventmesh.dashboard.console.EventmeshConsoleApplication;
import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.TopicEntity;
import org.apache.eventmesh.dashboard.console.service.group.GroupService;
import org.apache.eventmesh.dashboard.console.service.groupmember.GroupMemberService;
import org.apache.eventmesh.dashboard.console.service.topic.TopicService;


import java.sql.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventmeshConsoleApplication.class)
public class testLinkedServiceOfTheThreeTable {

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private TopicService topicService;
    @Test
    private void testAddMemberIntoGroup(){
        GroupMemberEntity
            groupMemberEntity = new GroupMemberEntity(1L,1L,"test2","z1","admin","active",new Timestamp(System.currentTimeMillis()),new Timestamp(System.currentTimeMillis()));
        groupService.insertMemberToGroup_plus(groupMemberEntity);
    }
    @Test
    private void testDeleteMemberFromGroup(){
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setGroupName("z1");
        groupMemberEntity.setId(2L);
        groupService.deleteMemberFromGroup_plus(groupMemberEntity);
    }
    @Test
    public void testAddTopicForMember(){
        TopicEntity topicEntity = new TopicEntity(1L,1L,"z1","10","10",100L,1,"testTopic",new Timestamp(System.currentTimeMillis()),new Timestamp(System.currentTimeMillis()));
        topicService.addTopic_plus(topicEntity);
    }
    @Test
    public void testDeleteTopicForMember(){
        TopicEntity topicEntity = new TopicEntity(1L,1L,"z1","10","10",100L,1,"testTopic",new Timestamp(System.currentTimeMillis()),new Timestamp(System.currentTimeMillis()));
        topicService.deleteTopic_plus(topicEntity);
    }

}
