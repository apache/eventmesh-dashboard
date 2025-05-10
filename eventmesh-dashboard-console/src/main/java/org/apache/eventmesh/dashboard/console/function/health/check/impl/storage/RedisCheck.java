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

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@HealthCheckType(clusterType = {ClusterType.STORAGE_REDIS_BROKER}, healthType = HealthCheckTypeEnum.PING)
public class RedisCheck extends AbstractHealthCheckService<StatefulRedisConnection<String, String>> {

    @Override
    public void doCheck(HealthCheckCallback callback) throws Exception {
        RedisAsyncCommands<String, String> commands = this.getClient().async();
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
    }

}
