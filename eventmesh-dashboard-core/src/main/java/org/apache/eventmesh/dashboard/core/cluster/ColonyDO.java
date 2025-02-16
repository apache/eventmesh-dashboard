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

package org.apache.eventmesh.dashboard.core.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * eventmesh ClusterDO meta    ClusterDO runtime ClusterDO storage ClusterDO meta（注册中心，zk，） ClusterDO runtime（broker）   ClusterDO
 * 一个集群 eventmesh 集群有
 * 1.有多个 runtime 集群
 * 2.有多个 存储 集群
 * 3.有多个 meta 集群
 *
 * 一个 RocketMQ 集群
 * 1. 有多个 存储群集（这里是 runtime 集群，是内部实例，不是外部实例）
 * 2. 有一个注册中心集群（这里是 meta ）
 */
@Data
public class ColonyDO {

    private Long superiorId;

    private ClusterDO clusterDO;

    // 双活集群 所以是可以是一个list的
    // 可以默认一个集群
    private Map<Long, ColonyDO> runtimeColonyDOList = new ConcurrentHashMap<>();

    // 只有 eventmesh 集群有这个点，其他没有。
    private Map<Long, ColonyDO> storageColonyDOList = new ConcurrentHashMap<>();

    /**
     * A(nameserver cluster) a1 a2 a3 B(nameserver cluster) b1 b2 b3
     * <p>
     * rocketmq   a1,a2,a3,b1,b2,b3
     */
    private Map<Long, ColonyDO> metaColonyDOList = new ConcurrentHashMap<>();


    public Long getClusterId() {
        return Objects.nonNull(this.clusterDO.getClusterInfo().getClusterId()) ? this.clusterDO.getClusterInfo().getClusterId()
            : this.clusterDO.getClusterInfo().getId();
    }

}
