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

package org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import javax.validation.constraints.Null;

import lombok.Data;

@Data
public class SimpleCreateClusterDataDTO {

    @Null
    private ClusterType clusterType;

    @Null
    private String name;

    @Null
    private ClusterTrusteeshipType clusterTrusteeshipType;

    @Null
    private FirstToWhom firstToWhom;

    @Null
    private String description;

    @Null
    private String address;

    private String version;

    private String jmxPort;

    private Long mainClusterId;

    private ClusterTrusteeshipType discoverRuntimeTrusteeshipType;

    private FirstToWhom discoverRuntimeFirstToWhom;

}
