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

package org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4;

import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
class Rocketmq4TopicCheckTest {

    private Rocketmq4TopicCheck rocketmqCheck;

    @BeforeEach
    public void init() throws InterruptedException {
        try {
            HealthCheckObjectConfig config = new HealthCheckObjectConfig();
            config.getRocketmqConfig().setBrokerUrl("127.0.0.1:10911");
            config.getRocketmqConfig().setNameServerUrl("127.0.0.1:9876");
            config.setRequestTimeoutMillis(1000L);
            rocketmqCheck = new Rocketmq4TopicCheck(config);
        } catch (Exception e) {
            log.error("Rocketmq4TopicCheck failed.", e);
        }
    }

    @Test
    public void testDoCheck() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(2);
            rocketmqCheck.doCheck(new HealthCheckCallback() {
                @Override
                public void onSuccess() {
                    latch.countDown();
                    log.info("{} success", this.getClass().getSimpleName());
                }

                @Override
                public void onFail(Exception e) {
                    latch.countDown();
                    log.error("{}, failed for reason {}", this.getClass().getSimpleName(), e);
                }
            });
            rocketmqCheck.doCheck(new HealthCheckCallback() {
                @Override
                public void onSuccess() {
                    latch.countDown();
                    log.info("{} success", this.getClass().getSimpleName());
                }

                @Override
                public void onFail(Exception e) {
                    latch.countDown();
                    log.error("{}, failed for reason {}", this.getClass().getSimpleName(), e);
                }
            });
            latch.await();
        } catch (Exception e) {
            log.error("Rocketmq4TopicCheck failed.", e);
        }
    }
}