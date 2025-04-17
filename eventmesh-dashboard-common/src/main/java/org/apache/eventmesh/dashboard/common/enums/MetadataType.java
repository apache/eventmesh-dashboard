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


package org.apache.eventmesh.dashboard.common.enums;

import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;

import lombok.Getter;


@Getter
public enum MetadataType {

    CLUSTER(ClusterMetadata.class),

    CONFIG(ConfigMetadata.class),

    RUNTIME(RuntimeMetadata.class),

    TOPIC(TopicMetadata.class),

    GROUP(GroupMetadata.class),

    OFFSET(TopicMetadata.class),

    SUBSCRIBER(GroupMetadata.class),

    CLIENT(ClientMetadata.class, true),

    NET_CONNECT(ConnectionMetadata.class, true),

    USER(ConnectionMetadata.class),

    ACL(AclMetadata.class);

    private Class<?> metadataClass;

    private boolean readOnly = false;

    MetadataType(Class<?> metadataClass) {
        this.metadataClass = metadataClass;
    }

    MetadataType(Class<?> metadataClass, boolean readOnly) {
        this.metadataClass = metadataClass;
        this.readOnly = readOnly;
    }
}
