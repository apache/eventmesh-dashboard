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

package org.apache.eventmesh.dashboard.common.enums;

import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_KAFKA;
import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_PULSAR;
import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_RABBITMQ;
import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_REDIS;
import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_ROCKETMQ;
import static org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant.STORE_TYPE_STANDALONE;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum StoreType {

    STANDALONE(0, STORE_TYPE_STANDALONE),
    ROCKETMQ(1, STORE_TYPE_ROCKETMQ),
    KAFKA(2, STORE_TYPE_KAFKA),
    PULSAR(3, STORE_TYPE_PULSAR),
    RABBITMQ(4, STORE_TYPE_RABBITMQ),
    REDIS(5, STORE_TYPE_REDIS);

    @Getter
    private final Integer number;
    @Getter
    private final String name;

    public static StoreType fromNumber(Integer number) {
        for (StoreType storeType : StoreType.values()) {
            if (storeType.getNumber().equals(number)) {
                return storeType;
            }
        }
        return null;
    }

    public static StoreType fromName(String name) {
        for (StoreType storeType : StoreType.values()) {
            if (storeType.getName().equalsIgnoreCase(name)) {
                return storeType;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
