package org.apache.eventmesh.dashboard.console.unit.group;

import org.apache.eventmesh.dashboard.console.EventmeshConsoleApplication;
import org.apache.eventmesh.dashboard.console.entity.GroupEntity;
import org.apache.eventmesh.dashboard.console.mapper.OprGroupDao;
import org.apache.eventmesh.dashboard.console.service.GroupService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventmeshConsoleApplication.class)
public class TestGroupDao {

    @Autowired
    private OprGroupDao groupDao;
    public List<GroupEntity> insertGroupData(String name){
        List<GroupEntity> groupEntities = new ArrayList<>();
        for(int i=0;i<10;i++){
             GroupEntity groupEntity = new GroupEntity(null, (long) i, name, 0, null, 1, "OK", null, null);
            groupDao.addGroup(groupEntity);
            groupEntities.add(groupEntity);
        }
        return groupEntities;
    }

    public List<GroupEntity> getRemovedTimeList(String name){
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(name);
        List<GroupEntity> groupEntities = groupDao.selectGroupByDynamic(groupEntity);
        for(GroupEntity groupEntity1:groupEntities){
            groupEntity1.setCreateTime(null);
            groupEntity1.setUpdateTime(null);
        }
        return groupEntities;
    }
    @Test
    public  void testAddGroup(){
        List<GroupEntity> groupEntities = this.insertGroupData("addGroup");
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName("addGroup");
        List<GroupEntity> groupEntities1 = groupDao.selectGroupByDynamic(groupEntity);
        Assert.assertEquals(groupEntities, this.getRemovedTimeList("addGroup"));
    }
    @Test
    public  void testUpdateGroupById(){
        List<GroupEntity> groupEntities = this.insertGroupData("updateById2");
        GroupEntity groupEntity = groupEntities.get(9);
        groupEntity.setType(3);
        groupEntity.setMembers("1,");
        groupEntity.setState("fail");
        groupEntity.setMemberCount(1);
        groupDao.updateGroup(groupEntity);
        Assert.assertEquals(groupEntities,this.getRemovedTimeList("updateById2"));
    }
    @Test
    public void testDeleteGroupById(){
        List<GroupEntity> groupEntities = this.insertGroupData("deleteById");
        GroupEntity groupEntity = groupEntities.get(9);
        groupDao.deleteGroup(groupEntity);
        groupEntities.remove(9);
        Assert.assertEquals(groupEntities,this.getRemovedTimeList("deleteById"));
    }
    @Test
    public void testSelectGroupById(){
        List<GroupEntity> groupEntities = this.insertGroupData("selectById");
        GroupEntity groupEntity = groupDao.selectGroupById(groupEntities.get(0));
        groupEntity.setCreateTime(null);
        groupEntity.setUpdateTime(null);
        Assert.assertEquals(groupEntities.get(0),groupEntity);
    }
    @Test
    public void testSelectGroupByClusterId(){
        List<GroupEntity> groupEntities = this.insertGroupData("selectByUnique");
        GroupEntity groupEntity1 = new GroupEntity();
        groupEntity1.setClusterId(groupEntities.get(0).getClusterId());
        groupEntity1.setName(groupEntities.get(0).getName());
        GroupEntity groupEntity = groupDao.selectGroupByUnique(groupEntity1);
        groupEntity.setCreateTime(null);
        groupEntity.setUpdateTime(null);
        Assert.assertEquals(groupEntities.get(0),groupEntity);
    }

    @Test
    public void test_selectGroupByDynamic(){
        List<GroupEntity> groupEntities = this.insertGroupData("selectByDynamic");
        Assert.assertEquals(groupEntities,this.getRemovedTimeList("Dynamic"));
    }
}
