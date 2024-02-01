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

package org.apache.eventmesh.dashboard.console.service.group;

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/EventMeshDashboardApplication.java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
========
import org.apache.eventmesh.dashboard.console.entity.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;
>>>>>>>> 3391325 (first improve):eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/group/GroupService.java


<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/EventMeshDashboardApplication.java
@Slf4j
@SpringBootApplication
@EnableTransactionManagement
public class EventMeshDashboardApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(EventMeshDashboardApplication.class, args);
            log.info("{} Boot Successful!", EventMeshDashboardApplication.class.getSimpleName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
========
import java.util.List;

/**
 * operate Group Service
 */

public interface GroupService {

    List<GroupEntity> selectAll();

    void batchInsert(List<GroupEntity> groupEntities);

    List<GroupEntity> getGroupByClusterId(GroupEntity groupEntity);

    GroupEntity addGroup(GroupEntity groupEntity);

    void updateGroup(GroupEntity groupEntity);

    Integer deleteGroup(GroupEntity groupEntity);

    GroupEntity selectGroup(GroupEntity groupEntity);

    Integer insertMemberToGroup_plus(GroupMemberEntity groupMemberEntity);

    Integer deleteMemberFromGroup_plus(GroupMemberEntity groupMemberEntity);
>>>>>>>> 3391325 (first improve):eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/group/GroupService.java
}
