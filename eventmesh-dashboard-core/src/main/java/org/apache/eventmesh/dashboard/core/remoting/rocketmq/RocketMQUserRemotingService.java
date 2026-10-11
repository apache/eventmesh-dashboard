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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.metadata.InstanceUserMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserResult;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleteUserResult;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleterUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserResult;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.service.remoting.UserRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.UserInfo;
import org.apache.rocketmq.remoting.protocol.header.CreateUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetUserRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ListUsersRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateUserRequestHeader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** ACL 2.0 user operations. ACL 1.0 accounts are managed by AclRemotingService. */
public class RocketMQUserRemotingService extends AbstractRocketMQRemotingService implements UserRemotingService {

    @Override
    public CreateUserResult createInstanceUser(CreateUserRequest request) throws Exception {
        this.requireV2();
        InstanceUserMetadata user = this.requireUser(request == null ? null : request.getMetaData());
        if (StringUtils.isBlank(user.getPassword()) || user.getUserType() == null || user.getUserStatus() == null) {
            throw new IllegalArgumentException("Creating a user requires password, userType and userStatus");
        }
        return this.writeUser(user, RequestCode.AUTH_CREATE_USER, new CreateUserRequestHeader(user.getUserName()));
    }

    @Override
    public CreateUserResult updateInstanceUser(CreateUserRequest request) throws Exception {
        this.requireV2();
        InstanceUserMetadata user = this.requireUser(request == null ? null : request.getMetaData());
        if (user.getPassword() == null && user.getUserType() == null && user.getUserStatus() == null) {
            throw new IllegalArgumentException("Updating a user requires at least one changed field");
        }
        return this.writeUser(user, RequestCode.AUTH_UPDATE_USER, new UpdateUserRequestHeader(user.getUserName()));
    }

    @Override
    public DeleteUserResult deleteInstanceUser(DeleterUserRequest request) throws Exception {
        this.requireV2();
        InstanceUserMetadata user = this.requireUser(request == null ? null : request.getMetaData());
        return this.buildResult(this.invokeUser(RequestCode.AUTH_DELETE_USER,
            new DeleteUserRequestHeader(user.getUserName()), null), new DeleteUserResult());
    }

    @Override
    public GetUserResult getInstanceUser(GetUserRequest request) throws Exception {
        this.requireV2();
        InstanceUserMetadata filter = request == null ? null : request.getMetaData();
        String username = filter == null ? null : this.requireUser(filter).getUserName();
        RemotingCommand response = username == null
            ? this.invokeUser(RequestCode.AUTH_LIST_USER, new ListUsersRequestHeader(null), null)
            : this.invokeUser(RequestCode.AUTH_GET_USER, new GetUserRequestHeader(username), null);
        GetUserResult result = this.buildResult(response, new GetUserResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        List<InstanceUserMetadata> users = new ArrayList<>();
        if (response.getBody() != null && response.getBody().length > 0) {
            List<UserInfo> infos = username == null ? RemotingSerializable.decodeList(response.getBody(), UserInfo.class)
                : java.util.Collections.singletonList(RemotingSerializable.decode(response.getBody(), UserInfo.class));
            if (infos == null) {
                throw new IllegalStateException("RocketMQ returned an invalid user list");
            }
            Set<String> names = new HashSet<>();
            for (UserInfo info : infos) {
                if (info == null || StringUtils.isBlank(info.getUsername()) || !names.add(info.getUsername())
                    || username != null && !username.equals(info.getUsername())) {
                    throw new IllegalStateException("RocketMQ returned an invalid or duplicate user");
                }
                InstanceUserMetadata user = new InstanceUserMetadata();
                user.setUserName(info.getUsername());
                user.setUserType(this.userType(info.getUserType()));
                user.setUserStatus(this.userStatus(info.getUserStatus()));
                // The Broker may return credentials. Never copy the password into query metadata.
                users.add(user);
            }
        }
        users.sort(Comparator.comparing(InstanceUserMetadata::getUserName));
        result.setData(users);
        return result;
    }

    private CreateUserResult writeUser(InstanceUserMetadata user, int code, CommandCustomHeader header) throws Exception {
        if (user.getPassword() != null && StringUtils.isBlank(user.getPassword())) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        UserInfo info = UserInfo.of(user.getUserName(), user.getPassword(),
            user.getUserType() == null ? null : this.userType(user.getUserType()),
            user.getUserStatus() == null ? null : this.userStatus(user.getUserStatus()));
        return this.buildResult(this.invokeUser(code, header, RemotingSerializable.encode(info)), new CreateUserResult());
    }

    private RemotingCommand invokeUser(int code, CommandCustomHeader header, byte[] body) throws Exception {
        RemotingCommand command = RemotingCommand.createRequestCommand(code, header);
        command.setBody(body);
        RemotingCommand response = this.invokeSync(command);
        if (response.getCode() == ResponseCode.REQUEST_CODE_NOT_SUPPORTED) {
            throw new UnsupportedOperationException("This Broker does not support ACL 2.0 user administration");
        }
        return response;
    }

    private InstanceUserMetadata requireUser(InstanceUserMetadata user) {
        if (user == null || StringUtils.isBlank(user.getUserName()) || !user.getUserName().equals(user.getUserName().trim())) {
            throw new IllegalArgumentException("A nonblank username without surrounding whitespace is required");
        }
        return user;
    }

    private String userType(String value) {
        if ("Normal".equalsIgnoreCase(value)) {
            return "Normal";
        }
        if ("Super".equalsIgnoreCase(value)) {
            return "Super";
        }
        throw new IllegalArgumentException("User type must be Normal or Super");
    }

    private String userStatus(String value) {
        if ("enable".equalsIgnoreCase(value)) {
            return "enable";
        }
        if ("disable".equalsIgnoreCase(value)) {
            return "disable";
        }
        throw new IllegalArgumentException("User status must be enable or disable");
    }

    private void requireV2() {
        if (this.getCreateSdkConfig() == null) {
            return;
        }
        if (!(this.getCreateSdkConfig() instanceof CreateRemotingConfig)) {
            throw new IllegalStateException("RocketMQ user administration requires CreateRemotingConfig");
        }
        CreateRemotingConfig config = (CreateRemotingConfig) this.getCreateSdkConfig();
        if (config.getAclVersion() == null) {
            throw new IllegalStateException("RocketMQ aclVersion is required");
        }
        if (config.getAclVersion() != CreateRemotingConfig.AclVersion.V2) {
            throw new UnsupportedOperationException("ACL 1.0 accounts must be managed through AclRemotingService");
        }
    }
}
