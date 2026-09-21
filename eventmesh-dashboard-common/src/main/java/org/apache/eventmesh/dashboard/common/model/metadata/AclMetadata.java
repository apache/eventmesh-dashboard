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

import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AclMetadata extends BaseRuntimeIdBase {

    private Long clusterId;

    private String principal;

    private Integer operation;

    private String permissionType;

    private String host;

    private String resourceType;

    private String resourceName;

    private Integer patternType;


    /** RocketMQ ACL 2.0 policy entry fields; not persisted by the existing console schema. */
    private String policyType;

    private List<String> actions;

    /** Required on writes: an explicit empty list means no source-IP restriction. */
    private List<String> sourceIps;

    @Override
    public String nodeUnique() {
        if (this.actions == null && this.policyType == null) {
            return this.principal;
        }
        // One entry per subject, policy and resource, regardless of its current permissions.
        return part(this.principal) + part(this.policyType == null ? "Custom" : this.policyType)
            + part(this.resourceType) + part(this.resourceName);
    }

    private String part(String value) {
        return value == null ? "-1:" : value.length() + ":" + value;
    }
}
