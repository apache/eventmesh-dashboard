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

import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;

/** Kafka 采集复用既有的维度填充，独立管理每轮结果缓冲。 */
public abstract class KafkaCollect extends AbstractCollect {

    private CollectMetadata collectMetadata;

    private RuntimeMetadata runtimeMetadata;

    @Override
    public void setCollectMetadata(CollectMetadata collectMetadata) {
        super.setCollectMetadata(collectMetadata);
        this.collectMetadata = collectMetadata;
    }

    @Override
    public void setRuntimeMetadata(RuntimeMetadata runtimeMetadata) {
        super.setRuntimeMetadata(runtimeMetadata);
        this.runtimeMetadata = runtimeMetadata;
    }

    @Override
    protected void setData(OrganizationId data) {
        if (data instanceof RuntimeId runtime && runtimeMetadata != null && runtimeMetadata.getClusterType() != null) {
            runtime.setRuntimeType(runtimeMetadata.getClusterType().name());
        }
        super.setData(data);
    }

    @Override
    public synchronized void restore(RestoreData restoreData) {
        // Kafka 每轮使用独立结果容器，避免复用仍可能被另一轮持有的缓冲。
    }

    protected final RestoreData createRestoreData() {
        RestoreData data = new RestoreData();
        data.setAbstractCollect(this);
        data.setCollectMetadata(collectMetadata);
        return data;
    }
}
