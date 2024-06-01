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

package org.apache.eventmesh.dashboard.core.function.SDK;

public enum SDKTypeEnum {

    RUNTIME,

    STORAGE_ROCKETMQ_REMOTING,

    STORAGE_ROCKETMQ_ADMIN,

    STORAGE_ROCKETMQ_PRODUCER,

    STORAGE_ROCKETMQ_CONSUMER,

    STORAGE_REDIS,

    META_NACOS,
    META_NACOS_CONFIG,

    META_NACOS_NAMING,

    META_ETCD,

    RUNTIME_EVENTMESH_CLIENT,

    RUNTIME_TCP_CLOUDEVENT_CLIENT,
    RUNTIME_TCP_EVENTMESH_CLIENT,
    RUNTIME_TCP_OPENMESSAGE_CLIENT,

    RUNTIME_HTTP_PRODUCER,
    RUNTIME_HTTP_CONSUMER,

    RUNTIME_GRPC_PRODUCER,
    RUNTIME_GRPC_CONSUMER,
}
