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

package org.apache.eventmesh.dashboard.console.unit.group;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.group.GroupEntity;
import org.apache.eventmesh.dashboard.console.mapper.group.OprGroupMapper;

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
public class GroupMapperTest {

    @Autowired
    private OprGroupMapper groupMapper;

    public List<GroupEntity> insertGroupData(String name) {
        List<GroupEntity> groupEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupEntity groupEntity = new GroupEntity(null, (long) i, name, 0, null, 1, "OK", null, null);
            groupMapper.addGroup(groupEntity);
            groupEntities.add(groupEntity);
        }
        return groupEntities;
    }

    public List<GroupEntity> getRemovedTimeList(String name) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(name);
        List<GroupEntity> groupEntities = groupMapper.selectGroup(groupEntity);
        for (GroupEntity groupEntity1 : groupEntities) {
            groupEntity1.setCreateTime(null);
            groupEntity1.setUpdateTime(null);
        }
        return groupEntities;
    }

    @Test
    public void testAddGroup() {
        List<GroupEntity> groupEntities = this.insertGroupData("addGroup");
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName("addGroup");
        List<GroupEntity> groupEntities1 = groupMapper.selectGroup(groupEntity);
        Assert.assertEquals(groupEntities, this.getRemovedTimeList("addGroup"));
    }

    @Test
    public void testUpdateGroupById() {
        List<GroupEntity> groupEntities = this.insertGroupData("updateById2");
        GroupEntity groupEntity = groupEntities.get(9);
        groupEntity.setType(3);
        groupEntity.setMembers("1,");
        groupEntity.setState("fail");
        groupEntity.setMemberCount(1);
        groupMapper.updateGroup(groupEntity);
        Assert.assertEquals(groupEntities, this.getRemovedTimeList("updateById2"));
    }

    @Test
    public void testDeleteGroupById() {
        List<GroupEntity> groupEntities = this.insertGroupData("deleteById");
        GroupEntity groupEntity = groupEntities.get(9);
        groupMapper.deleteGroup(groupEntity);
        groupEntities.remove(9);
        Assert.assertEquals(groupEntities, this.getRemovedTimeList("deleteById"));
    }

    @Test
    public void testSelectGroupById() {
        List<GroupEntity> groupEntities = this.insertGroupData("selectById");
        GroupEntity groupEntity = groupMapper.selectGroupById(groupEntities.get(0));
        groupEntity.setCreateTime(null);
        groupEntity.setUpdateTime(null);
        Assert.assertEquals(groupEntities.get(0), groupEntity);
    }

    @Test
    public void testSelectGroupByClusterId() {
        List<GroupEntity> groupEntities = this.insertGroupData("selectByUnique");
        GroupEntity groupEntity1 = new GroupEntity();
        groupEntity1.setClusterId(groupEntities.get(0).getClusterId());
        groupEntity1.setName(groupEntities.get(0).getName());
        GroupEntity groupEntity = groupMapper.selectGroupByUnique(groupEntity1);
        groupEntity.setCreateTime(null);
        groupEntity.setUpdateTime(null);
        Assert.assertEquals(groupEntities.get(0), groupEntity);
    }

    @Test
    public void testSelectGroup() {
        List<GroupEntity> groupEntities = this.insertGroupData("selectByDynamic1");
        Assert.assertEquals(groupEntities, this.getRemovedTimeList("Dynamic1"));
    }
}
