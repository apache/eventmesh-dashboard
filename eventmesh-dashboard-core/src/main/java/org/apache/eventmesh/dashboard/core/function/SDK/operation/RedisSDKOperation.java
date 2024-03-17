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

import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRedisConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.util.AbstractMap.SimpleEntry;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisSDKOperation extends AbstractSDKOperation<StatefulRedisConnection<String, String>> {

    @Override
    public SimpleEntry<String, StatefulRedisConnection<String, String>> createClient(CreateSDKConfig clientConfig) {
        String redisUrl = ((CreateRedisConfig) clientConfig).getRedisUrl();
        RedisClient redisClient = RedisClient.create(redisUrl);
        return new SimpleEntry<>(clientConfig.getUniqueKey(), redisClient.connect());
    }

    @Override
    public void close(Object client) {
        castClient(client).close();
    }
}
