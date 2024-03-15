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

package org.apache.eventmesh.dashboard.console.function.SDK;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.eventmesh.dashboard.console.function.SDK.config.CreateRedisConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class SDKManagerTest {

    private final CreateRedisConfig createRedisConfig = new CreateRedisConfig();
    private String redisKey;

    @BeforeEach
    void setUp() {
        try {
            createRedisConfig.setRedisUrl("redis://localhost:6379");
            redisKey = SDKManager.getInstance().createClient(SDKTypeEnum.STORAGE_REDIS, createRedisConfig).getKey();
        } catch (Exception e) {
            log.warn("SDK manager test init failed, possible reason: redis-server is offline. {}", this.getClass().getSimpleName(), e);
        }
    }

    @Test
    public void testGetClient() {
        try {
            Object redisClient = SDKManager.getInstance().getClient(SDKTypeEnum.STORAGE_REDIS, redisKey);
            assertNotNull(redisClient);
        } catch (Exception e) {
            log.warn("SDK manager test failed, possible reason: redis-server is offline. {}", this.getClass().getSimpleName(), e);
        }
    }
}