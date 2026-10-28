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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractCollect implements Collect {

    private RestoreData current;

    private volatile RestoreData backup;

    @Setter
    private CollectMetadata collectMetadata;

    @Setter
    private ClusterMetadata clusterMetadata;

    @Setter
    private RuntimeMetadata runtimeMetadata;

    @Setter
    private DataSyncHandlerWrapper dataSyncHandlerWrapper;

    protected abstract void doCollect();

    @Override
    public void collect(int index, DataSyncHandlerWrapper dataSyncHandlerWrapper) {
        log.info("collect start index {} , cluster type {}  cluster id {}  runtime id {}", index, clusterMetadata.getClusterType(),
            clusterMetadata.getClusterType(), Objects.isNull(runtimeMetadata) ? "null" : runtimeMetadata.getId());
        this.currentData();
        this.current.setIndex(index);
        this.doCollect();
        dataSyncHandlerWrapper.sync(this.current);
        this.current = null;
        log.info("collect end index {} , cluster type {}  cluster id {}  runtime id {}", index, clusterMetadata.getClusterType(),
            clusterMetadata.getClusterType(), Objects.isNull(runtimeMetadata) ? "null" : runtimeMetadata.getId());
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
        } else {
            this.current = this.createRestoreData();
        }
    }


}
