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

package org.apache.eventmesh.dashboard.common.constant.health;

import org.apache.eventmesh.dashboard.common.constant.StoreTypeConstant;

public class HealthCheckTypeConstant {

    public static final String HEALTH_CHECK_TYPE_UNKNOWN = "unknown";

    public static final String HEALTH_CHECK_TYPE_CLUSTER = "cluster";

    public static final String HEALTH_CHECK_TYPE_RUNTIME = "runtime";
    public static final String HEALTH_CHECK_TYPE_STORAGE = "storage";
    public static final String HEALTH_CHECK_TYPE_META = "meta";
    public static final String HEALTH_CHECK_TYPE_TOPIC = "topic";

    public static final String HEALTH_CHECK_SUBTYPE_REDIS = StoreTypeConstant.STORE_TYPE_REDIS;
    public static final String HEALTH_CHECK_SUBTYPE_MYSQL = StoreTypeConstant.STORE_TYPE_MYSQL;
    public static final String HEALTH_CHECK_SUBTYPE_ROCKETMQ = StoreTypeConstant.STORE_TYPE_ROCKETMQ;

    public static final String HEALTH_CHECK_SUBTYPE_NACOS_CONFIG = "nacos-config";
    public static final String HEALTH_CHECK_SUBTYPE_NACOS_REGISTRY = "nacos-registry";
}
