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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

public enum ClusterType {

    DEFAULT(1),

    EVENTMESH(20),

    STORAGE(21),

    DEFAULT_TYPE_NAME(1),

    CLUSTER(1),

    META(2),

    RUNTIME(3),


    RUNTIME_EVENT_RUNTIME(ClusterType.META.code + 1),

    RUNTIME_ROCKETMQ_BROKER(ClusterType.META.code + 31),

    META_TYPE_ETCD(ClusterType.META.code + 1),

    META_TYPE_NACOS(ClusterType.META.code + 2),

    META_TYPE_ROCKETMQ_NAMESERVER(ClusterType.META.code + 31),


    EVENTMESH_CLUSTER(EVENTMESH, EVENTMESH, CLUSTER, DEFAULT, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_RUNTIME(EVENTMESH, EVENTMESH, RUNTIME, DEFAULT, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_META_ETCD(EVENTMESH, EVENTMESH, META, META_TYPE_ETCD, RemotingType.EVENT_MESH_ETCD),

    EVENTMESH_META_NACOS(EVENTMESH, EVENTMESH, META, META_TYPE_NACOS, RemotingType.EVENT_MESH_NACOS),

    STORAGE_ROCKETMQ(ClusterType.STORAGE.code + 1),

    STORAGE_ROCKETMQ_CLUSTER(STORAGE, STORAGE_ROCKETMQ, CLUSTER, DEFAULT, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_NAMESERVER(STORAGE, STORAGE_ROCKETMQ, META, DEFAULT, RemotingType.ROCKETMQ_NAMESERVER),

    STORAGE_ROCKETMQ_BROKER(STORAGE, STORAGE_ROCKETMQ, RUNTIME, DEFAULT, RemotingType.ROCKETMQ);


    public static final List<ClusterType> STORAGE_TYPES = getStorage();
    @Getter
    private ClusterType eventmeshNodeType;
    @Getter
    private ClusterType assemblyName;
    @Getter
    private ClusterType assemblyNodeType;
    @Getter
    private ClusterType assemblyBusiness;
    @Getter
    private RemotingType remotingType;
    @Getter
    private int code;

    ClusterType(int code) {
        this.code = code;
    }


    ClusterType(ClusterType eventmeshNodeType, ClusterType assemblyName, ClusterType assemblyNodeType, ClusterType assemblyBusiness,
        RemotingType remotingType) {
        this.eventmeshNodeType = eventmeshNodeType;
        this.assemblyName = assemblyName;
        this.assemblyNodeType = assemblyNodeType;
        this.assemblyBusiness = assemblyBusiness;
        this.remotingType = remotingType;
    }

    private static List<ClusterType> getStorage() {
        List<ClusterType> list = new ArrayList<>();
        for (ClusterType clusterType : ClusterType.values()) {
            if (Objects.equals(clusterType.eventmeshNodeType, ClusterType.STORAGE) && Objects.equals(clusterType.assemblyNodeType,
                ClusterType.CLUSTER)) {
                list.add(clusterType);
            }
        }
        return list;
    }

    public boolean isMainCluster() {
        return Objects.equals(this, ClusterType.EVENTMESH_CLUSTER) || Objects.equals(this.assemblyNodeType, ClusterType.CLUSTER);
    }

    public boolean isFirstLayer() {
        return Objects.equals(this, ClusterType.EVENTMESH_META_NACOS) || Objects.equals(this, ClusterType.EVENTMESH_META_ETCD) || Objects.equals(this,
            ClusterType.EVENTMESH_RUNTIME) || Objects.equals(this.getAssemblyNodeType(), ClusterType.CLUSTER);
    }

    public boolean isSecondFloor() {
        return Objects.equals(eventmeshNodeType, ClusterType.STORAGE) ? (Objects.equals(assemblyNodeType, ClusterType.RUNTIME) || Objects.equals(
            assemblyNodeType, ClusterType.META)) : false;
    }

}
