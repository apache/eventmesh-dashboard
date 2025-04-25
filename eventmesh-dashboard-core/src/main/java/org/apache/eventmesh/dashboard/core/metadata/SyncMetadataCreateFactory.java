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


package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

/**
 * MetadataHandler。 读写行为 每个 runtime or cluster 。所有 MetadataType 的 MetadataHandler 已经关系 从 runtime（Cluster） -> 读 -> createDataMetadataHandler 缓存 -> 定时写
 * 从 db 定时读  发动  -> SyncMetadataCreateFactory -> createDataMetadataHandler 获得 runtime 或则 cluster 维度数据，在写入 runtime 1000个节点，进行一千次db操作， db直接费了。
 */
public class SyncMetadataCreateFactory {

    @Setter
    private DataMetadataHandler<Object> dataMetadataHandler;

    @Setter
    private MetadataType metadataType;

    @Getter
    @Setter
    private ConvertMetaData<Object, BaseClusterIdBase> convertMetaData;


    @Getter
    @Setter
    private DatabaseAndMetadataMapper databaseAndMetadataMapper;

    private final Map<Long, List<BaseClusterIdBase>> runtimeMetadataMap = new ConcurrentHashMap<>();

    private final Map<Long, List<BaseClusterIdBase>> clusterMetadataMap = new ConcurrentHashMap<>();

    private List<Object> addData = new ArrayList<>();

    private List<Object> updateData = new ArrayList<>();

    private List<Object> deleteData = new ArrayList<>();

    public void loadData() {
        // TODO 加载的时候， 目前永远不会加载 Runtime
        if (metadataType.isReadOnly()) {
            return;
        }

        dataMetadataHandler.getData().forEach(data -> {
            BaseRuntimeIdBase baseRuntimeIdBase = (BaseRuntimeIdBase) this.convertMetaData.toMetaData(data);
            ClusterFramework clusterFramework =
                ClusterSyncMetadataEnum.getClusterFramework(baseRuntimeIdBase.getClusterType());
            if (clusterFramework.isCAP()) {
                this.clusterMetadataMap.computeIfAbsent(baseRuntimeIdBase.getClusterId(), (value) -> new ArrayList<>()).add(baseRuntimeIdBase);
            } else {
                this.runtimeMetadataMap.computeIfAbsent(baseRuntimeIdBase.getRuntimeId(), (value) -> new ArrayList<>()).add(baseRuntimeIdBase);
            }
        });

    }

    public void persistence() {
        if (this.deleteData.isEmpty() && this.updateData.isEmpty() && this.clusterMetadataMap.isEmpty()) {
            return;
        }
        this.dataMetadataHandler.handleAll(this.addData, this.updateData, this.deleteData);
        this.addData = new ArrayList<>();
        this.updateData = new ArrayList<>();
        this.deleteData = new ArrayList<>();
    }

    public MetadataHandler<BaseClusterIdBase> createDataMetadataHandler(BaseSyncBase baseSyncBase) {
        Map<Long, List<BaseClusterIdBase>> mapetadataMap =
            Objects.equals(baseSyncBase.getClass(), RuntimeMetadata.class) ? this.runtimeMetadataMap : this.clusterMetadataMap;

        return new MetadataHandler<BaseClusterIdBase>() {
            @Override
            public void addMetadata(BaseClusterIdBase meta) {
                SyncMetadataCreateFactory.this.addData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void deleteMetadata(BaseClusterIdBase meta) {
                SyncMetadataCreateFactory.this.deleteData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void updateMetadata(BaseClusterIdBase meta) {
                SyncMetadataCreateFactory.this.updateData.add(meta);
            }

            @Override
            public void handleAll(List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData, List<BaseClusterIdBase> deleteData) {
                this.addAll(SyncMetadataCreateFactory.this.addData, addData);
                this.addAll(SyncMetadataCreateFactory.this.updateData, updateData);
                this.addAll(SyncMetadataCreateFactory.this.deleteData, deleteData);
            }

            private void addAll(List<Object> persistenceData, List<BaseClusterIdBase> handleData) {
                handleData.forEach(data -> {
                    persistenceData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(data));
                });
            }

            @Override
            public List<BaseClusterIdBase> getData() {
                return mapetadataMap.remove(baseSyncBase.getId());
            }

        };
    }
}
