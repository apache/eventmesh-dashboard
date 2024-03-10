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

package org.apache.eventmesh.dashboard.console.enums.health;

import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_CLUSTER;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_RUNTIME;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_STORAGE;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_TOPIC;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_UNKNOWN;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum HealthCheckType {
    UNKNOWN(0, HEALTH_CHECK_TYPE_UNKNOWN),

    CLUSTER(1, HEALTH_CHECK_TYPE_CLUSTER),

    RUNTIME(2, HEALTH_CHECK_TYPE_RUNTIME),

    TOPIC(3, HEALTH_CHECK_TYPE_TOPIC),

    STORAGE(4, HEALTH_CHECK_TYPE_STORAGE);

    @Getter
    private final Integer number;
    @Getter
    private final String name;

    public static Integer toNumber(String name) {
        for (HealthCheckType healthCheckTypeEnum : HealthCheckType.values()) {
            if (healthCheckTypeEnum.name.equals(name)) {
                return healthCheckTypeEnum.number;
            }
        }
        return UNKNOWN.number;
    }

    public static String toName(Integer number) {
        for (HealthCheckType healthCheckTypeEnum : HealthCheckType.values()) {
            if (healthCheckTypeEnum.number.equals(number)) {
                return healthCheckTypeEnum.name;
            }
        }
        return UNKNOWN.name;
    }
}
