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

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractCollect implements Collect {

    // 采集回调可能在其他线程追加样本，需要看到本轮缓冲。
    private volatile RestoreData current;

    private volatile RestoreData backup;

    private final AtomicBoolean collectionRunning = new AtomicBoolean();

    @Setter
    private CollectMetadata collectMetadata;

    // Share one field with exporters to prevent metadata shadowing.
    @Setter
    protected ClusterMetadata clusterMetadata;

    @Setter
    private RuntimeMetadata runtimeMetadata;

    @Setter
    private DataSyncHandlerWrapper dataSyncHandlerWrapper;

    /**
     * 异步采集器必须在返回前结束写入回调，或原子化本轮关闭与写入，防止迟到样本进入后续轮次。
     */
    protected abstract void doCollect();

    @Override
    public void collect(int index, DataSyncHandlerWrapper dataSyncHandlerWrapper) {
        log.info("collect start index {} , cluster type {}  cluster id {}  runtime id {}", index, clusterMetadata.getClusterType(),
            clusterMetadata.getClusterType(), Objects.isNull(runtimeMetadata) ? "null" : runtimeMetadata.getId());
        // 同一采集器仍在运行时，完成空批次，避免后续轮次占用等待线程。
        if (!this.collectionRunning.compareAndSet(false, true)) {
            log.warn("Skipping overlapping collection for cluster {} and runtime {}", this.clusterMetadata.getId(),
                Objects.isNull(this.runtimeMetadata) ? null : this.runtimeMetadata.getId());
            RestoreData empty = this.createRestoreData();
            empty.setIndex(index);
            dataSyncHandlerWrapper.sync(empty);
            return;
        }
        RestoreData result;
        try {
            this.currentData();
            result = this.current;
            result.setIndex(index);
            try {
                this.doCollect();
            } catch (RuntimeException | Error e) {
                // 采集中断后丢弃未交付的样本，再将缓冲交还给采集器。
                result.restore();
                throw e;
            }
        } finally {
            this.current = null;
            this.collectionRunning.set(false);
        }
        dataSyncHandlerWrapper.sync(result);
        log.info("collect end index {} , cluster type {}  cluster id {}  runtime id {}", index, clusterMetadata.getClusterType(),
            clusterMetadata.getClusterType(), Objects.isNull(runtimeMetadata) ? "null" : runtimeMetadata.getId());
    }

    // 节点离开元数据缓存后，采集任务仍需要以原集群 ID 限定清理范围。
    final boolean belongsToCluster(Long clusterId) {
        return this.clusterMetadata != null && clusterId != null && Objects.equals(this.clusterMetadata.getId(), clusterId);
    }

    // 归属未知不能作为迁移证据，避免清理其他实现注入的任务。
    final boolean belongsToDifferentCluster(Long clusterId) {
        return this.clusterMetadata != null && this.clusterMetadata.getId() != null && clusterId != null
            && !this.belongsToCluster(clusterId);
    }

    public synchronized void restore(RestoreData restoreData) {
        if (this.backup == null) {
            this.backup = restoreData;
        } else {
            log.warn("restore data already exist");
        }
    }

    protected void setData(OrganizationId data) {
        data.setOrganizationId(clusterMetadata.getOrganizationId());
        if (Objects.isNull(data.getTime())) {
            data.setTime(LocalDateTime.now());
        }
        if (data instanceof ClusterId clusterId) {
            clusterId.setClustersId(this.clusterMetadata.getId());
            clusterId.setClustersName(this.clusterMetadata.getName());
        }
        if (Objects.nonNull(this.runtimeMetadata) && data instanceof RuntimeId runtimeId) {
            runtimeId.setRuntimeId(this.runtimeMetadata.getId());
            runtimeId.setRuntimeName(this.runtimeMetadata.getName());
        }
        this.current.setData(data);
    }

    private RestoreData createRestoreData() {
        RestoreData restoreData = new RestoreData();
        restoreData.setAbstractCollect(this);
        restoreData.setCollectMetadata(this.collectMetadata);
        return restoreData;
    }

    @SuppressWarnings("ReplaceNullCheck")
    private synchronized void currentData() {
        if (this.backup != null) {
            this.current = this.backup;
            // 已借出的缓冲在交付完成前不能再次用于其他批次。
            this.backup = null;
        } else {
            this.current = this.createRestoreData();
        }
    }


}
