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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.cache.ClusterCache;
import org.apache.eventmesh.dashboard.console.cache.RuntimeCache;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.handler.MetadataHandler;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata.RuntimeMetadata2EntityConverter;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.runtime.RuntimeService;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RuntimeMetadataHandlerToDbImpl implements MetadataHandler<RuntimeMetadata> {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    ClusterService clusterService;

    private static final Converter<RuntimeMetadata, RuntimeEntity> converter = new RuntimeMetadata2EntityConverter();

    @Override
    public void addMetadata(RuntimeMetadata meta) {
        ClusterEntity cluster = ClusterCache.getINSTANCE().getClusterByRegistryAddress(meta.getClusterRegistryAddress());
        if (Objects.isNull(cluster)) {
            log.info("new cluster detected syncing runtime, adding cluster to db, cluster:{}", meta.getClusterName());
            ClusterEntity clusterEntity = new ClusterEntity();
            clusterEntity.setId(0L);
            clusterEntity.setName(meta.getClusterName());
            clusterEntity.setRegistryAddress(meta.getRegistryAddress());
            clusterEntity.setBootstrapServers("");
            clusterEntity.setEventmeshVersion("");
            clusterEntity.setClientProperties("");
            clusterEntity.setJmxProperties("");
            clusterEntity.setRegProperties("");
            clusterEntity.setDescription("");
            clusterEntity.setAuthType(0);
            clusterEntity.setRunState(0);
            clusterEntity.setStoreType(0);
            
            clusterService.addCluster(clusterEntity);
        } else {
            cluster.setName(meta.getClusterName());
            clusterService.addCluster(cluster);
        }
        if (Objects.isNull(meta.getClusterId())) {
            meta.setClusterId(ClusterCache.getINSTANCE().getClusterByName(meta.getClusterName()).getId());
        }
        runtimeService.addRuntime(converter.convert(meta));
        RuntimeCache.getInstance().addRuntime(converter.convert(meta));
    }

    @Override
    public void deleteMetadata(RuntimeMetadata meta) {
        runtimeService.deactivate(converter.convert(meta));
        RuntimeCache.getInstance().deleteRuntime(converter.convert(meta));
    }
}
