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

package org.apache.eventmesh.dashboard.console.databuild;

import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterEntityMapperTest;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapperTest;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeEntityMapperTest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import lombok.Getter;


/**
 * 创建全 多矩阵的 cluster 构建集群
 */
public class BuildFullSceneData {


    private final List<Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType>> tripleList = new ArrayList<>();

    private final List<Triple<ClusterType, List<ClusterType>/* meta */, List<ClusterType>>> clusterTypeTripleList = new ArrayList<>();


    @Getter
    private final List<Pair<ClusterEntity, List<ClusterEntity>>> eventMeshRuntimeTripleList = new ArrayList<>();

    @Getter
    private final List<Triple<ClusterEntity, List<ClusterFrameworkData>/* meta cluster */, List<ClusterFrameworkData> /* runtime cluster*/>>
        clusterTripleList =
        new ArrayList<>();

    @Getter
    private final List<ClusterEntity> clusterEntityList = new ArrayList<>();

    @Getter
    private final List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();


    private final Map<Long, List<RuntimeEntity>> runtimeEntityMap = new HashMap<>();


    private final ShareMetadata runtimeShareMetadata = new ShareMetadata();

    private final ShareMetadata mateShareMetadata = new ShareMetadata();


    @Getter
    private BuildFullMetadata buildFullMetadata;


    private ClusterEntity createClusterEntity(ClusterType clusterType) {
        ClusterEntity clusterEntity = ClusterEntityMapperTest.createClusterEntity();
        clusterEntity.setClusterType(clusterType);
        this.clusterEntityList.add(clusterEntity);
        return clusterEntity;
    }

    /**
     * 构建思路
     * <p>
     * 维度
     * <ol>
     *     <li>所有集群</li>
     *     <li>eventmesh 下 所有不同类型 storage</li>
     *     <li>cluster 集群下 所有不同类型 meta</li>
     *     <li>cluster 集群下 所有不同类型 runtime</li>
     *     <li>ClusterTrusteeshipType 所有状态</li>
     *     <li>FirstToWhom 所有状态</li>
     *     <li>ClusterOwnType 状态</li>
     * </ol>
     * <p>
     * <p>
     *  TODO 如何构建 ， RocketMQ 三个结构
     */
    public void build() {
        // 计算 基本枚举 矩阵
        this.buildBase();
        // 计算 main cluster 下  meta 与 runtime 的矩阵 以及 共享集群概率
        this.buildReplica();

        this.association();
    }

    public void association() {
        tripleList.forEach(this::buildCluster);
        this.bindStorage();


    }

    public void bindStorage() {
        if (!this.buildFullMetadata.isEventMeshEnabled()) {
            return;
        }
        // 这里建 eventmesh 集群 与 存储集群 关联。 随机关联
        List<ClusterEntity> eventmeshClusterList = new ArrayList<>();
        List<ClusterEntity> storageClusterList = new ArrayList<>();
        this.clusterTripleList.forEach(tripleList1 -> {
            ClusterEntity clusterEntity = tripleList1.getLeft();
            if (clusterEntity.getClusterType().isStorageCluster()) {
                storageClusterList.add(clusterEntity);
            } else if (clusterEntity.getClusterType().isEventCluster()) {
                eventmeshClusterList.add(clusterEntity);
            }
        });
        int num = storageClusterList.size() % eventmeshClusterList.size();
        int count = storageClusterList.size() / eventmeshClusterList.size();
        int start = 0, subCount = 0;
        for (ClusterEntity clusterEntity : eventmeshClusterList) {
            subCount = subCount + (num-- > 0 ? count + 1 : count);
            eventMeshRuntimeTripleList.add(Pair.of(clusterEntity, storageClusterList.subList(start, subCount)));
            start = start + subCount;
        }
    }

    public List<Triple<ClusterEntity, List<ClusterFrameworkData>, List<ClusterFrameworkData>>> getClusterTripleList(ClusterType clusterType) {
        List<Triple<ClusterEntity, List<ClusterFrameworkData>/* meta cluster */, List<ClusterFrameworkData> /* runtime cluster*/>> tripleList =
            new ArrayList<>();
        this.clusterTripleList.forEach(tripleList1 -> {
            if (tripleList1.getLeft().getClusterType() == clusterType) {
                tripleList.add(tripleList1);
            }
        });
        return tripleList;
    }

    /**
     * 计算 ClusterTrusteeshipType, FirstToWhom, ClusterOwnType 的 笛卡尔 集
     */
    public void buildBase() {
        if (CollectionUtils.isEmpty(buildFullMetadata.getClusterTrusteeshipTypesSet())) {
            buildFullMetadata.setClusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF));
        }
        if (CollectionUtils.isEmpty(buildFullMetadata.getFirstToWhomSet())) {
            buildFullMetadata.setFirstToWhomSet(Set.of(FirstToWhom.NOT));
        }
        if (CollectionUtils.isEmpty(buildFullMetadata.getClusterOwnTypesSet())) {
            buildFullMetadata.setClusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE));
        }

        buildFullMetadata.getClusterTrusteeshipTypesSet().forEach(clusterTrusteeshipType -> {
            buildFullMetadata.getFirstToWhomSet().forEach(firstToWhom -> {
                buildFullMetadata.getClusterOwnTypesSet().forEach(clusterOwnType -> {
                    Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple =
                        Triple.of(clusterTrusteeshipType, firstToWhom, clusterOwnType);
                    if (ClusterOwnType.SHARE == clusterOwnType) {
                        this.mateShareMetadata.shareNum++;
                        this.runtimeShareMetadata.shareNum++;
                    }
                    this.tripleList.add(triple);
                });
            });
        });
    }


    public void buildReplica() {

        this.buildFullMetadata.getClusterTypeSet().forEach(this::buildReplica);

        this.buildShare(this.buildFullMetadata.getRuntimeTypeSet(), this.runtimeShareMetadata);
        this.buildShare(this.buildFullMetadata.getMetaTypeSet(), this.mateShareMetadata);

    }

    private void buildCluster(Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        clusterTypeTripleList.forEach(cluster -> {
            ClusterType clusterType = cluster.getLeft();
            // 构建 主 集群
            ClusterEntity clusterEntity = this.createClusterEntity(clusterType);
            this.build(clusterEntity, triple);
            clusterTripleList.add(Triple.of(clusterEntity, this.buildCluster(clusterType, cluster.getMiddle(), triple),
                this.buildCluster(clusterType, cluster.getRight(), triple)));
        });

    }

    /**
     * 随机创建 meta or runtime 集群 数量。随机加入一些共享集群
     */
    private List<ClusterFrameworkData> buildCluster(ClusterType clusterType, List<ClusterType> clusterTypeList,
        Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        Set<ClusterType> oneSet = Set.of(ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_ZK, ClusterType.STORAGE_KAFKA_RAFT,
            ClusterType.STORAGE_JVM_CAP_CLUSTER);
        List<ClusterFrameworkData> runtimeClusterEntity = new ArrayList<>();
        Random random = new Random();
        clusterTypeList.forEach(middle -> {
            Map<ClusterType, List<ClusterEntity>> shareMap = middle.isMeta() ? mateShareMetadata.getShareMap() : runtimeShareMetadata.getShareMap();
            int num = 1;
            if (!oneSet.contains(middle)) {
                if (middle.isMeta()) {
                    num = random.nextInt(3) + 1;
                } else {
                    num = random.nextInt(5) + 2;
                }
            }
            int shareNum = random.nextInt(num) + 1;
            Set<Integer> shareNumSet = new HashSet<>();
            for (int i = 0; i < num; i++) {
                ClusterFrameworkData runtime;
                if (triple.getRight() == ClusterOwnType.SHARE && shareNum != 0) {
                    List<ClusterEntity> clusterEntitieList = shareMap.get(middle);
                    for (; ; ) {
                        int randomNum = random.nextInt(clusterEntitieList.size());
                        if (shareNumSet.contains(randomNum)) {
                            continue;
                        }
                        shareNumSet.add(randomNum);
                        ClusterEntity clusterEntity = clusterEntitieList.get(random.nextInt(clusterEntitieList.size()));
                        runtime = new ClusterFrameworkData();
                        runtime.setClusterEntity(clusterEntity);
                        break;
                    }
                } else {
                    runtime = this.createClusterFrameworkData(middle);
                }
                runtime.build(triple);
                runtimeClusterEntity.add(runtime);
            }
        });
        return runtimeClusterEntity;
    }

    private void build(ClusterFrameworkData clusterFrameworkData, Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        this.build(clusterFrameworkData.clusterEntity, triple);
        if (CollectionUtils.isNotEmpty(clusterFrameworkData.childClusterEntityList)) {
            clusterFrameworkData.childClusterEntityList.forEach(childClusterEntity -> {
                build(childClusterEntity, triple);
            });
        }
    }

    private void build(ClusterEntity clusterEntity, Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        clusterEntity.setTrusteeshipType(triple.getLeft());
        clusterEntity.setFirstToWhom(triple.getMiddle());
        clusterEntity.setClusterOwnType(triple.getRight());
    }

    private ClusterFrameworkData createClusterFrameworkData(ClusterType clusterType) {
        ClusterFrameworkData clusterFrameworkData = new ClusterFrameworkData();
        if (clusterType.isStorage() && clusterType.isRuntime()) {
            ClusterType assembly = clusterType.getAssemblyNodeType();
            if (assembly.isRuntime()) {
                ClusterEntity clusterEntity = this.createClusterEntity(assembly);
                clusterFrameworkData.setClusterEntity(clusterEntity);
                List<ClusterEntity> mainSlaveClusterEntity = new ArrayList<>();
                clusterFrameworkData.setChildClusterEntityList(mainSlaveClusterEntity);
                Random random = new Random();
                int num = random.nextInt(5) + 2;
                for (int i = 0; i < num; i++) {
                    ClusterEntity clusterEntity1 = createClusterEntity(clusterType);
                    clusterEntity1.setReplicationType(ReplicationType.MAIN_SLAVE);
                    mainSlaveClusterEntity.add(clusterEntity1);
                }
                return clusterFrameworkData;
            }
        }
        ClusterEntity clusterEntity = this.createClusterEntity(clusterType);
        clusterFrameworkData.setClusterEntity(clusterEntity);
        return clusterFrameworkData;
    }

    /**
     * 随机创建 共享集群，
     */
    private void buildShare(Set<ClusterType> clusterTypeList, ShareMetadata mateShareMetadata) {
        Random random = new Random();
        clusterTypeList.forEach(clusterType -> {
            AtomicInteger atomicInteger1 = new AtomicInteger(mateShareMetadata.getShareNum());
            for (; ; ) {
                int num = random.nextInt(3) + 2;
                if (num > atomicInteger1.get()) {
                    num = atomicInteger1.get();
                }
                atomicInteger1.set(atomicInteger1.get() - num);
                ClusterEntity clusterEntity = this.createClusterEntity(clusterType);
                //clusterEntityList.add(clusterEntity);
                clusterEntity.setClusterOwnType(ClusterOwnType.SHARE);
                mateShareMetadata.getShareMap().computeIfAbsent(clusterType, (key) -> new ArrayList<>()).add(clusterEntity);
                if (atomicInteger1.get() <= 0) {
                    return;
                }
            }

        });
    }

    /**
     * 得到 clusterType 里面 所有 meta 类型 与  broker 类型。 </p> 形成 笛卡尔 集. 一对一，一对多，全支持。比如 RocketMQ , 有两种 runtime，那么 1，2， 1+2 三种情况
     */
    public void buildReplica(ClusterType type) {
        List<ClusterType> metaList = new ArrayList<>();
        for (ClusterType meta : type.getMetaClusterType()) {
            metaList.add(meta);
            List<ClusterType> runtimeList = new ArrayList<>();
            for (ClusterType runtime : type.getRuntimeClusterType()) {
                if (runtime.isDefinition()) {
                    for (ClusterType clusterType : runtime.getRuntimeClusterType()) {
                        runtimeList.add(clusterType);
                        clusterTypeTripleList.add(Triple.of(type, List.copyOf(metaList), List.of(clusterType)));
                        if (runtimeList.size() > 1) {
                            clusterTypeTripleList.add(Triple.of(type, List.copyOf(metaList), List.copyOf(runtimeList)));
                        }
                    }
                } else {
                    runtimeList.add(runtime);
                    clusterTypeTripleList.add(Triple.of(type, List.copyOf(metaList), List.of(runtime)));
                    if (runtimeList.size() > 1) {
                        clusterTypeTripleList.add(Triple.of(type, List.copyOf(metaList), List.copyOf(runtimeList)));
                    }
                }
            }
        }
    }


    public List<RuntimeEntity> createRuntimeEntity() {
        this.clusterEntityList.forEach(this::createRuntimeEntity);
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        this.runtimeEntityMap.values().forEach(runtimeEntityList::addAll);
        return runtimeEntityList;
    }

    public List<RuntimeEntity> getRuntimeEntityListByClusterFrameworkData(List<ClusterFrameworkData> clusterFrameworkDataList) {
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (ClusterFrameworkData clusterFrameworkData : clusterFrameworkDataList) {
            runtimeEntityList.addAll(this.runtimeEntityMap.get(clusterFrameworkData.getClusterEntity().getId()));
            if (CollectionUtils.isNotEmpty(clusterFrameworkData.childClusterEntityList)) {
                clusterFrameworkData.childClusterEntityList.forEach(childClusterEntity -> {
                    runtimeEntityList.addAll(this.runtimeEntityMap.get(childClusterEntity.getId()));
                });
            }
        }
        return runtimeEntityList;
    }


    public List<RuntimeEntity> getRuntimeEntityList(List<ClusterEntity> clusterEntityList) {
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (ClusterEntity clusterEntity : clusterEntityList) {
            runtimeEntityList.addAll(this.runtimeEntityMap.get(clusterEntity.getId()));
        }
        return runtimeEntityList;
    }

    public List<ClusterRelationshipEntity> createClusterRelationshipEntity() {
        this.clusterTripleList.forEach(triple -> {

            triple.getMiddle().forEach(middle -> {
                this.createClusterRelationshipEntity(triple.getLeft(), middle);
            });
            triple.getRight().forEach(r -> {
                this.createClusterRelationshipEntity(triple.getLeft(), r);
            });
        });
        this.eventMeshRuntimeTripleList.forEach(tripleList1 -> {
            tripleList1.getRight().forEach(r -> {
                this.createClusterRelationshipEntity(tripleList1.getLeft(), r);
            });
        });
        return this.clusterRelationshipEntityList;
    }


    private void createClusterRelationshipEntity(ClusterEntity clusterEntity, ClusterFrameworkData clusterFrameworkData) {
        this.createClusterRelationshipEntity(clusterEntity, clusterFrameworkData.getClusterEntity());
        if (CollectionUtils.isNotEmpty(clusterFrameworkData.getChildClusterEntityList())) {
            clusterFrameworkData.getChildClusterEntityList().forEach(childClusterEntity -> {
                this.createClusterRelationshipEntity(clusterFrameworkData.getClusterEntity(), childClusterEntity);
            });
        }
    }

    private void createClusterRelationshipEntity(ClusterEntity clusterEntity, ClusterEntity relationshipClusterEntity) {
        ClusterRelationshipEntity clusterRelationshipEntity =
            ClusterRelationshipMapperTest.buildClusterRelationshipEntity(clusterEntity, relationshipClusterEntity);

        this.clusterRelationshipEntityList.add(clusterRelationshipEntity);
    }

    private void createRuntimeEntity(ClusterEntity clusterEntity) {
        if (!this.buildFullMetadata.getClusterOwnTypesSet().contains(clusterEntity.getClusterOwnType())) {
            return;
        }
        if (clusterEntity.getClusterType().isDefinition()) {
            return;
        }
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        Random random = new Random();
        int num = random.nextInt(3) + 3;
        for (int i = 0; i < num; i++) {
            RuntimeEntity runtimeEntity = RuntimeEntityMapperTest.createRuntimeEntity(clusterEntity);
            runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_FULL_WAIT);
            if (num == i + 1) {
                runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_WAIT);
            }
            runtimeEntityList.add(runtimeEntity);
            if (ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType()).isMainSlave()) {
                runtimeEntity.setReplicationType(ReplicationType.MAIN);
                RuntimeEntity slave = RuntimeEntityMapperTest.createRuntimeEntity(clusterEntity);
                slave.setDeployStatusType(runtimeEntity.getDeployStatusType());
                slave.setReplicationType(ReplicationType.SLAVE);
                runtimeEntityList.add(slave);
            }
        }
        this.runtimeEntityMap.put(clusterEntity.getId(), runtimeEntityList);
    }


    public void buildFullMetadata() {
        this.buildFullMetadata = BuildFullMetadata.builder().eventMeshEnabled(true).clusterTypeSet(new HashSet<>(ClusterType.getStorageCluster()))
            .metaTypeSet(new HashSet<>(ClusterType.getStorageRuntimeCluster())).runtimeTypeSet(new HashSet<>(ClusterType.getStorageMetaCluster()))
            .firstToWhomSet(Set.of(FirstToWhom.NOT)).clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF))
            .clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();

        this.buildFullMetadata.getClusterTypeSet().add(ClusterType.EVENTMESH_CLUSTER);
        this.buildFullMetadata.getMetaTypeSet().addAll(ClusterType.EVENTMESH_CLUSTER.getMetaClusterType());
        this.buildFullMetadata.getRuntimeTypeSet().addAll(ClusterType.EVENTMESH_CLUSTER.getMetaClusterInStorage());
        this.build();
    }

    public void buildStorageAll() {
        this.buildFullMetadata = BuildFullMetadata.builder().clusterTypeSet(new HashSet<>(ClusterType.getStorageCluster()))
            .metaTypeSet(new HashSet<>(ClusterType.getStorageRuntimeCluster())).runtimeTypeSet(new HashSet<>(ClusterType.getStorageMetaCluster()))
            .firstToWhomSet(Set.of(FirstToWhom.NOT)).clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF))
            .clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();
        this.build();
    }

    public void build(ClusterType clusterType) {
        this.buildFullMetadata =
            BuildFullMetadata.builder().clusterTypeSet(Set.of(clusterType)).metaTypeSet(Set.copyOf(clusterType.getMetaClusterType()))
                .runtimeTypeSet(Set.copyOf(clusterType.getRuntimeClusterType())).firstToWhomSet(Set.of(FirstToWhom.NOT))
                .clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF)).clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();
        this.build();
    }

    public void build(BuildFullMetadata buildFullMetadata) {
        this.buildFullMetadata = buildFullMetadata;
        this.build();
    }

    public void buildBySupplement(BuildFullMetadata buildFullMetadata) {
        Set<ClusterType> clusterTypeSet = buildFullMetadata.getClusterTypeSet();
        Set<ClusterType> metaTypeSet = new HashSet<>();
        Set<ClusterType> runtimeTypeSet = new HashSet<>();
        buildFullMetadata.setMetaTypeSet(metaTypeSet);
        buildFullMetadata.setRuntimeTypeSet(runtimeTypeSet);
        clusterTypeSet.forEach(type -> {
            metaTypeSet.addAll(type.getMetaClusterType());
            runtimeTypeSet.addAll(type.getRuntimeClusterType());
        });
        this.build(buildFullMetadata);
    }

    @Data
    private static class ShareMetadata {

        private final List<ClusterEntity> shareClusterEntityList = new ArrayList<>();
        private final Map<ClusterType, List<ClusterEntity>> shareMap = new HashMap<>();
        /**
         * 共享 集群 概率值
         */
        private Integer shareNum = 0;


    }


    @Data
    public static class ClusterFrameworkData {

        private ClusterEntity clusterEntity;


        private List<ClusterEntity> childClusterEntityList;


        public void addClusterEntity(List<ClusterEntity> clusterEntityList) {
            clusterEntityList.add(clusterEntity);
            clusterEntityList.addAll(childClusterEntityList);
        }

        public void build(Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
            clusterEntity.setTrusteeshipType(triple.getLeft());
            clusterEntity.setFirstToWhom(triple.getMiddle());
            clusterEntity.setClusterOwnType(triple.getRight());
            if (CollectionUtils.isNotEmpty(childClusterEntityList)) {
                this.childClusterEntityList.forEach(childClusterEntity -> {
                    childClusterEntity.setTrusteeshipType(triple.getLeft());
                    childClusterEntity.setFirstToWhom(triple.getMiddle());
                    childClusterEntity.setClusterOwnType(triple.getRight());
                });
            }
        }
    }

}
