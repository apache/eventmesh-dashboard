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


package org.apache.eventmesh.dashboard.console.domain.Impl;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.ClusterAndRuntimeDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.DO.clusterRelationship.QueryListByClusterIdAndTypeDO;
import org.apache.eventmesh.dashboard.console.modle.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.modle.DO.domain.clusterAndRuntimeDomain.GetClusterInSyncReturnDO;
import org.apache.eventmesh.dashboard.console.modle.QO.cluster.QueryRelationClusterByClusterIdListAndType;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Component;

import net.sf.jsqlparser.statement.ExplainStatement.OptionType;

import lombok.Setter;

/**
 * TODO 1. 这个类提供 cluster 和 runtime 的关系 以及 树结构</p>
 *       2. 其他 domain ， 通过该 domain 获得 操作对象</p>
 *       3. 这个与 类 其实与 ColonyDO 域冲突了。 两个 domain 功能基本重叠了。</p>
 *          1. ColonyDO 的 QueueConditionHandler 没有 实现好，没有 SQL 的查询效果好。</p>
 *          2. 后期加强 QueueConditionHandler </p>
 *       4. 本域 是否 与 ColonyDO 统一，设计一个统一接口。</p>
 * TODO
 *     这个类主要是在主要在一下域使用：
 *          1. 部署 需要整体关系
 *          2. 运维 只需要 可操作节点
 *          3. 关系可视化 需要整体关系
 */
@Component
public class ClusterAndRuntimeDomainImpl implements ClusterAndRuntimeDomain {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private ClusterRelationshipService clusterRelationshipService;

    @Autowired
    private CacheProperties cacheProperties;


    @Override
    public List<ClusterEntity> getClusterByCLusterId(ClusterEntity clusterEntity) {
        return Collections.emptyList();
    }


    public void getClusterTree(ClusterEntity clusterEntity) {

    }

    /**
     * 运维操作使用列表啊：</p>
     * <p>
     * 部署操作使用列表：</p>
     *
     * @see org.apache.eventmesh.dashboard.console.controller.deploy.uninstall.UninstallClusterHandler
     * @see org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateClusterByCopyHandler
     * @see org.apache.eventmesh.dashboard.console.controller.deploy.relationship.RelationshipHandler
     * @see org.apache.eventmesh.dashboard.console.controller.deploy.relationship.UnRelationshipHandler
     * @see org.apache.eventmesh.dashboard.console.controller.deploy.pause.PauseCluster
     */
    @Override
    public ClusterAndRuntimeOfRelationshipDO getAllClusterAndRuntimeByCluster(ClusterEntity clusterEntity) {
        GetSyncObjectHandler getSyncObjectHandler = new GetSyncObjectHandler();
        getSyncObjectHandler.setClusterEntity(clusterEntity);
        return getSyncObjectHandler.deploy();
    }

    public GetClusterInSyncReturnDO getClusterInSync(ClusterEntity clusterEntity, OptionType optionType) {

        return null;
    }

    /**
     * 修改配置只能在一个维度的进行操作 topic 需要对 eventmesh 与 存储同时进行操作， acl 需要多维度操作，还是？ offset 的操作 按照具体 offset，只能操作 队列 级别 按照 最大或则最小 和 时间，可以 是 大集群 或则 全域操作 定义操作域
     */
    public GetClusterInSyncReturnDO getClusterInSync(ClusterEntity clusterEntity, List<ClusterType> syncClusterTypeList) {
        GetSyncObjectHandler getSyncObjectHandler = new GetSyncObjectHandler();
        getSyncObjectHandler.setClusterEntity(clusterEntity);
        getSyncObjectHandler.setSyncClusterTypeList(syncClusterTypeList);

        return getSyncObjectHandler.sync();
    }

    public void getClusterInSyncByEventmesh() {

    }


    class GetSyncObjectHandler {

        @Setter
        private ClusterEntity clusterEntity;

        private ClusterType clusterType;

        @Setter
        private List<ClusterType> syncClusterTypeList;

        private List<ClusterEntity> independenceClusterList = new ArrayList<>();

        private final List<ClusterRelationshipEntity> clusterRelationshipEntityList = new ArrayList<>();

        private List<ClusterEntity> clusterEntityList = new ArrayList<>();

        private final List<ClusterEntity> capClusterList = new ArrayList<>();

        private List<RuntimeEntity> runtimeList;

        private void base(boolean isSync) {
            this.clusterEntity = clusterService.queryClusterById(clusterEntity);
            this.clusterType = clusterEntity.getClusterType();
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType());
            if(clusterFramework.isCAP() && isSync){
                return;
            }
            if (this.clusterType.isRuntime()) {
                this.queryRuntimeByClusterId();
                return;
            }
            this.queryClusterRelationship();
            this.queryClusterByRelationship();
        }

        public ClusterAndRuntimeOfRelationshipDO deploy() {
            this.base(false);
            if(this.clusterType.isRuntime()) {
                ClusterAndRuntimeOfRelationshipDO clusterAndRuntimeOfRelationshipDO = new ClusterAndRuntimeOfRelationshipDO();
                clusterAndRuntimeOfRelationshipDO.setClusterEntityList(List.of(this.clusterEntity));
                clusterAndRuntimeOfRelationshipDO.setRuntimeEntityList(this.runtimeList);
                clusterAndRuntimeOfRelationshipDO.getRuntimeEntityPairList().add(Pair.of(this.clusterEntity, this.runtimeList));
                return clusterAndRuntimeOfRelationshipDO;
            }
            this.queryRuntimeByClusterList();
            // 组织关系
            return this.structureRelationship();
        }


        public GetClusterInSyncReturnDO sync() {
            this.base(true);
            if(this.clusterType.isRuntime()){
                ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterEntity.getClusterType());
                if(clusterFramework.isCAP()){
                    this.independenceClusterList = List.of(this.clusterEntity);
                }
            }else{
                this.filerSyncObject();
                this.queryRuntimeByIndependenceClusterList();
            }
            GetClusterInSyncReturnDO syncReturnDO = new GetClusterInSyncReturnDO();
            syncReturnDO.setClusterEntityList(this.independenceClusterList);
            syncReturnDO.setRuntimeEntityList(this.runtimeList);
            return syncReturnDO;
        }

        private ClusterAndRuntimeOfRelationshipDO structureRelationship() {
            Map<Long, ClusterEntity> clusterEntityMap =
                this.clusterEntityList.stream().collect(Collectors.toMap(ClusterEntity::getId, Function.identity()));
            ClusterAndRuntimeOfRelationshipDO clusterAndRuntimeOfRelationshipDO = new ClusterAndRuntimeOfRelationshipDO();

            Map<Long, List<RuntimeEntity>> runtimeListByClusterIdMap = new HashMap<>();
            Map<Long, List<ClusterEntity>> clusterListByClusterIdMap = new HashMap<>();
            this.clusterRelationshipEntityList.forEach(relation -> {
                ClusterEntity mainClusterEntity = clusterEntityMap.get(relation.getClusterId());
                ClusterEntity relationshipClusterEntity = clusterEntityMap.get(relation.getRelationshipId());

                Triple<ClusterRelationshipEntity, ClusterEntity, ClusterEntity> triple =
                    Triple.of(relation, mainClusterEntity, relationshipClusterEntity);
                clusterAndRuntimeOfRelationshipDO.getClusterRelationshipTripleList().add(triple);

                if (relationshipClusterEntity.getClusterType().isRuntime()) {
                    runtimeListByClusterIdMap.computeIfAbsent(relationshipClusterEntity.getClusterId(), k -> {
                        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
                        clusterAndRuntimeOfRelationshipDO.getRuntimeEntityPairList().add(Pair.of(relationshipClusterEntity, runtimeEntityList));
                        return runtimeEntityList;
                    });
                }
                clusterListByClusterIdMap.computeIfAbsent(mainClusterEntity.getClusterId(), k->{
                    List<ClusterEntity> clusterEntityList = new ArrayList<>();
                    clusterAndRuntimeOfRelationshipDO.getClusterEntityPairleList().add(Pair.of(mainClusterEntity, clusterEntityList));
                    return clusterEntityList;
                });
            });

            this.clusterEntityList.forEach(clusterEntity -> {
                List<ClusterEntity> list = clusterListByClusterIdMap.get(clusterEntity.getId());
                if(Objects.nonNull(list)){
                    list.add(clusterEntity);
                }
            });

            this.runtimeList.forEach(runtimeEntity -> {
                List<RuntimeEntity> list = runtimeListByClusterIdMap.get(runtimeEntity.getId());
                if(Objects.nonNull(list)){
                    list.add(runtimeEntity);
                }
            });
            return clusterAndRuntimeOfRelationshipDO;
        }

        public void getEventSpace() {
                        /*
                查询 eventmesh 集群 里面 eventmesh 相关集群
                storage 有一份，那么 eventmesh 也要有一份。至于 eventmesh 的是否执行，另外说。
                TODO 这里需要有一个任务，对比 eventmesh  与 storage 的相关配置是否一样，不一样就补充
                     这个对比在数据数据的对比，不在 sync 行为进行
                     比如 新增集群，激活集群，集群上线等造成，会造成 eventmesh 与 storage 数据不一样。
                     这样需要把 eventmesh 的数据 与 storage 的数据 进行一次对比，保证一样
            */
            // 分类集群
            List<ClusterEntity> oneClusterList = clusterService.queryRelationClusterByClusterIdAndType(clusterEntity);
            List<ClusterEntity> notDefinitionClusterList = new ArrayList<>(oneClusterList);
            // 查询 eventmesh 里面 存储集群
            List<ClusterEntity> clusterEntityList = clusterService.queryStorageClusterByEventMeshId(clusterEntity);
        }

        public void getDefinition() {
            // 查询是否存
            List<ClusterEntity> reationClusterList = clusterService.queryRelationClusterByClusterIdAndType(clusterEntity);
            List<ClusterEntity> definitionClusterList = new ArrayList<>();
            reationClusterList.forEach(value -> {
                ClusterType storageClusterType = value.getClusterType();
                if (!storageClusterType.isDefinition()) {
                    /*
                    目前来说 definition 的 storage 集群 只有 rocketmq（还有关系型数据库），
                    如果认为 definition 即 主从集群，那么认 definition
                    */
                    definitionClusterList.add(value);
                } else {
                    capClusterList.add(value);
                }
            });
            if (definitionClusterList.isEmpty()) {
                QueryRelationClusterByClusterIdListAndType queryRelationClusterByClusterIdListAndType =
                    new QueryRelationClusterByClusterIdListAndType();
                queryRelationClusterByClusterIdListAndType.setClusterEntityList(definitionClusterList);
                queryRelationClusterByClusterIdListAndType.setClusterTypeList(syncClusterTypeList);
                List<ClusterEntity> definitionQueryRelustClusternList =
                    clusterService.queryRelationClusterByClusterIdListAndType(queryRelationClusterByClusterIdListAndType);
                capClusterList.addAll(definitionQueryRelustClusternList);
            }
        }

        public void getRuntimeByCluster() {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterEntity.getId());
            this.runtimeList = runtimeService.queryRuntimeToFrontByClusterId(runtimeEntity);
        }

        private void filerSyncObject() {
            this.clusterEntityList.forEach(value -> {
                ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType());
                if (clusterFramework.isCAP() || clusterFramework.isCP()) {
                    capClusterList.add(value);
                } else if (clusterFramework.isIndependence()) {
                    independenceClusterList.add(value);
                }
            });
        }

        private void queryRuntimeByClusterId(){
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterEntity.getId());
            this.runtimeList = runtimeService.queryRuntimeToFrontByClusterId(runtimeEntity);
        }

        private void queryRuntimeByIndependenceClusterList() {
            if (!this.independenceClusterList.isEmpty()) {
                this.runtimeList = runtimeService.queryRuntimeToFrontByClusterIdList(this.independenceClusterList);
            }
        }

        private void queryRuntimeByClusterList() {
            if (!this.clusterEntityList.isEmpty()) {
                this.runtimeList = runtimeService.queryRuntimeToFrontByClusterIdList(this.clusterEntityList);
            }
        }

        private void queryClusterByRelationship() {
            List<ClusterEntity> clsuterEnttiyList = this.clusterRelationshipEntityList.stream().map(value -> {
                ClusterEntity entity = new ClusterEntity();
                entity.setId(value.getRelationshipId());
                return entity;
            }).toList();
            this.clusterEntityList = clusterService.queryClusterListByClusterList(clsuterEnttiyList);
        }

        private void queryClusterRelationship() {
            List<Long> clusterId = new ArrayList<>();
            Queue<ClusterRelationshipEntity> clusterRelationshipEntityQueue = new LinkedList<>();
            QueryListByClusterIdAndTypeDO queryListByClusterIdAndTypeDO = new QueryListByClusterIdAndTypeDO();
            queryListByClusterIdAndTypeDO.setClusterId(this.clusterEntity.getId());
            queryListByClusterIdAndTypeDO.setClusterTypeList(this.syncClusterTypeList);
            List<ClusterRelationshipEntity> relationshipEntityList =
                clusterRelationshipService.queryListByClusterIdAndType(queryListByClusterIdAndTypeDO);
            this.clusterRelationshipEntityList.addAll(relationshipEntityList);
            for (; ; ) {
                List<Long> idList = relationshipEntityList.stream().map(ClusterRelationshipEntity::getRelationshipId).toList();
                queryListByClusterIdAndTypeDO.setClusterIdList(idList);
                relationshipEntityList = clusterRelationshipService.queryListByClusterIdListAndType(queryListByClusterIdAndTypeDO);
                if (relationshipEntityList.isEmpty()) {
                    break;
                }
                this.clusterRelationshipEntityList.addAll(relationshipEntityList);
            }
        }

        /**
         * TODO for 循环查询 所有 cluster ，不想 怕有 性能 问题，造成 bug。
         *      queryClusterRelationship 可以代替这个 方法
         */
        private void queryCluster() {
            // 构建第一次查询对象
            QueryRelationClusterByClusterIdListAndType queryRelationClusterByClusterIdListAndType =
                new QueryRelationClusterByClusterIdListAndType();
            queryRelationClusterByClusterIdListAndType.setClusterTypeList(syncClusterTypeList);
            List<ClusterEntity> clusterEntityList = new ArrayList<>();
            queryRelationClusterByClusterIdListAndType.setClusterEntityList(clusterEntityList);
            clusterEntityList.add(clusterEntity);
            /*
                循环查询所有 cluster </p>
                目前循环四次就行了，</p>
                需要去重，应为 有共享集群的存在 </p>
                TODO 如何 识别 目标对象
            */
            for (int i = 0; i < 5; i++) {
                List<ClusterEntity> reationClusterList =
                    clusterService.queryRelationClusterByClusterIdListAndType(queryRelationClusterByClusterIdListAndType);

                List<ClusterEntity> definitionClusterList = new ArrayList<>();
                reationClusterList.forEach(value -> {
                    ClusterType storageClusterType = value.getClusterType();
                    if (!storageClusterType.isDefinition()) {
                    /*
                    目前来说 definition 的 storage 集群 只有 rocketmq（还有关系型数据库），
                    如果认为 definition 即 主从集群，那么认 definition
                    */
                        definitionClusterList.add(value);
                    } else {
                        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType());
                        if (clusterFramework.isCAP() || clusterFramework.isCP()) {
                            capClusterList.add(value);
                        } else if (clusterFramework.isIndependence()) {
                            independenceClusterList.add(value);
                        }
                    }
                });
                if (definitionClusterList.isEmpty()) {
                    return;
                }
                queryRelationClusterByClusterIdListAndType.setClusterEntityList(definitionClusterList);
            }
        }
    }
}