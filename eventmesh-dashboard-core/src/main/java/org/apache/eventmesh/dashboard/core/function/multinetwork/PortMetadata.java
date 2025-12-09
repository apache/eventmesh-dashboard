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

package org.apache.eventmesh.dashboard.core.function.multinetwork;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import lombok.Data;

/**
 *  申请端口必须连续，这点需要 组件支持端口的配置
 */
@Data
public class PortMetadata {

    private ClusterType clusterType;

    private String configName;

    private String protocol = "TCP";

    private String effect = "client";

    private Integer port;

    private boolean required = true;

    private boolean virtual = false;

    private boolean nullPort  = false;

    private String explanation;

}
