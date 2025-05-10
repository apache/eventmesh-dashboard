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


package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRedisConfig;

import java.time.Duration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;


@SDKMetadata(clusterType = {
    ClusterType.STORAGE_REDIS_BROKER}, remotingType = RemotingType.REDIS, sdkTypeEnum = SDKTypeEnum.ALL, config = CreateRedisConfig.class)
public class RedisSDKOperation extends AbstractSDKOperation<StatefulRedisConnection<String, String>, CreateRedisConfig> {

    @Override
    public StatefulRedisConnection<String, String> createClient(CreateRedisConfig clientConfig) {
        RedisURI redisURI = RedisURI.builder()
            .withHost(clientConfig.getNetAddress().getAddress())
            .withPort(clientConfig.getNetAddress().getPort())
            .withPassword(clientConfig.getPassword() == null ? "" : clientConfig.getPassword())
            .withTimeout(Duration.ofSeconds(clientConfig.getTimeOut()))
            .build();
        RedisClient redisClient = RedisClient.create(redisURI);
        return redisClient.connect();
    }

    @Override
    public void close(StatefulRedisConnection<String, String> client) {
        client.close();
    }
}
