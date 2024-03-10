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

package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.console.constant.health.HealthConstant;
import org.apache.eventmesh.dashboard.console.enums.health.HealthCheckStatus;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class CheckResultCache {

    private final HashMap<String, HashMap<Long, CheckResult>> cacheMap = new HashMap<>();

    public void update(String type, Long typeId, HealthCheckStatus status, String resultDesc, Long latency) {
        HashMap<Long, CheckResult> subMap = cacheMap.get(type);
        if (Objects.isNull(subMap)) {
            subMap = new HashMap<>();
            cacheMap.put(type, subMap);
        }
        CheckResult oldResult = subMap.get(typeId);
        String description = resultDesc;
        if (oldResult.getResultDesc() != null && !oldResult.getResultDesc().isEmpty()) {
            description = oldResult.getResultDesc() + HealthConstant.NEW_LINE_ENDING + resultDesc;
        }
        description += " Latency: " + latency.toString() + "ms";
        CheckResult result = new CheckResult(status, description, LocalDateTime.now(),
            latency, oldResult.getConfig());
        subMap.put(typeId, result);
    }

    public void update(String type, Long typeId, HealthCheckStatus status, String resultDesc, Long latency, HealthCheckObjectConfig config) {
        HashMap<Long, CheckResult> subMap = cacheMap.get(type);
        if (Objects.isNull(subMap)) {
            subMap = new HashMap<>();
            cacheMap.put(type, subMap);
        }
        subMap.put(typeId, new CheckResult(status, resultDesc, LocalDateTime.now(), latency, config));
    }

    public Map<String, HashMap<Long, CheckResult>> getCacheMap() {
        return Collections.unmodifiableMap(cacheMap);
    }

    @Getter
    @AllArgsConstructor
    public class CheckResult {

        /**
         * the status of a health check.
         *
         * @see HealthCheckStatus
         */
        @Setter
        private HealthCheckStatus status;
        /**
         * if not passed, this field is used to store some description.
         */
        private String resultDesc;
        /**
         * the time this record is inserted into memory map.
         */
        private LocalDateTime createTime;
        /**
         * latency of a health check, for example ping latency.
         */
        private Long latencyMilliSeconds;

        private HealthCheckObjectConfig config;
    }
}
