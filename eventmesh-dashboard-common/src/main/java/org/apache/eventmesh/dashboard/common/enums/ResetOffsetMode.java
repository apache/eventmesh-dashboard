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

public enum ResetOffsetMode {

    /**
     * 支持维度如下：
     * <p>
     * event mesh cluster 。
     *  RockerMQ 查出 所有的 存储集群，然后存储存储集群里面,所有子集群，以及子集群里面所有的 broker ，对每个broker 进行一次请求奥做。
     *  Kkakfa 查出 所有的 存储集群，然后存储存储集群里面。 只需要对 cluster 进行一次请求操作
     * <p>
     * storage cluster
     * <p>
     * storage definition cluster
     * <p>
     * storage runtime cluster
     *
     *
     */
    CONSUME_FROM_LAST_OFFSET,

    CONSUME_FROM_FIRST_OFFSET,

    CONSUME_FROM_TIMESTAMP,

    /**
     * 只有此模式支持 Queue 维度
     * <p>
     * 只有这个模式，传递Queue id
     */
    CONSUME_FROM_DESIGNATED_OFFSET

}
