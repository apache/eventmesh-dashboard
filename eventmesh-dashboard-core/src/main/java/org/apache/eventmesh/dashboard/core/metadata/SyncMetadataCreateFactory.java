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
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * MetadataHandler。 读写行为 每个 runtime or cluster 。所有 MetadataType 的 MetadataHandler 已经关系 从 runtime（Cluster） -> 读 -> createDataMetadataHandler 缓存 -> 定时写
 * 从 db 定时读  发动  -> SyncMetadataCreateFactory -> createDataMetadataHandler 获得 runtime 或则 cluster 维度数据，在写入 runtime 1000个节点，进行一千次db操作， db直接费了。
 */
@Slf4j
public class SyncMetadataCreateFactory {

    @Setter
    private DataMetadataHandler<Object> dataMetadataHandler;

    @Setter
    @Getter
    private MetadataType metadataType;

    @Getter
    @Setter
    private ConvertMetaData<Object, BaseClusterIdBase> convertMetaData;


    @Getter
    @Setter
    private DatabaseAndMetadataMapper databaseAndMetadataMapper;

    @Setter
    private ColonyDO<ClusterDO> colonyDO;

    /**
     * 初次加载量非常大，设置一个一个变量识别初次加载
     */
    private boolean printIntiGetData = false;

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
        /*
          TODO  bug 如何全量，就会缓存大量的 之前的 无效数据。
                第一次查询需要过滤果无效 cluster 与 runtime 的数据
         */
        List<Object> dataList = dataMetadataHandler.getData();
        if (log.isTraceEnabled()) {
            log.trace("$sync metadata type {} load data: {}",dataMetadataHandler.getClass().getSimpleName(), dataList.size());
        }
        dataList.forEach(data -> {
            BaseRuntimeIdBase baseRuntimeIdBase = (BaseRuntimeIdBase) this.convertMetaData.toMetaData(data);
            ClusterType clusterType = baseRuntimeIdBase.getClusterType();
            if (Objects.isNull(clusterType)) {
                ColonyDO<ClusterDO> colony = this.colonyDO.getAllColonyDO().get(baseRuntimeIdBase.getClusterId());
                if (Objects.isNull(colony)) {
                    log.info("cluster type is null, data is {}", data);
                    return;
                }
                clusterType = colony.getClusterType();
            }
            ClusterFramework clusterFramework =
                ClusterSyncMetadataEnum.getClusterFramework(clusterType);
            if (this.printIntiGetData) {
                this.printIntiGetData = false;

            } else {
                log.info("$sync from database load , cluster is {} runtime is {} u is {}",
                    baseRuntimeIdBase.getClusterId(), baseRuntimeIdBase.getRuntimeId(), baseRuntimeIdBase.getId());
            }
            if (clusterFramework.isCAP()) {
                this.clusterMetadataMap.computeIfAbsent(baseRuntimeIdBase.getClusterId(), (value) -> new ArrayList<>()).add(baseRuntimeIdBase);
            } else {
                this.runtimeMetadataMap.computeIfAbsent(baseRuntimeIdBase.getRuntimeId(), (value) -> new ArrayList<>()).add(baseRuntimeIdBase);
            }
        });

    }

    public void persistence() {
        if (this.deleteData.isEmpty() && this.updateData.isEmpty() && this.clusterMetadataMap.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("#sync persistence {} metadata is empty", metadataType);
            }
            return;
        }

        List<Object> addData, updateData, deleteData;
        synchronized (this) {
            addData = this.addData;
            updateData = this.updateData;
            deleteData = this.deleteData;
            this.addData = new ArrayList<>();
            this.updateData = new ArrayList<>();
            this.deleteData = new ArrayList<>();
        }
        this.dataMetadataHandler.handleAll(null, addData, updateData, deleteData);
    }

    /**
     * 需要记录 这里可以记录id， 第一次全量加载，之后查询创建的 id 的
     */
    public MetadataHandler<BaseClusterIdBase> createDataMetadataHandler(BaseSyncBase baseSyncBase) {
        Map<Long, List<BaseClusterIdBase>> mapetadataMap =
            Objects.equals(baseSyncBase.getClass(), RuntimeMetadata.class) ? this.runtimeMetadataMap : this.clusterMetadataMap;
        Global2Request global2Request = new Global2Request();
        if (Objects.equals(baseSyncBase.getClass(), RuntimeMetadata.class)) {
            global2Request.setRuntimeId(baseSyncBase.getId());
        } else {
            global2Request.setClusterId(baseSyncBase.getClusterId());
        }

        return new MetadataHandler<>() {

            private final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

            private final long timeout = System.currentTimeMillis() + 5000;

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
            public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                List<BaseClusterIdBase> deleteData) {
                this.addAll(SyncMetadataCreateFactory.this.addData, addData);
                this.addAll(SyncMetadataCreateFactory.this.updateData, updateData);
                this.addAll(SyncMetadataCreateFactory.this.deleteData, deleteData);
            }

            private void addAll(List<Object> persistenceData, List<BaseClusterIdBase> handleData) {
                List<Object> newHandleData = handleData.stream().map(SyncMetadataCreateFactory.this.convertMetaData::toEntity).toList();
                synchronized (SyncMetadataCreateFactory.this) {
                    persistenceData.addAll(newHandleData);
                }
            }

            /**
             * 这里 如果 baseSyncBase 第一次调用次方法，如何没有 没有数据可以直接通过 id加载 </p>
             * 防止有延迟，第一次没有数据。可以延迟 10次 或则 5秒
             */
            @SuppressWarnings("unchecked")
            @Override
            public List<BaseClusterIdBase> getData() {
                List<BaseClusterIdBase> baseClusterIdBases = mapetadataMap.remove(baseSyncBase.getId());
                if (CollectionUtils.isEmpty(baseClusterIdBases) && atomicBoolean.get() && System.currentTimeMillis() < timeout) {
                    baseClusterIdBases = (List<BaseClusterIdBase>) ((List) dataMetadataHandler.getData(global2Request));
                    if (CollectionUtils.isEmpty(baseClusterIdBases)) {
                        atomicBoolean.set(true);
                    }
                }
                return baseClusterIdBases;
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<BaseClusterIdBase> getData(Global2Request request) {
                return (List<BaseClusterIdBase>) ((List) dataMetadataHandler.getData(global2Request));
            }

        };
    }
}
