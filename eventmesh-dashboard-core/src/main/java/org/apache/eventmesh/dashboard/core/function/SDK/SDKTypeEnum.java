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

/**
 * TODO
 *    META 与 RUNTIME 有相同操作，如何确定.
 *    同时多协议，如何处理。
 *    TPC的优先级最高，那如何确定端口。
 * <p>
 * TODO 关于订阅与发布
 *    消费组 与 生产组 的 group name 是否随机
 *    RocketMQ 的 namespace 如何处理
 *    PRODUCER,CONSUMER,操作完之后，是否要删除 生产与消费相关信息
 */
public enum SDKTypeEnum {

    ALL,

    PING,

    ADMIN,

    PRODUCER,

    CONSUMER,

    ;
}
