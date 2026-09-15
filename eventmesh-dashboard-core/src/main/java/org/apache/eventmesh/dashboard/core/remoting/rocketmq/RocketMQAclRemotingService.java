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

import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

/** Framework entry point; protocol selection belongs to the registered target configuration. */
public class RocketMQAclRemotingService extends AbstractRocketMQRemotingService implements AclRemotingService {

    private final RocketMQAclProtocolHandler v1 = new RocketMQAclV1ProtocolHandler(this);

    private final RocketMQAclProtocolHandler v2 = new RocketMQAclV2ProtocolHandler(this);

    private RocketMQAclProtocolHandler protocol() {
        // Preserve ACL 2.0 behavior for existing client registrations without an explicit setting.
        if (this.getCreateSdkConfig() == null) {
            return this.v2;
        }
        if (!(this.getCreateSdkConfig() instanceof CreateRemotingConfig)) {
            throw new IllegalStateException("RocketMQ ACL requires CreateRemotingConfig");
        }
        CreateRemotingConfig config = (CreateRemotingConfig) this.getCreateSdkConfig();
        if (config.getAclVersion() == null) {
            throw new IllegalStateException("RocketMQ aclVersion must be V1 or V2");
        }
        switch (config.getAclVersion()) {
            case V1:
                return this.v1;
            case V2:
                return this.v2;
            default:
                throw new IllegalStateException("Unsupported RocketMQ ACL version");
        }
    }

    @Override
    public CreateAclResult createAcl(CreateAclRequest request) throws Exception {
        return this.protocol().createAcl(request);
    }

    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest request) throws Exception {
        return this.protocol().deleteAcl(request);
    }

    @Override
    public GetAclsResult getAllAcls(GetAcls2Request request) throws Exception {
        return this.protocol().getAllAcls(request);
    }
}
