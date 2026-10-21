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

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 *      MetadataHandler。 读写行为 每个 runtime or cluster 。
 *      所有 MetadataType 的 MetadataHandler 已经关系 从 runtime（Cluster）
 *          -> 读
 *          -> createDataMetadataHandler 缓存
 *          -> 定时写
 *      从 db 定时读  发动
 *          -> SyncMetadataCreateFactory
 *          -> createDataMetadataHandler 获得 runtime 或则 cluster 维度数据，
 *              在写入 runtime 1000个节点，进行一千次db操作， db直接费了。
 * </pre>
 */
@Slf4j
public class SyncMetadataCreateFactory {

    private final Map<Long, Map<String, BaseClusterIdBase>> runtimeMetadataMap = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, BaseClusterIdBase>> clusterMetadataMap = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, BaseClusterIdBase>> appliedRuntimeMetadata = new HashMap<>();
    private final Map<Long, Map<String, BaseClusterIdBase>> appliedClusterMetadata = new HashMap<>();
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
    private List<Object> addData = new ArrayList<>();

    private List<Object> updateData = new ArrayList<>();

    private List<Object> deleteData = new ArrayList<>();


    @Setter
    private SnapshotMetadataHandler<BaseClusterIdBase> snapshotMetadataHandler;

    public synchronized void loadData() {
        if (Objects.nonNull(this.snapshotMetadataHandler)) {
            List<BaseClusterIdBase> snapshot = new ArrayList<>();
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (Object entity : this.dataMetadataHandler.getData()) {
                BaseClusterIdBase metadata = this.convertMetaData.toMetaData(entity);
                if (Objects.isNull(metadata) || Objects.isNull(metadata.getId()) || !ids.add(metadata.getId())) {
                    throw new IllegalArgumentException("Snapshot rows require distinct persisted IDs");
                }
                snapshot.add(metadata);
            }
            this.snapshotMetadataHandler.synchronize(snapshot, this.colonyDO);
            return;
        }
        // TODO 加载的时候， 目前永远不会加载 Runtime
        if (metadataType.isReadOnly()) {
            return;
        }
        /*
          TODO  bug 如何全量，就会缓存大量的 之前的 无效数据。
                第一次查询需要过滤果无效 cluster 与 runtime 的数据
         */
        try {
            Map<Long, Map<String, BaseClusterIdBase>> clusters = new HashMap<>();
            Map<Long, Map<String, BaseClusterIdBase>> runtimes = new HashMap<>();
            for (Object data : this.dataMetadataHandler.getData()) {
                BaseClusterIdBase metadata = Objects.requireNonNull(this.convertMetaData.toMetaData(data), "Converted metadata");
                final String identity = metadata.getUnique();
                if (!metadata.isDelete() && (Objects.isNull(metadata.getStatus())
                    || Objects.isNull(metadata.getCreateTime()) || Objects.isNull(metadata.getUpdateTime()))) {
                    throw new IllegalArgumentException("Live metadata requires status and timestamps");
                }
                ColonyDO<ClusterDO> colony = this.colonyDO.getAllColonyDO().get(
                    Objects.requireNonNull(metadata.getClusterId(), "Metadata cluster id"));
                if (Objects.isNull(colony)) {
                    throw new IllegalStateException("Metadata cluster is not loaded: " + metadata.getClusterId());
                }
                ClusterType clusterType = metadata.getClusterType();
                if (Objects.isNull(clusterType)) {
                    clusterType = colony.getClusterType();
                    metadata.setClusterType(clusterType);
                }
                if (!Objects.equals(clusterType, colony.getClusterType())) {
                    throw new IllegalArgumentException("Metadata type does not match its target cluster");
                }
                ClusterFramework framework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
                Long target;
                Map<Long, Map<String, BaseClusterIdBase>> destination;
                if (framework.isCAP()) {
                    target = metadata.getClusterId();
                    destination = clusters;
                } else {
                    target = ((BaseRuntimeIdBase) metadata).getRuntimeId();
                    if (Objects.isNull(target) || !colony.getClusterDO().getRuntimeMap().containsKey(target)) {
                        throw new IllegalStateException("Metadata runtime is not loaded: " + target);
                    }
                    destination = runtimes;
                }
                destination.computeIfAbsent(target, key -> new LinkedHashMap<>()).put(identity, metadata);
            }
            this.enqueue(clusters, this.clusterMetadataMap, this.appliedClusterMetadata);
            this.enqueue(runtimes, this.runtimeMetadataMap, this.appliedRuntimeMetadata);
            this.dataMetadataHandler.acknowledgeData();
        } catch (RuntimeException exception) {
            this.dataMetadataHandler.retryData();
            throw exception;
        }
    }

    private void enqueue(Map<Long, Map<String, BaseClusterIdBase>> batches,
        Map<Long, Map<String, BaseClusterIdBase>> pending, Map<Long, Map<String, BaseClusterIdBase>> applied) {
        batches.forEach((target, batch) -> batch.forEach((key, value) -> {
            Map<String, BaseClusterIdBase> queue = pending.computeIfAbsent(target, id -> new LinkedHashMap<>());
            BaseClusterIdBase queued = queue.get(key);
            // Preserve the same version object while an RPC is in flight, including partial-batch retries.
            if (Objects.nonNull(queued) && this.sameDesiredState(queued, value)) {
                return;
            }
            BaseClusterIdBase previous = applied.getOrDefault(target, Map.of()).get(key);
            if (Objects.nonNull(queued) || !this.sameDesiredState(previous, value)) {
                queue.put(key, value);
            }
        }));
    }

    private boolean sameDesiredState(BaseClusterIdBase first, BaseClusterIdBase second) {
        // Same-second incremental overlap and sync-result audit updates are not new desired state.
        return EqualsBuilder.reflectionEquals(first, second, "syncStatus", "updateTime", "updateUserId");
    }

    public synchronized void forgetTarget(BaseSyncBase target) {
        if (target instanceof RuntimeMetadata) {
            this.runtimeMetadataMap.remove(target.getId());
            this.appliedRuntimeMetadata.remove(target.getId());
        } else {
            this.clusterMetadataMap.remove(target.getId());
            this.appliedClusterMetadata.remove(target.getId());
        }
    }

    public synchronized void persistence() {
        if (this.addData.isEmpty() && this.updateData.isEmpty() && this.deleteData.isEmpty()) {
            return;
        }
        // Keep the queued writes if persistence fails; the next round retries them.
        this.dataMetadataHandler.handleAll(null, List.copyOf(this.addData), List.copyOf(this.updateData), List.copyOf(this.deleteData));
        this.addData.clear();
        this.updateData.clear();
        this.deleteData.clear();
    }

    /**
     * 需要记录 这里可以记录id， 第一次全量加载，之后查询创建的 id 的
     */
    public MetadataHandler<BaseClusterIdBase> createDataMetadataHandler(BaseSyncBase baseSyncBase) {
        Map<Long, Map<String, BaseClusterIdBase>> mapetadataMap =
            Objects.equals(baseSyncBase.getClass(), RuntimeMetadata.class) ? this.runtimeMetadataMap : this.clusterMetadataMap;
        Map<Long, Map<String, BaseClusterIdBase>> appliedMap = baseSyncBase instanceof RuntimeMetadata
            ? this.appliedRuntimeMetadata : this.appliedClusterMetadata;
        Global2Request global2Request = new Global2Request();
        if (Objects.equals(baseSyncBase.getClass(), RuntimeMetadata.class)) {
            global2Request.setRuntimeId(baseSyncBase.getId());
        } else {
            global2Request.setClusterId(baseSyncBase.getId());
        }

        return new MetadataHandler<>() {

            private Map<String, BaseClusterIdBase> inFlight;

            @Override
            public void addMetadata(BaseClusterIdBase meta) {
                this.handleAll(null, List.of(meta), List.of(), List.of());
            }

            @Override
            public void deleteMetadata(BaseClusterIdBase meta) {
                this.handleAll(null, List.of(), List.of(), List.of(meta));
            }

            @Override
            public void updateMetadata(BaseClusterIdBase meta) {
                this.handleAll(null, List.of(), List.of(meta), List.of());
            }

            @Override
            public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                List<BaseClusterIdBase> deleteData) {
                List<Object> adds = addData.stream().map(convertMetaData::toEntity).toList();
                List<Object> updates = updateData.stream().map(convertMetaData::toEntity).toList();
                List<Object> deletes = deleteData.stream().map(convertMetaData::toEntity).toList();
                synchronized (SyncMetadataCreateFactory.this) {
                    SyncMetadataCreateFactory.this.addData.addAll(adds);
                    SyncMetadataCreateFactory.this.updateData.addAll(updates);
                    SyncMetadataCreateFactory.this.deleteData.addAll(deletes);
                }
            }

            /** Capture a batch without draining it; acknowledgement removes only these versions. */
            @Override
            public List<BaseClusterIdBase> getData() {
                synchronized (SyncMetadataCreateFactory.this) {
                    if (Objects.isNull(this.inFlight)) {
                        this.inFlight = new LinkedHashMap<>(mapetadataMap.getOrDefault(baseSyncBase.getId(), Map.of()));
                    }
                    return new ArrayList<>(this.inFlight.values());
                }
            }

            @Override
            public List<BaseClusterIdBase> getData(Global2Request request) {
                return dataMetadataHandler.getData(global2Request).stream().map(convertMetaData::toMetaData).toList();
            }

            @Override
            public void acknowledgeData() {
                synchronized (SyncMetadataCreateFactory.this) {
                    Map<String, BaseClusterIdBase> pending = mapetadataMap.get(baseSyncBase.getId());
                    if (Objects.nonNull(pending) && Objects.nonNull(this.inFlight)) {
                        appliedMap.computeIfAbsent(baseSyncBase.getId(), id -> new LinkedHashMap<>()).putAll(this.inFlight);
                        this.inFlight.forEach((key, value) -> {
                            // A newer load must not be acknowledged by an older RPC batch.
                            if (pending.get(key) == value) {
                                pending.remove(key);
                            }
                        });
                        if (pending.isEmpty()) {
                            mapetadataMap.remove(baseSyncBase.getId());
                        }
                    }
                    this.inFlight = null;
                }
            }

            @Override
            public void retryData() {
                synchronized (SyncMetadataCreateFactory.this) {
                    this.inFlight = null;
                }
            }
        };
    }
}
