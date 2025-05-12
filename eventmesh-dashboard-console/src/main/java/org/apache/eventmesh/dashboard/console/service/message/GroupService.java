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


package org.apache.eventmesh.dashboard.console.service.message;

import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.SubscriptionEntity;

import java.util.List;

/**
 * operate Group Service
 */

public interface GroupService {

    @Deprecated
    List<GroupEntity> selectAll();

    void batchInsert(List<GroupEntity> groupEntities);

    List<GroupEntity> getGroupByClusterId(GroupEntity groupEntity);

    void addGroup(GroupEntity groupEntity);

    @Deprecated
    void updateGroup(GroupEntity groupEntity);

    Integer deleteGroup(GroupEntity groupEntity);

    @Deprecated
    GroupEntity selectGroup(GroupEntity groupEntity);

    @Deprecated
    Integer insertMemberToGroup(SubscriptionEntity subscriptionEntity);

    @Deprecated
    Integer deleteMemberFromGroup(SubscriptionEntity subscriptionEntity);
}
