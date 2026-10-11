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


package org.apache.eventmesh.dashboard.common.model.metadata;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** ACL 1.0 complete account configuration, used only for RPC and never persisted by console mappers. */
@Data
@EqualsAndHashCode(callSuper = true)
public class RocketMQAclAccountMetadata extends AclMetadata {

    private String accessKey;

    @ToString.Exclude
    private String secretKey;

    private Boolean admin;

    /** Empty string explicitly disables the account whitelist. This is not ACL 2.0 sourceIps. */
    private String whiteRemoteAddress;

    private String defaultTopicPerm;

    private String defaultGroupPerm;

    /** Complete lists of name=DENY/PUB/SUB/PUB|SUB rules; empty lists remove explicit rules. */
    private List<String> topicPerms;

    private List<String> groupPerms;

    @Override
    public String nodeUnique() {
        return "RocketMQAclV1:" + this.accessKey;
    }
}
