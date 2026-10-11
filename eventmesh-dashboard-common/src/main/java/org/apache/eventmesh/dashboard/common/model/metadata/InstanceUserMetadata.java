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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** User account metadata. Passwords are write-only in RocketMQ query results. */
@Data
@EqualsAndHashCode(callSuper = true)
public class InstanceUserMetadata extends BaseRuntimeIdBase {

    private String userName;

    @ToString.Exclude
    private String password;

    /** RocketMQ: Normal or Super; required explicitly for creation. */
    private String userType;

    /** RocketMQ: enable or disable; required explicitly for creation. */
    private String userStatus;

    @Override
    public String nodeUnique() {
        return this.userName;
    }
}
