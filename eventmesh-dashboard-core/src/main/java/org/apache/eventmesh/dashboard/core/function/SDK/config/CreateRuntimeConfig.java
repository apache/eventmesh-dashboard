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

package org.apache.eventmesh.dashboard.core.function.SDK.config;

import org.apache.eventmesh.common.protocol.tcp.UserAgent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateRuntimeConfig implements CreateSDKConfig {

    // 127.0.0.1:10105;127.0.0.2:10105
    private String runtimeServerAddress;

    // protocol example:HTTP,TCP,GRPC
    private String protocol;
    // for tcp:cloudevents,eventmeshmessage,openmessage
    private String protocolName;

    // producer or consumer
    private String clientType;

    // topic
    private String topic;

    // config
    private String env;
    private String idc;
    private String ip;
    private String sys;
    private String pid;
    private String username;
    private String password;

    // producer
    private String producerGroup;

    // consumer
    private String consumerGroup;
    private String subUrl;

    // Agent
    private UserAgent userAgent;

    @Override
    public String getUniqueKey() {
        return runtimeServerAddress;
    }
}