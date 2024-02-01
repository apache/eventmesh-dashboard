package org.apache.eventmesh.dashboard.console.unit.groupmember;

import org.apache.eventmesh.dashboard.console.EventmeshConsoleApplication;
import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.groupmember.OprGroupMemberMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventmeshConsoleApplication.class)
public class testGroupMemberDao {

    @Autowired
    OprGroupMemberMapper groupMemberDao;

    public List<GroupMemberEntity> insertGroupData(String topicName, String groupName) {
        List<GroupMemberEntity> groupMemberEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupMemberEntity groupMemberEntity = new GroupMemberEntity(null, (long) i, topicName, groupName, "admin", "active", null, null);
            groupMemberDao.addGroupMember(groupMemberEntity);
            groupMemberEntities.add(groupMemberEntity);
        }
        return groupMemberEntities;
    }

    public List<GroupMemberEntity> getRemovedTimeList(String topicName, String groupName) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicName);
        groupMemberEntity.setGroupName(groupName);
        List<GroupMemberEntity> groupEntities = groupMemberDao.selectAllMemberByDynamic(groupMemberEntity);
        for (GroupMemberEntity groupEntity1 : groupEntities) {
            groupEntity1.setCreateTime(null);
            groupEntity1.setUpdateTime(null);
        }
        return groupEntities;
    }

    @Test
    public void testAddGroupMember() {
        GroupMemberEntity groupMemberEntity =
            new GroupMemberEntity(1L, 1L, "test_GroupMember", "z1", "admin", "active", new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));
        groupMemberDao.addGroupMember(groupMemberEntity);
    }

    @Test
    public void testGetGroupMemberByClusterId() {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(1L);
        System.out.println(groupMemberDao.getGroupByClusterId(groupMemberEntity));
    }

    @Test
    public void testDeleteGroupMemberById() {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setId(1L);
        groupMemberDao.deleteGroupMember(groupMemberEntity);
    }

    @Test
    public void testUpdateGroupMemberById() {
        GroupMemberEntity groupMemberEntity =
            new GroupMemberEntity(1L, 1L, "test_GroupMember", "z1", "admin", "active", new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));
        groupMemberDao.updateGroupMember(groupMemberEntity);
    }

    @Test
    public void testSelectGroupMemberByUnique() {
        GroupMemberEntity groupMemberEntity =
            new GroupMemberEntity(1L, 1L, "test_GroupMember", "z1", "admin", "active", new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));
        System.out.println(groupMemberDao.selectGroupMemberByUnique(groupMemberEntity));
    }

    @Test
    public void testSelectGroupMemberByGroup() {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(1L);
        groupMemberEntity.setGroupName("z1");
        System.out.println(groupMemberDao.selectAllMemberByDynamic(groupMemberEntity));
    }

    @Test
    public void testSelectGroupMemberByTopic() {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName("test_GroupMember");
        System.out.println(groupMemberDao.selectAllMemberByDynamic(groupMemberEntity));
    }

    @Test
    public void testUpdateGroupMemberByTopic() {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName("test_GroupMember");
        groupMemberDao.updateMemberByTopic(groupMemberEntity);
    }

    @Test
    public void test_selectGroupMemberById() {
        GroupMemberEntity groupMemberEntity =
            new GroupMemberEntity(1L, 1L, "test_GroupMember", "z1", "admin", "active", new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));
        System.out.println(groupMemberDao.selectGroupMemberById(groupMemberEntity));
    }

}
