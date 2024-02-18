package org.apache.eventmesh.dashboard.console.linkage.log;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.group.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.log.LogEntity;
import org.apache.eventmesh.dashboard.console.service.group.GroupService;
import org.apache.eventmesh.dashboard.console.service.log.LogService;

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
        GroupEntity groupEntity = new GroupEntity(null, 1L, "logTest", 0, null, 1, "OK", null, null);
        GroupEntity groupEntity1 = groupService.addGroup(groupEntity);
        LogEntity logEntity = new LogEntity(null, 1L, "add", "Group", 2, groupEntity1.toString(), null, null, null, null);
        logEntity.setResult(groupEntity.toString());
        logEntity.setId(groupEntity1.getId());
        List<LogEntity> logListByCluster = logService.getLogListByCluster(logEntity);
        logListByCluster.get(0).setId(null);
        logListByCluster.get(0).setCreateTime(null);
        logListByCluster.get(0).setEndTime(null);
        Assert.assertEquals(logListByCluster.get(0), logEntity);
        Assert.assertEquals(logListByCluster.size(), 1);
    }

}
