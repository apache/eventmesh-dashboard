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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;

public enum ClusterType {

    /**
     * 只有逻辑集群才会使用 这个枚举。比如  EVENTMESH_CLUSTER，先定义 EVENTMESH_CLUSTER，然后 runtime 与 meta 集群关联上
     */
    DEFINITION(0),

    DEFAULT(1),


    EVENTMESH(20),

    EVENTMESH_JVM(21),

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


    PROMETHEUS_EXPORTER(1),

    NODE_BY_COPY_IN_TYPE(2),

    NODE_BY_COPY_IN_TYPE_NOT_HAVE(2),

    NODE_BY_COPY_IN_TYPE_MAIN(2),

    NODE_BY_COPY_IN_TYPE_SLAVE(3),


    RUNTIME_EVENT_RUNTIME(ClusterType.META.code + 1),

    RUNTIME_ROCKETMQ_BROKER(ClusterType.META.code + 31),

    META_TYPE_ETCD(ClusterType.META.code + 1),

    META_TYPE_NACOS(ClusterType.META.code + 2),

    META_TYPE_ZK(ClusterType.META.code + 3),

    META_TYPE_JVM(ClusterType.META.code + 4),

    META_TYPE_ROCKETMQ_NAMESERVER(ClusterType.META.code + 31),

    KUBERNETES_RUNTIME(DEFAULT, DEFAULT, CLUSTER, RUNTIME, RemotingType.KUBERNETES),

    EVENTMESH_CLUSTER(EVENTMESH, EVENTMESH, CLUSTER, DEFINITION, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_RUNTIME(EVENTMESH, EVENTMESH_CLUSTER, RUNTIME, DEFAULT, RemotingType.EVENT_MESH_RUNTIME),

    EVENTMESH_META_ETCD(EVENTMESH, EVENTMESH_CLUSTER, META, META_TYPE_ETCD, RemotingType.EVENT_MESH_ETCD),

    EVENTMESH_META_NACOS(EVENTMESH, EVENTMESH_CLUSTER, META, META_TYPE_NACOS, RemotingType.EVENT_MESH_NACOS),

    EVENTMESH_JVM_CLUSTER(EVENTMESH, EVENTMESH_JVM, CLUSTER, DEFINITION, RemotingType.JVM),

    EVENTMESH_JVM_RUNTIME(EVENTMESH, EVENTMESH_JVM_CLUSTER, RUNTIME, DEFAULT, RemotingType.JVM),

    EVENTMESH_JVM_META(EVENTMESH, EVENTMESH_JVM_CLUSTER, META, META_TYPE_JVM, RemotingType.JVM),


    STORAGE_ROCKETMQ(ClusterType.STORAGE.code + 1),

    STORAGE_ROCKETMQ_CLUSTER(STORAGE, STORAGE_ROCKETMQ, CLUSTER, DEFINITION, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_NAMESERVER(STORAGE, STORAGE_ROCKETMQ_CLUSTER, META, DEFAULT, RemotingType.ROCKETMQ_NAMESERVER),

    STORAGE_ROCKETMQ_BROKER(STORAGE, STORAGE_ROCKETMQ_CLUSTER, RUNTIME, DEFINITION, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE(STORAGE, STORAGE_ROCKETMQ_BROKER, RUNTIME, DEFAULT, RemotingType.ROCKETMQ),

    STORAGE_ROCKETMQ_BROKER_RAFT(STORAGE, STORAGE_ROCKETMQ_BROKER, RUNTIME, DEFAULT, RemotingType.ROCKETMQ),


    STORAGE_KAFKA(STORAGE_ROCKETMQ.code + 1),

    STORAGE_KAFKA_CLUSTER(STORAGE, STORAGE_KAFKA, CLUSTER, DEFINITION, RemotingType.KAFKA),

    STORAGE_KAFKA_ZK(STORAGE, STORAGE_KAFKA_CLUSTER, META, META_TYPE_ZK, RemotingType.ZK),

    STORAGE_KAFKA_RAFT(STORAGE, STORAGE_KAFKA_CLUSTER, META_AND_RUNTIME, STORAGE_KAFKA, RemotingType.KAFKA),

    STORAGE_KAFKA_BROKER(STORAGE, STORAGE_KAFKA_CLUSTER, RUNTIME, DEFAULT, RemotingType.KAFKA),


    STORAGE_REDIS(STORAGE_KAFKA.code + 1),

    STORAGE_REDIS_CLUSTER(STORAGE, STORAGE_REDIS, CLUSTER, DEFINITION, RemotingType.REDIS),

    STORAGE_REDIS_BROKER(STORAGE, STORAGE_REDIS_CLUSTER, RUNTIME, DEFAULT, RemotingType.REDIS),

    STORAGE_JVM(STORAGE_REDIS.code + 1),

    STORAGE_JVM_CLUSTER(STORAGE, STORAGE_JVM, CLUSTER, DEFINITION, RemotingType.JVM),

    STORAGE_JVM_META(STORAGE, STORAGE_JVM_CLUSTER, META, DEFAULT, RemotingType.JVM),

    STORAGE_JVM_BROKER(STORAGE, STORAGE_JVM_CLUSTER, RUNTIME, DEFAULT, RemotingType.JVM),

    STORAGE_JVM_CAP(STORAGE_REDIS.code + 1),

    STORAGE_JVM_CAP_CLUSTER(STORAGE, STORAGE_JVM_CAP, CLUSTER, DEFINITION, RemotingType.JVM),

    STORAGE_JVM_CAP_BROKER(STORAGE, STORAGE_JVM_CAP_CLUSTER, META_AND_RUNTIME, DEFAULT, RemotingType.JVM),

    STORAGE_JVM_CAP_META(STORAGE, STORAGE_JVM_CAP_CLUSTER, META, DEFAULT, RemotingType.JVM),
    ;


    private static final List<ClusterType> STORAGE_MAIN_CLUSTER_TYPE_LIST = new ArrayList<>();

    private static final List<ClusterType> STORAGE_META_CLUSTER_TYPE_LIST = new ArrayList<>();

    private static final List<ClusterType> STORAGE_RUNTIME_CLUSTER_TYPE_LIST = new ArrayList<>();

    private static final List<ClusterType> STORAGE_META_RUNTIME_TYPE_LIST = new ArrayList<>();

    private static final Map<ClusterType, ClusterTypeChild> CLUSTER_TYPE_CLUSTER_TYPE_CHILD_MAP = new HashMap<>();

    static {
        for (ClusterType clusterType : ClusterType.values()) {
            if (Objects.isNull(clusterType.assemblyName)) {
                continue;
            }
            ClusterTypeChild clusterTypeChild =
                CLUSTER_TYPE_CLUSTER_TYPE_CHILD_MAP.computeIfAbsent(clusterType.assemblyName, k -> new ClusterTypeChild());
            clusterType.assemblyName.clusterTypeChild = clusterTypeChild;

            clusterType.clusterTypeChild = CLUSTER_TYPE_CLUSTER_TYPE_CHILD_MAP.computeIfAbsent(clusterType, k -> new ClusterTypeChild());
            if (clusterType.isMeta()) {
                clusterTypeChild.metaClusterType.add(clusterType);
            } else if (clusterType.isRuntime()) {
                clusterTypeChild.runtimeClusterType.add(clusterType);
            } else if (clusterType.isDefinition()) {
                clusterTypeChild.definitionClusterType.add(clusterType);
            } else if (clusterType.isCluster()) {
                clusterTypeChild.clusterType.add(clusterType);
            }
            if (clusterType.isStorageCluster()) {
                STORAGE_MAIN_CLUSTER_TYPE_LIST.add(clusterType);
            }
            if (clusterType.isStorageMeta()) {
                STORAGE_META_CLUSTER_TYPE_LIST.add(clusterType);
            }
            if (clusterType.isStorageRuntime()) {
                STORAGE_RUNTIME_CLUSTER_TYPE_LIST.add(clusterType);
            }
            if (clusterType.isMetaAndRuntime()) {
                STORAGE_META_RUNTIME_TYPE_LIST.add(clusterType);
            }

        }
        for (ClusterType clusterType : ClusterType.values()) {
            clusterType.getHigher();
            clusterType.injection();
        }
    }

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
    private ClusterType higherType;

    private ClusterTypeChild clusterTypeChild;

    private List<ClusterType> mainClusterType;
    private List<ClusterType> metaClusterType;
    private List<ClusterType> runtimeClusterType;


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

    public static List<ClusterType> getStorageCluster() {
        return STORAGE_MAIN_CLUSTER_TYPE_LIST;
    }

    public static List<ClusterType> getStorageMetaCluster() {
        return STORAGE_META_CLUSTER_TYPE_LIST;
    }

    public static List<ClusterType> getStorageRuntimeCluster() {
        return STORAGE_RUNTIME_CLUSTER_TYPE_LIST;
    }

    public static List<ClusterType> getStorageMetaRuntimeCluster() {
        return STORAGE_META_RUNTIME_TYPE_LIST;
    }

    /**
     * 半托管状态需要 如要从
     */
    public boolean isEventMethMeta() {
        return this.eventmeshNodeType.equals(EVENTMESH) && this.assemblyNodeType.equals(META);
    }


    public boolean isEventCluster() {
        return this.eventmeshNodeType.equals(EVENTMESH) && this.assemblyNodeType.equals(CLUSTER);
    }

    public boolean isEventMethRuntime() {
        return this == EVENTMESH_RUNTIME || this == EVENTMESH_JVM_RUNTIME;
    }

    public boolean isMeta() {
        if (Objects.isNull(this.assemblyNodeType)) {
            return false;
        }
        return this.assemblyNodeType.equals(META);
    }

    public boolean isCluster() {
        if (Objects.isNull(this.assemblyNodeType)) {
            return false;
        }
        return this.assemblyNodeType.equals(CLUSTER);
    }

    public boolean isMetaAndRuntime() {
        if (Objects.isNull(this.eventmeshNodeType)) {
            return false;
        }
        return this.eventmeshNodeType.equals(META_AND_RUNTIME);
    }

    public boolean isRuntime() {
        if (Objects.isNull(this.assemblyNodeType)) {
            return false;
        }
        return this.assemblyNodeType.equals(RUNTIME) || this.eventmeshNodeType.equals(META_AND_RUNTIME);
    }

    public boolean isStorage() {
        if (Objects.isNull(this.eventmeshNodeType)) {
            return false;
        }
        return this.eventmeshNodeType.equals(STORAGE);
    }

    public boolean isMainCluster() {
        return Objects.equals(this, ClusterType.EVENTMESH_CLUSTER) || Objects.equals(this.assemblyNodeType, ClusterType.CLUSTER);
    }


    public boolean isHealthTopic() {
        return Objects.equals(this, EVENTMESH_RUNTIME) || this.isStorageRuntime();
    }

    public boolean isStorageCluster() {
        return Objects.equals(this.eventmeshNodeType, STORAGE) && Objects.equals(this.assemblyNodeType, CLUSTER);
    }

    public boolean isStorageRuntime() {
        return Objects.equals(this.eventmeshNodeType, STORAGE) && Objects.equals(this.assemblyNodeType, RUNTIME);
    }

    public boolean isStorageMeta() {
        return Objects.equals(this.eventmeshNodeType, STORAGE) && Objects.equals(this.assemblyNodeType, META);
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

    public List<ClusterType> getMetaClusterInStorage() {
        List<ClusterType> list = new ArrayList<>();
        for (ClusterType allClusterType : ClusterType.values()) {
            if (Objects.equals(allClusterType.eventmeshNodeType, STORAGE) && Objects.equals(allClusterType.assemblyNodeType, META)) {
                list.add(allClusterType);
            }
        }
        return list;
    }

    public List<ClusterType> getThisInAllRuntimeCluster() {
        return this.clusterTypeChild.allRuntimeClusterType;
    }

    public List<ClusterType> getFrameworkInAllMetaCluster() {
        return this.higherType.clusterTypeChild.allMetaClusterType;
    }

    public List<ClusterType> getFrameworkInAllRuntimeCluster() {
        return this.higherType.clusterTypeChild.allRuntimeClusterType;
    }

    public List<ClusterType> getMainClusterType() {
        return this.getThisClusterType(CLUSTER, this.mainClusterType, "mainClusterType");

    }

    public List<ClusterType> getMetaClusterType() {
        return this.getThisClusterType(META, this.metaClusterType, "metaClusterType");
    }

    public List<ClusterType> getRuntimeClusterType() {
        return this.getThisClusterType(RUNTIME, this.runtimeClusterType, "runtimeClusterType");
    }

    void injection() {
        if (Objects.isNull(this.clusterTypeChild)) {
            return;
        }
        if (Objects.isNull(this.higherType)) {
            return;
        }
        if (this.clusterTypeChild.isRuntimeAdd.get()) {
            return;
        }
        if (Objects.equals(this.higherType, this)) {
            this.clusterTypeChild.isRuntimeAdd.set(true);
            return;
        }
        this.clusterTypeChild.isRuntimeAdd.set(true);
        this.assemblyName.injection(List.of(this), this.assemblyNodeType);
    }


    void injection(List<ClusterType> list, ClusterType clusterType) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        if (Objects.equals(clusterType, META)) {
            this.clusterTypeChild.allMetaClusterType.addAll(list);
            return;
        }
        if (Objects.equals(clusterType, CLUSTER)) {
            this.clusterTypeChild.allClusterType.addAll(list);
            return;
        }
        if (Objects.equals(clusterType, RUNTIME)) {
            this.clusterTypeChild.allRuntimeClusterType.addAll(list);
        }
        if (Objects.equals(this.higherType, this)) {
            return;
        }
        this.assemblyName.injection(list, clusterType);
    }

    /**
     *
     */
    public ClusterType getHigher() {
        if (Objects.nonNull(this.higherType)) {
            return this.higherType;
        }
        if (this.code != 0) {
            this.higherType = this;
            return this.higherType;
        }
        if (this.eventmeshNodeType == DEFAULT) {
            this.higherType = this;
            return this.higherType;
        }
        if (Objects.isNull(this.assemblyName)) {
            return this;
        }
        ClusterType assemblyName = this.assemblyName;
        ClusterType lastAssemblyName = this;
        for (; ; ) {
            if (Objects.isNull(assemblyName.assemblyName)) {
                this.higherType = lastAssemblyName;
                break;
            }
            lastAssemblyName = assemblyName;
            assemblyName = assemblyName.assemblyName;
        }
        return this.higherType;
    }


    private List<ClusterType> getThisClusterType(ClusterType assemblyNodeType, List<ClusterType> clusterTypeList, String fieldName) {
        if (Objects.equals(assemblyNodeType, this.assemblyNodeType) && !this.isDefinition()) {
            return null;
        }
        if (Objects.nonNull(clusterTypeList)) {
            return clusterTypeList;
        }
        clusterTypeList = new ArrayList<>();
        synchronized (this) {
            for (ClusterType allClusterType : ClusterType.values()) {
                if (this == allClusterType) {
                    continue;
                }
                if (Objects.equals(this.eventmeshNodeType, allClusterType.eventmeshNodeType)
                    && (Objects.equals(this.assemblyName, allClusterType.assemblyName) || Objects.equals(this, allClusterType.assemblyName))
                    && Objects.equals(assemblyNodeType, allClusterType.assemblyNodeType)) {
                    clusterTypeList.add(allClusterType);
                }
            }
        }
        if (clusterTypeList.isEmpty()) {
            // TODO
            // String message = String.format("cluster %s assemblyNodeType is %s fieldName is %s ", this, assemblyNodeType, fieldName);
            // throw new RuntimeException(message);
        }

        try {
            FieldUtils.writeField(this, fieldName, clusterTypeList, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return clusterTypeList;
    }


    static class ClusterTypeChild {

        List<ClusterType> metaClusterType = new ArrayList<>();

        List<ClusterType> runtimeClusterType = new ArrayList<>();

        AtomicBoolean isRuntimeAdd = new AtomicBoolean(false);

        List<ClusterType> definitionClusterType = new ArrayList<>();

        List<ClusterType> clusterType = new ArrayList<>();

        List<ClusterType> allClusterType = new ArrayList<>();

        List<ClusterType> allMetaClusterType = new ArrayList<>();

        List<ClusterType> allRuntimeClusterType = new ArrayList<>();

    }

}
