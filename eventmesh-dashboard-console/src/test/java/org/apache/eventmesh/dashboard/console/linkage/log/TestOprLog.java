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

package org.apache.eventmesh.dashboard.console.linkage.log;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.function.LogEntity;
import org.apache.eventmesh.dashboard.console.modle.dto.log.GetLogListDTO;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;
import org.apache.eventmesh.dashboard.console.service.function.LogService;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class TestOprLog {

    @Autowired
    private GroupService groupService;

    @Autowired
    private LogService logService;

    @Test
    public void testGroupServiceOprLog() {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(1L);
        groupEntity.setName("logTest");
        groupEntity.setStatus(1);
        groupEntity.setType(0);
        groupEntity.setState("OS");
        groupService.insertGroup(groupEntity);
        LogEntity logEntity = new LogEntity();
        logEntity.setClusterId(1L);
        logEntity.setOperationType("add");
        logEntity.setOperationUser("Group");
        logEntity.setContent(groupEntity.toString());
        logEntity.setResult(groupEntity.toString());
        logEntity.setId(groupEntity.getId());
        List<LogEntity> logListByCluster = logService.selectLogListByCluster(new GetLogListDTO());
        logListByCluster.get(0).setId(null);
        logListByCluster.get(0).setCreateTime(null);
        logListByCluster.get(0).setEndTime(null);
        Assert.assertEquals(logListByCluster.get(0), logEntity);
        Assert.assertEquals(1, logListByCluster.size());
    }

}
