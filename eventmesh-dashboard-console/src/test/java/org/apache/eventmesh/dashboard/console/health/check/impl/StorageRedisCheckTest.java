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

package org.apache.eventmesh.dashboard.console.health.check.impl;

import org.apache.eventmesh.dashboard.console.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.health.check.config.HealthCheckObjectConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageRedisCheckTest {

    private StorageRedisCheck storageRedisCheck;

    @BeforeEach
    public void init() {
        HealthCheckObjectConfig config = new HealthCheckObjectConfig();
        config.setInstanceId(1L);
        config.setHealthCheckResourceType("storage");
        config.setHealthCheckResourceSubType("redis");
        config.setSimpleClassName("StorageRedisCheck");
        config.setClusterId(1L);
        config.setConnectUrl("redis://localhost:6379");
        storageRedisCheck = new StorageRedisCheck(config);
    }

    @Test
    public void testDoCheck() {
        storageRedisCheck.doCheck(new HealthCheckCallback() {
            @Override
            public void onSuccess() {
                System.out.println("success");
            }

            @Override
            public void onFail(Exception e) {
                System.out.println("fail");
            }
        });
    }
}