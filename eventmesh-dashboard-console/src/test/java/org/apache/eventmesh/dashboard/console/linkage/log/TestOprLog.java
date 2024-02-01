package org.apache.eventmesh.dashboard.console.linkage.log;

import org.apache.eventmesh.dashboard.console.EventmeshConsoleApplication;
import org.apache.eventmesh.dashboard.console.entity.GroupEntity;
import org.apache.eventmesh.dashboard.console.service.group.GroupService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventmeshConsoleApplication.class)
public class TestOprLog {

    @Autowired
    private GroupService groupService;

    @Test
    public void test_groupService_OprLog() {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(1L);
        System.out.println(groupService.getGroupByClusterId(groupEntity));

    }

}
