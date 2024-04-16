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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRedisConfig;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RedisSDKCreateOperationTest {

    private final RedisSDKOperation redisClientCreateOperation = new RedisSDKOperation();

    @Test
    void testCreateClient() {
        CreateRedisConfig createClientConfig = CreateRedisConfig.builder()
            .redisUrl("localhost:6379")
            .password("")
            .timeOut(5)
            .build();
        SimpleEntry<String, StatefulRedisConnection<String, String>> simpleEntry = null;
        try {
            simpleEntry = redisClientCreateOperation.createClient(createClientConfig);
            assertEquals("localhost:6379", simpleEntry.getKey());
            String response = simpleEntry.getValue().sync().ping();
            log.info("response:{}", response);
            simpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create redis client failed", e);
            if (simpleEntry != null) {
                simpleEntry.getValue().close();
            }
        }
    }
}