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


package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseSyncEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.MetadataSyncResultEntity;
import org.apache.eventmesh.dashboard.console.service.function.MetadataSyncResultService;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultMetadataSyncResultHandler implements MetadataSyncResultHandler {


    private final Map<String, Dimension> dimensionMap = new ConcurrentHashMap<>();

    private final Object recordLock = new Object();

    private List<MetadataSyncResultEntity> record = new ArrayList<>();

    private final Object runtimeLock = new Object();

    private List<RuntimeEntity> runtimeList = new ArrayList<>();

    private final Object clusterLock = new Object();

    private List<ClusterEntity> clusterList = new ArrayList<>();


    @Autowired
    private MetadataSyncResultService metadataSyncResultService;


    /**
     * TODO
     *      1. 如何优化这里的持久。只保存错误记录还是全量报错
     *      2. 保存 FirstToWhom 处理结果
     *      3. 保存 节点异常状态
     */
    @Override
    public void persistence() {
        List<MetadataSyncResultEntity> record = null;
        if (!this.record.isEmpty()) {
            synchronized (recordLock) {
                record = this.record;
                this.record = new ArrayList<>();
            }
        }

        List<RuntimeEntity> runtimeList = null;
        if (!this.runtimeList.isEmpty()) {
            synchronized (runtimeLock) {
                runtimeList = this.runtimeList;
                this.runtimeList = new ArrayList<>();
            }
        }

        List<ClusterEntity> clusterList = null;
        if (!this.clusterList.isEmpty()) {
            synchronized (clusterLock) {
                clusterList = this.clusterList;
                this.clusterList = new ArrayList<>();
            }
        }
        metadataSyncResultService.bachMetadataSyncResult(record, runtimeList, clusterList);
    }


    @Override
    public void register(List<MetadataSyncResult> metadataSyncResults) {
        MetadataSyncResult metadataSyncResult = metadataSyncResults.get(0);
        Dimension dimension = new Dimension();
        dimensionMap.put(metadataSyncResult.getBaseSyncBase().getUnique(), dimension);

        metadataSyncResults.forEach((value) -> {
            dimension.metadataTypeMap.put(value.getMetadataType(), 1);
        });
        dimension.baseSyncBase = metadataSyncResult.getBaseSyncBase();
    }

    @Override
    public void unregister(BaseSyncBase baseSyncBase) {
        dimensionMap.remove(baseSyncBase.getUnique());
    }

    /**
     * 分为正常同步，校验校验 ruguo
     *
     * @param metadataSyncResult
     */
    @Override
    public void handleMetadataSyncResult(MetadataSyncResult metadataSyncResult) {
        Dimension dimension = this.dimensionMap.get(metadataSyncResult.getBaseSyncBase().getUnique());
        if (!metadataSyncResult.isSuccess()) {
            MetadataSyncResultEntity metadataSyncResultEntity = getMetadataSyncResultEntity(metadataSyncResult, dimension);
            synchronized (record) {
                record.add(metadataSyncResultEntity);
            }
            BaseSyncEntity baseSyncEntity = dimension.getBaseSyncEntity();
            baseSyncEntity.setSyncErrorType(metadataSyncResult.getSyncErrorType());

        }
        if (Objects.equals(dimension.baseSyncBase.getFirstToWhom(), FirstToWhom.COMPLETE)) {
            return;
        }
        dimension.metadataTypeMap.remove(metadataSyncResult.getMetadataType());
        if (MapUtils.isEmpty(dimension.metadataTypeMap)) {
            dimension.baseSyncBase.setFirstSyncState(FirstToWhom.COMPLETE);
            BaseSyncEntity baseSyncEntity = dimension.getBaseSyncEntity();
            baseSyncEntity.setFirstSyncState(FirstToWhom.COMPLETE);
        }
    }

    private static MetadataSyncResultEntity getMetadataSyncResultEntity(MetadataSyncResult metadataSyncResult, Dimension dimension) {
        BaseSyncEntity baseSyncEntity = dimension.getBaseSyncEntity();

        MetadataSyncResultEntity metadataSyncResultEntity = new MetadataSyncResultEntity();
        metadataSyncResultEntity.setClusterId(baseSyncEntity.getClusterId());
        metadataSyncResultEntity.setSyncId(baseSyncEntity.getId());
        metadataSyncResultEntity.setMetadataType(metadataSyncResult.getMetadataType());
        metadataSyncResultEntity.setSyncErrorType(metadataSyncResult.getSyncErrorType());
        metadataSyncResultEntity.setResultData(metadataSyncResultEntity.getResultData());
        return metadataSyncResultEntity;
    }


    /**
     *
     */
    public class Dimension {

        /**
         * 如果第一次同步，需要修改对应的， dimension 对象。runtime cluster
         *
         * @see org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom
         * <p>
         * 每次完成同步，进行整理，如果有失败的，进行记录并且对 dimension 队形进行标记
         */
        private BaseSyncBase baseSyncBase;


        private Map<MetadataType, Integer> metadataTypeMap = new ConcurrentHashMap<>();


        public BaseSyncEntity getBaseSyncEntity() {
            return Objects.equals(ClusterMetadata.class, baseSyncBase.getClass()) ? this.getClusterEntity() : this.getRuntimeEntity();
        }

        public RuntimeEntity getRuntimeEntity() {
            RuntimeEntity runtimeEntity = RuntimeConvertMetaData.INSTANCE.toEntity((RuntimeMetadata) baseSyncBase);
            synchronized (DefaultMetadataSyncResultHandler.this.runtimeLock) {
                DefaultMetadataSyncResultHandler.this.runtimeList.add(runtimeEntity);
            }
            return runtimeEntity;
        }

        private ClusterEntity getClusterEntity() {
            ClusterEntity clusterEntity = ClusterConvertMetaData.INSTANCE.toEntity((ClusterMetadata) this.baseSyncBase);
            synchronized (DefaultMetadataSyncResultHandler.this.clusterLock) {
                DefaultMetadataSyncResultHandler.this.clusterList.add(clusterEntity);
            }
            return clusterEntity;
        }

        public boolean isCluster() {
            return Objects.equals(ClusterMetadata.class, baseSyncBase.getClass());
        }
    }
}
