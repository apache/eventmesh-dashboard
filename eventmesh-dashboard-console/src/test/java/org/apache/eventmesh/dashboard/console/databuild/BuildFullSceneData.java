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
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterEntityMapperTest;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterRelationshipMapperTest;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeEntityMapperTest;

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

public class BuildFullSceneData {


    private final List<Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType>> tripleList = new ArrayList<>();

    private final List<Triple<ClusterType, List<ClusterType>/* meta */, List<ClusterType>>> clusterTypeTripleList = new ArrayList<>();


    @Getter
    private final List<Pair<ClusterEntity, List<ClusterEntity>>> eventMeshRuntimeTripleList = new ArrayList<>();

    @Getter
    private final List<Triple<ClusterEntity, List<ClusterEntity>/* meta */, List<ClusterEntity>>> clusterTripleList = new ArrayList<>();

    @Getter
    private final List<ClusterEntity> clusterEntityList = new ArrayList<>();

    @Getter
    private final List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();


    private final Map<Long, RuntimeEntity> runtimeEntityMap = new HashMap<>();


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
        this.buildBase();
        this.buildReplica();
        this.association();
    }

    public void association() {
        tripleList.forEach(this::buildCluster);
        // 写入数据库

        //this.createClusterRelationshipEntity();
        // 写入数据库

        //this.clusterEntityList.forEach(this::createRuntimeEntity);
        // 写入数据库

    }

    public List<Triple<ClusterEntity, List<ClusterEntity>, List<ClusterEntity>>> getClusterTripleList(ClusterType clusterType) {
        List<Triple<ClusterEntity, List<ClusterEntity>, List<ClusterEntity>>> tripleList = new ArrayList<>();
        this.clusterTripleList.forEach(tripleList1 -> {
            if (tripleList1.getLeft().getClusterType() == clusterType) {
                tripleList.add(tripleList1);
            }
        });
        return tripleList;
    }

    public void buildBase() {
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
            ClusterEntity clusterEntity = this.createClusterEntity(clusterType);
            this.build(clusterEntity, triple);
            clusterTripleList.add(Triple.of(clusterEntity, this.buildCluster(clusterType, cluster.getMiddle(), triple),
                this.buildCluster(clusterType, cluster.getRight(), triple)));
        });
        if (this.buildFullMetadata.isEventMeshEnabled()) {
            List<ClusterEntity> eventMeshEntities = new ArrayList<>();
            List<ClusterEntity> clusterEntityList1 = new ArrayList<>();
            this.clusterTripleList.forEach(tripleList1 -> {
                ClusterEntity clusterEntity = tripleList1.getLeft();
                if (clusterEntity.getClusterType() == ClusterType.EVENTMESH_CLUSTER) {
                    eventMeshEntities.add(clusterEntity);
                } else {
                    clusterEntityList1.add(clusterEntity);
                }
            });
            int num = clusterEntityList1.size() % eventMeshEntities.size();
            for (int i = 0; i < eventMeshEntities.size(); i++) {
                eventMeshRuntimeTripleList.add(Pair.of(eventMeshEntities.get(i), clusterEntityList1.subList(i * num, num)));
            }

        }
    }

    private List<ClusterEntity> buildCluster(ClusterType clusterType, List<ClusterType> clusterTypeList,
        Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        Set<ClusterType> oneSet = Set.of(ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_ZK, ClusterType.STORAGE_KAFKA_RAFT);
        List<ClusterEntity> runtimeClusterEntity = new ArrayList<>();
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
                ClusterEntity runtime = null;
                if (triple.getRight() == ClusterOwnType.SHARE && shareNum != 0) {
                    List<ClusterEntity> clusterEntitieList = shareMap.get(middle);
                    for (; ; ) {
                        int randomNum = random.nextInt(clusterEntitieList.size());
                        if (shareNumSet.contains(randomNum)) {
                            continue;
                        }
                        shareNumSet.add(randomNum);
                        runtime = clusterEntitieList.get(random.nextInt(clusterEntitieList.size()));
                        break;
                    }
                } else {
                    runtime = this.createClusterEntity(middle);
                }
                runtimeClusterEntity.add(runtime);

                if (runtime != null) {
                    this.build(runtime, triple);
                }
            }
        });
        return runtimeClusterEntity;
    }

    private void build(ClusterEntity clusterEntity, Triple<ClusterTrusteeshipType, FirstToWhom, ClusterOwnType> triple) {
        clusterEntity.setTrusteeshipType(triple.getLeft());
        clusterEntity.setFirstToWhom(triple.getMiddle());
        clusterEntity.setClusterOwnType(triple.getRight());
    }


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
        return new ArrayList<>(this.runtimeEntityMap.values());
    }

    public List<RuntimeEntity> getRuntimeEntityList(List<ClusterEntity> clusterEntityList) {
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (ClusterEntity clusterEntity : clusterEntityList) {
            runtimeEntityList.add(this.runtimeEntityMap.get(clusterEntity.getId()));
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
        Random random = new Random();
        int num = random.nextInt(3) + 3;
        for (int i = 0; i < num; i++) {
            RuntimeEntity runtimeEntity = RuntimeEntityMapperTest.createRuntimeEntity(clusterEntity);
            runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_FULL_WAIT);
            if (num == i + 1) {
                runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_WAIT);
            }
            this.runtimeEntityMap.put(runtimeEntity.getId(), runtimeEntity);
        }
    }


    public void buildFullMetadata() {
        this.buildFullMetadata =
            BuildFullMetadata.builder()
                .eventMeshEnabled(true)
                .clusterTypeSet(new HashSet<>(ClusterType.getStorageCluster()))
                .metaTypeSet(new HashSet<>(ClusterType.getStorageRuntimeCluster()))
                .runtimeTypeSet(new HashSet<>(ClusterType.getStorageMetaCluster()))
                .firstToWhomSet(Set.of(FirstToWhom.NOT))
                .clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF))
                .clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();

        this.buildFullMetadata.getClusterTypeSet().add(ClusterType.EVENTMESH_CLUSTER);
        this.buildFullMetadata.getMetaTypeSet().addAll(ClusterType.EVENTMESH_CLUSTER.getMetaClusterType());
        this.buildFullMetadata.getRuntimeTypeSet().addAll(ClusterType.EVENTMESH_CLUSTER.getMetaClusterInStorage());
        this.build();
    }

    public void buildStorageAll() {
        this.buildFullMetadata =
            BuildFullMetadata.builder()
                .clusterTypeSet(new HashSet<>(ClusterType.getStorageCluster()))
                .metaTypeSet(new HashSet<>(ClusterType.getStorageRuntimeCluster()))
                .runtimeTypeSet(new HashSet<>(ClusterType.getStorageMetaCluster()))
                .firstToWhomSet(Set.of(FirstToWhom.NOT))
                .clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF))
                .clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();
        this.build();
    }

    public void build(ClusterType clusterType) {
        this.buildFullMetadata = BuildFullMetadata.builder().clusterTypeSet(Set.of(clusterType))
            .metaTypeSet(Set.copyOf(clusterType.getMetaClusterType()))
            .runtimeTypeSet(Set.copyOf(clusterType.getRuntimeClusterType()))
            .firstToWhomSet(Set.of(FirstToWhom.NOT))
            .clusterTrusteeshipTypesSet(Set.of(ClusterTrusteeshipType.SELF))
            .clusterOwnTypesSet(Set.of(ClusterOwnType.INDEPENDENCE)).build();
        this.build();
    }

    public void build(BuildFullMetadata buildFullMetadata) {
        this.buildFullMetadata = buildFullMetadata;
    }

    @Data
    private static class ShareMetadata {

        private final List<ClusterEntity> shareClusterEntityList = new ArrayList<>();

        private Integer shareNum = 0;

        private final Map<ClusterType, List<ClusterEntity>> shareMap = new HashMap<>();
    }

}
