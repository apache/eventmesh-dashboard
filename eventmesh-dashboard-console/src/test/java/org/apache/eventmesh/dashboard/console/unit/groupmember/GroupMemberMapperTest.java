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

package org.apache.eventmesh.dashboard.console.unit.groupmember;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.groupmember.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.groupmember.OprGroupMemberMapper;

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
public class GroupMemberMapperTest {

    @Autowired
    OprGroupMemberMapper groupMemberMapper;

    public List<GroupMemberEntity> insertGroupData(String topicName, String groupName) {
        List<GroupMemberEntity> groupMemberEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupMemberEntity groupMemberEntity = new GroupMemberEntity(null, (long) i, topicName, groupName, "admin", "active", null, null);
            groupMemberMapper.addGroupMember(groupMemberEntity);
            groupMemberEntities.add(groupMemberEntity);
        }
        return groupMemberEntities;
    }

    public List<GroupMemberEntity> getRemovedTimeList(String topicName, String groupName) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setTopicName(topicName);
        groupMemberEntity.setGroupName(groupName);
        List<GroupMemberEntity> groupEntities = groupMemberMapper.selectMember(groupMemberEntity);
        for (GroupMemberEntity groupEntity1 : groupEntities) {
            groupEntity1.setCreateTime(null);
            groupEntity1.setUpdateTime(null);
        }
        return groupEntities;
    }

    @Test
    public void testAddGroupMember() {
        List<GroupMemberEntity> add1 = this.insertGroupData("add1", "groupMember");
        Assert.assertEquals(add1, this.getRemovedTimeList("add1", "groupMember"));
    }

    @Test
    public void testGetGroupMemberByClusterId() {
        List<GroupMemberEntity> add1 = this.insertGroupData("getByCluster", "groupMember");
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(add1.get(1).getClusterId());
        List<GroupMemberEntity> groupByClusterId = groupMemberMapper.getGroupByClusterId(groupMemberEntity);
        GroupMemberEntity groupMemberEntity1 = groupByClusterId.get(0);
        groupMemberEntity1.setCreateTime(null);
        groupMemberEntity1.setUpdateTime(null);
        Assert.assertEquals(1, groupByClusterId.size());
        Assert.assertEquals(add1.get(1), groupMemberEntity1);
    }

    @Test
    public void testDeleteGroupMemberById() {
        List<GroupMemberEntity> add1 = this.insertGroupData("getById", "groupMember");
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setId(add1.get(2).getId());
        GroupMemberEntity groupMemberEntity1 = groupMemberMapper.selectGroupMemberById(groupMemberEntity);
        groupMemberEntity1.setUpdateTime(null);
        groupMemberEntity1.setCreateTime(null);
        Assert.assertEquals(groupMemberEntity1, add1.get(2));
    }

    @Test
    public void testUpdateGroupMemberById() {
        List<GroupMemberEntity> add1 = this.insertGroupData("updateById", "groupMember");
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        add1.get(1).setState("fail1");
        groupMemberEntity.setState("fail1");
        groupMemberEntity.setId(add1.get(1).getId());
        groupMemberMapper.updateGroupMember(groupMemberEntity);
        GroupMemberEntity groupMemberEntity1 = groupMemberMapper.selectGroupMemberById(add1.get(1));
        groupMemberEntity1.setUpdateTime(null);
        groupMemberEntity1.setCreateTime(null);
        Assert.assertEquals(groupMemberEntity1, add1.get(1));
    }

    @Test
    public void testSelectGroupMemberByUnique() {
        List<GroupMemberEntity> groupMemberEntities = this.insertGroupData("selectByUnique", "groupMember");
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setClusterId(groupMemberEntities.get(1).getClusterId());
        groupMemberEntity.setTopicName(groupMemberEntities.get(1).getTopicName());
        groupMemberEntity.setGroupName(groupMemberEntities.get(1).getGroupName());
        GroupMemberEntity groupMemberEntity1 = groupMemberMapper.selectGroupMemberByUnique(groupMemberEntity);
        groupMemberEntity1.setUpdateTime(null);
        groupMemberEntity1.setCreateTime(null);
        Assert.assertEquals(groupMemberEntity1, groupMemberEntities.get(1));
    }

    @Test
    public void testSelectGroupMemberByGroup() {
        List<GroupMemberEntity> groupMemberEntities = this.insertGroupData("selectByGroup1", "groupMember1");
        List<GroupMemberEntity> removedTimeList = this.getRemovedTimeList(null, "groupMember1");
        Assert.assertEquals(groupMemberEntities, removedTimeList);
    }

    @Test
    public void testSelectGroupMemberByTopic() {
        List<GroupMemberEntity> groupMemberEntities = this.insertGroupData("selectByTopic1", "groupMember2");
        List<GroupMemberEntity> removedTimeList = this.getRemovedTimeList("selectByTopic1", null);
        Assert.assertEquals(groupMemberEntities, removedTimeList);
    }

    @Test
    public void testUpdateGroupMemberByTopic() {
        List<GroupMemberEntity> groupMemberEntities = this.insertGroupData("updateByTopic1", "groupMember2");
        for (GroupMemberEntity groupMemberEntity : groupMemberEntities) {
            groupMemberEntity.setState("fail2");
        }
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setState("fail2");
        groupMemberEntity.setTopicName("updateByTopic1");
        groupMemberMapper.updateMemberByTopic(groupMemberEntity);
        Assert.assertEquals(this.getRemovedTimeList("updateByTopic1", null), groupMemberEntities);
    }

    @Test
    public void testSelectGroupMemberById() {
        List<GroupMemberEntity> groupMemberEntities = this.insertGroupData("updateById1", "groupMember2");
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setId(groupMemberEntities.get(5).getId());
        GroupMemberEntity groupMemberEntity1 = groupMemberMapper.selectGroupMemberById(groupMemberEntity);
        groupMemberEntity1.setCreateTime(null);
        groupMemberEntity1.setUpdateTime(null);
        Assert.assertEquals(groupMemberEntity1, groupMemberEntities.get(5));
    }

}
