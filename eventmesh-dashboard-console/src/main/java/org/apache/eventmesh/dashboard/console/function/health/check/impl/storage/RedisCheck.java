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

package org.apache.eventmesh.dashboard.console.function.health.check.impl.storage;

import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_STORAGE;

import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.time.Duration;
import java.util.Objects;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisURI.Builder;
import io.lettuce.core.api.async.RedisAsyncCommands;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@HealthCheckType(type = HEALTH_CHECK_TYPE_STORAGE, subType = "redis")
public class RedisCheck extends AbstractHealthCheckService {

    private RedisClient redisClient;

    public RedisCheck(HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    @Override
    public void doCheck(HealthCheckCallback callback) {
        try {
            RedisAsyncCommands<String, String> commands = redisClient.connect().async();
            commands.ping().thenAccept(result -> {
                callback.onSuccess();
            }).exceptionally(e -> {
                if (e instanceof Exception) {
                    callback.onFail((Exception) e);
                } else {
                    callback.onFail(new RuntimeException("RedisCheck failed."));
                }
                return null;
            });
        } catch (Exception e) {
            log.error(e.toString());
            callback.onFail(e);
        }
    }

    @Override
    public void init() {
        String redisUrl;
        if (Objects.nonNull(getConfig().getConnectUrl()) && !getConfig().getConnectUrl().isEmpty()) {
            redisUrl = getConfig().getConnectUrl();
        } else {
            Builder builder = RedisURI.Builder.redis(getConfig().getHost(), getConfig().getPort());
            if (Objects.nonNull(getConfig().getUsername()) && Objects.nonNull(getConfig().getPassword())) {
                builder.withAuthentication(getConfig().getUsername(), getConfig().getPassword());
            }
            if (Objects.nonNull(getConfig().getRequestTimeoutMillis())) {
                builder.withTimeout(Duration.ofMillis(getConfig().getRequestTimeoutMillis()));
            }
            if (Objects.nonNull(getConfig().getDatabase())) {
                builder.withDatabase(Integer.parseInt(getConfig().getDatabase()));
            }
            redisUrl = builder.build().toString();
        }
        redisClient = RedisClient.create(redisUrl);
    }

    @Override
    public void destroy() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
