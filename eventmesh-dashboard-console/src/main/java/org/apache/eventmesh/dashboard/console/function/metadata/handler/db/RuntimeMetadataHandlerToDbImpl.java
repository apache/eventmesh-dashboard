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

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;
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

    @Override
    public void addMetadata(RuntimeMetadata meta) {
        ClusterEntity cluster = null;
        if (Objects.isNull(cluster)) {
            log.info("new cluster detected syncing runtime, adding cluster to db, cluster:{}", meta.getClusterName());
            ClusterEntity clusterEntity = new ClusterEntity();
            clusterEntity.setId(0L);
            clusterEntity.setClusterType(ClusterType.EVENTMESH_CLUSTER);
            clusterEntity.setTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP);
            clusterEntity.setName(meta.getClusterName());
            clusterEntity.setVersion("");
            clusterEntity.setJmxProperties("");
            clusterEntity.setDescription("");
            clusterEntity.setAuthType(0);
            clusterEntity.setRunState(0);
            clusterService.insertCluster(clusterEntity);
        } else {
            cluster.setName(meta.getClusterName());
            clusterService.insertCluster(cluster);
        }
        if (Objects.isNull(meta.getClusterId())) {
            //meta.setClusterId(ClusterCache.getINSTANCE().getClusterByName(meta.getClusterName()).getId());
        }
        //runtimeService.addRuntime(new RuntimeEntity(meta));
        //RuntimeCache.getInstance().addRuntime(new RuntimeEntity(meta));

        // 集群存在且不过时。 直接同步就可以。

        // 集群不存在 or 集群存在且过时。那么需要全部读出来，整理

        // 创建 cluster do cache

        //  在一个事务中，从 runtime 同步 元数据

        // 读取 config ， topic ， acl ，user ， group，订阅关系，

        // 同步成功修改 状态，同步成功，修改状态

        //
    }

    @Override
    public void deleteMetadata(RuntimeMetadata meta) {
        //runtimeService.deactivate(new RuntimeEntity(meta));

    }

    @Override
    public List<RuntimeMetadata> getData() {
        return null;
    }

    @Override
    public List<RuntimeMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }
}
