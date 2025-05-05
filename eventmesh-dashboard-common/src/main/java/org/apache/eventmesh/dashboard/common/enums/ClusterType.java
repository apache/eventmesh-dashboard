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

    DEFINITION(0),

    DEFAULT(1),


    EVENTMESH(20),

    CLUSTER(1),

    META(2),

    RUNTIME(3),

    META_AND_RUNTIME(4),

    BROKER(4),

    COPY_BROKER(5),

    ZK_BROKER(6),

    RFT_BROKER(6),

    STORAGE(21),

    RUNTIME_CLUSTER(21),

    META_CLUSTER(22),

    STORAGE_CLUSTER(22),

    DEFAULT_TYPE_NAME(1),


    CLUSTER_TYPE_META(1),

    CLUSTER_TYPE_RUNTIME(1),

    CLUSTER_TYPE_STORAGE(1),


    NODE_BY_COPY_IN_TYPE(2),

    NODE_BY_COPY_IN_TYPE_NOT_HAVE(2),

    NODE_BY_COPY_IN_TYPE_MAIN(2),

    NODE_BY_COPY_IN_TYPE_SLAVE(3),


    RUNTIME_EVENT_RUNTIME(ClusterType.META.code + 1),

    RUNTIME_ROCKETMQ_BROKER(ClusterType.META.code + 31),

    META_TYPE_ETCD(ClusterType.META.code + 1),

    META_TYPE_NACOS(ClusterType.META.code + 2),

    META_TYPE_ZK(ClusterType.META.code + 3),

    META_TYPE_ROCKETMQ_NAMESERVER(ClusterType.META.code + 31),

    KUBERNETES_RUNTIME(DEFAULT, DEFAULT, CLUSTER, RUNTIME, RemotingType.KUBERNETES),

    EVENTMESH_CLUSTER(EVENTMESH, EVENTMESH, CLUSTER, DEFINITION, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_RUNTIME(EVENTMESH, EVENTMESH, RUNTIME, DEFAULT, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_META_ETCD(EVENTMESH, EVENTMESH, META, META_TYPE_ETCD, RemotingType.EVENT_MESH_ETCD),

    EVENTMESH_META_NACOS(EVENTMESH, EVENTMESH, META, META_TYPE_NACOS, RemotingType.EVENT_MESH_NACOS),

    STORAGE_ROCKETMQ(ClusterType.STORAGE.code + 1),

    STORAGE_ROCKETMQ_CLUSTER(STORAGE, STORAGE_ROCKETMQ, CLUSTER, DEFINITION, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_NAMESERVER(STORAGE, STORAGE_ROCKETMQ, META, DEFAULT, RemotingType.ROCKETMQ_NAMESERVER),

    STORAGE_ROCKETMQ_BROKER(STORAGE, STORAGE_ROCKETMQ, RUNTIME, DEFINITION, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE(STORAGE, STORAGE_ROCKETMQ_BROKER, RUNTIME, DEFAULT, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_BROKER_RAFT(STORAGE, STORAGE_ROCKETMQ_BROKER, RUNTIME, DEFAULT, RemotingType.ROCKETMQ),


    STORAGE_KAFKA(STORAGE_ROCKETMQ.code + 1),

    STORAGE_KAFKA_CLUSTER(STORAGE, STORAGE_KAFKA, CLUSTER, DEFINITION, RemotingType.KAFKA),

    STORAGE_KAFKA_ZK(STORAGE, STORAGE_KAFKA, META, META_TYPE_ZK, RemotingType.ZK),

    STORAGE_KAFKA_RAFT(STORAGE, STORAGE_KAFKA, META_AND_RUNTIME, STORAGE_KAFKA, RemotingType.KAFKA),

    STORAGE_KAFKA_BROKER(STORAGE, STORAGE_KAFKA, RUNTIME, DEFAULT, RemotingType.KAFKA),


    STORAGE_REDIS(STORAGE_KAFKA.code + 1),

    STORAGE_REDIS_CLUSTER(STORAGE, STORAGE_REDIS, CLUSTER, DEFINITION, RemotingType.REDIS),

    STORAGE_REDIS_BROKER(STORAGE, STORAGE_REDIS, RUNTIME, DEFAULT, RemotingType.REDIS),

    STORAGE_JVM(STORAGE_REDIS.code + 1),

    STORAGE_JVM_CLUSTER(STORAGE, STORAGE_JVM, CLUSTER, DEFINITION, RemotingType.JVM),

    STORAGE_JVM_BROKER(STORAGE, STORAGE_JVM, RUNTIME, DEFAULT, RemotingType.JVM),

    STORAGE_JVM_CAP(STORAGE_REDIS.code + 1),

    STORAGE_JVM_CAP_CLUSTER(STORAGE, STORAGE_JVM_CAP, CLUSTER, DEFINITION, RemotingType.JVM),

    STORAGE_JVM_CAP_BROKER(STORAGE, STORAGE_JVM_CAP, RUNTIME, DEFAULT, RemotingType.JVM),
    ;

    public static final List<ClusterType> STORAGE_TYPES = getStorage();
    /**
     * 集群在 eventmesh 集群内的 节点（集群）类型。meta集群，存储集群，runtime集群
     */
    @Getter
    private ClusterType eventmeshNodeType;
    /**
     * 具体类型集群
     */
    @Getter
    private ClusterType assemblyName;

    /**
     * 这个节点在 具体集群内是什么节点
     */
    @Getter
    private ClusterType assemblyNodeType;

    /**
     * 厂商是什么类型。比如注册中心。注册中心有 etc，nacos
     */
    @Getter
    private ClusterType assemblyBusiness;

    /**
     * 远程协议类型
     */
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

    /**
     * 半托管状态需要 如要从
     *
     * @return
     */
    public boolean isEventMethMeta() {
        return this.eventmeshNodeType.equals(EVENTMESH) && this.assemblyNodeType.equals(META);
    }


    public boolean isEventMethRuntime() {
        return this == EVENTMESH_RUNTIME;
    }

    public boolean isMeta() {
        return this.assemblyNodeType.equals(META);
    }

    public boolean isMetaAndRuntime() {
        return this.eventmeshNodeType.equals(META_AND_RUNTIME);
    }

    public boolean isRuntime() {
        return this.assemblyNodeType.equals(RUNTIME) || this.eventmeshNodeType.equals(META_AND_RUNTIME);
    }

    public boolean isStorage() {
        return this.eventmeshNodeType.equals(STORAGE);
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

    public boolean isHealthTopic() {
        return Objects.equals(this, EVENTMESH_RUNTIME) || this.isStorageRuntime();
    }

    public boolean isStorageRuntime() {
        return Objects.equals(this.eventmeshNodeType, STORAGE) && Objects.equals(this.assemblyNodeType, RUNTIME);
    }

    public boolean isMain() {
        return Objects.equals(this, NODE_BY_COPY_IN_TYPE_MAIN);
    }

    public boolean isSlave() {
        return Objects.equals(this, NODE_BY_COPY_IN_TYPE_SLAVE);
    }

    public boolean isDefinition() {
        return Objects.equals(this.assemblyBusiness, DEFINITION);
    }
}
