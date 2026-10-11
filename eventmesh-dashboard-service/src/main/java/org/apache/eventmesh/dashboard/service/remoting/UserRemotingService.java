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


package org.apache.eventmesh.dashboard.service.remoting;

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMethodMapper;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserResult;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleteUserResult;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleterUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserResult;

/**
 * Remoting Service to manage service users. For example, create a mysql user or get kafka users.
 */
public interface UserRemotingService {

    @RemotingServiceMethodMapper(RemotingActionType.ADD)
    CreateUserResult createInstanceUser(CreateUserRequest request) throws Exception;

    @RemotingServiceMethodMapper(RemotingActionType.UPDATE)
    default CreateUserResult updateInstanceUser(CreateUserRequest request) throws Exception {
        throw new UnsupportedOperationException("User update is not implemented for this middleware");
    }

    @RemotingServiceMethodMapper(RemotingActionType.DELETE)
    DeleteUserResult deleteInstanceUser(DeleterUserRequest request) throws Exception;

    @RemotingServiceMethodMapper(RemotingActionType.QUEUE_ALL)
    GetUserResult getInstanceUser(GetUserRequest request) throws Exception;
}
