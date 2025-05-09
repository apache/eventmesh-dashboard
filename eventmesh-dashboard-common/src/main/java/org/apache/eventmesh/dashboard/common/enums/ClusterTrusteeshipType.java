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

import java.util.List;

import lombok.Getter;

/**
 *
 */
@Getter
public enum ClusterTrusteeshipType {


    NOT("", List.of(ClusterType.CLUSTER, ClusterType.META, ClusterType.RUNTIME, ClusterType.META_AND_RUNTIME)),

    /**
     * 全托管 存储不好做全托管 RocketMQ， kafhka，都适合
     **/
    SELF("自维护", List.of(ClusterType.CLUSTER, ClusterType.META, ClusterType.RUNTIME, ClusterType.META_AND_RUNTIME)),

    /**
     *
     */
    TRUSTEESHIP("托管", List.of(ClusterType.CLUSTER, ClusterType.META, ClusterType.RUNTIME, ClusterType.META_AND_RUNTIME)),

    /**
     * 托管，从 meta 获得 runtime 提供了 meta地址，从 meta 获得 runtime 主要用于已经存在的集群 支持： eventmesh cluster RocketMQ cluster
     **/
    TRUSTEESHIP_FIND("托管且从 meta 获得 runtime，同时同步配置", List.of(ClusterType.META, ClusterType.META_AND_RUNTIME)),

    /**
     * 继承 TRUSTEESHIP_FIND 能力 同时以 集群为主
     **/
    TRUSTEESHIP_FIND_REVERSE("以集群为主", List.of(ClusterType.CLUSTER, ClusterType.META, ClusterType.RUNTIME, ClusterType.META_AND_RUNTIME)),

    // 不托管
    NO_TRUSTEESHIP("不托管", List.of(ClusterType.CLUSTER, ClusterType.META, ClusterType.RUNTIME, ClusterType.META_AND_RUNTIME));

    private List<ClusterType> clusterTypeList;

    private final String type;

    private String name;

    private FirstToWhom firstToWhom;

    public enum FirstToWhom {

        NOT,

        DASHBOARD,

        RUNTIME,

        /**
         * 等待开始
         */
        WAIT_START,

        /**
         * 同步完成
         */
        COMPLETE,

        /**
         * 同步中
         */
        UNDER_WAY,

        FAIL,

    }

    /**
     * 注册 meta， 表示需要从 meta 读取 runtime。 如果  FirstToWhom.DASHBOARD ， 那么读取的 runtime 都标记为 DASHBOARD 如果  不托管 ， 那么读取的 runtime 标记如  托管状态
     * <p>
     * 读取 runtime 的会后， 如何是  DASHBOARD ， 使用 database 同步 如果是  RUNTIME   ， 使用 runtime 同步 执行完之后，判断 同步状态， 如果是 以 runtime 为主，使用 runtime 同步， 以 runtime 的不允许修改
     * 如果是 以  database 为主，使用 database 同步 分为第一次同步，与永久同步
     */
    @Deprecated
    public enum TrusteeshipArrangeType {

        /**
         * 同时注册了 meta 与 runtime
         */
        META_AND_RUNTIME,

        /**
         * 只注册 meta
         */
        META,

        /**
         * 只注册 runtime
         */
        RUNTIME,

        /**
         * 自维护
         */
        SELF,


    }

    ClusterTrusteeshipType(String type, List<ClusterType> clusterTypeList) {
        this.type = type;
    }


    public boolean isSelf() {
        return this == ClusterTrusteeshipType.SELF || this == ClusterTrusteeshipType.TRUSTEESHIP;
    }

    public boolean isReverse() {
        return this == ClusterTrusteeshipType.TRUSTEESHIP_FIND || this == ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE;
    }
}
