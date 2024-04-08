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

package org.apache.eventmesh.dashboard.console.function.health.check.config;

import java.util.Properties;

import lombok.Data;

@Data
public class HealthCheckObjectConfig {

    private Long instanceId;

    private String healthCheckResourceType;

    private String healthCheckResourceSubType;

    private String simpleClassName;

    private Class<?> checkClass;

    private Properties eventmeshProperties;

    private Long clusterId;

    //Prioritize passing in this field for a url.
    //redis, nacos
    private String connectUrl;

    //redis
    private String host;

    private Integer port;

    private String username;

    private String password;

    //mysql, redis
    private String database;

    private Long requestTimeoutMillis = 100000L;

    private RocketmqConfig rocketmqConfig = new RocketmqConfig();

    @Data
    public class RocketmqConfig {

        private String nameServerUrl;
        private String brokerUrl;
        private String endPoint;
    }
}