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

package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.controller.deploy.base.AbstractUpdateHandler;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.deploy.create.CreateClusterByCopyDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CreateClusterByCopyHandler extends AbstractUpdateHandler implements UpdateHandler<CreateClusterByCopyDTO> {

    @Override
    public void init() {

    }

    @Override
    public void handler(CreateClusterByCopyDTO dto) {

        ClusterEntity clusterEntity = new ClusterEntity();
        ClusterAndRuntimeOfRelationshipDO data =
            this.clusterAndRuntimeDomain.getAllClusterAndRuntimeByCluster(clusterEntity, DeployStatusType.CREATE_COPY);

        // 如果是 runtime 类型，那么只需要一个创建一个集群
        // 一个集群只会在一个 kubernetes 集群里面创建
        if (data.getClusterEntity().getClusterType().isRuntime()) {
            this.kubernetesManage.checkResource(data.getClusterResourcesList().get(0).getRight(), dto.getKubernetesClusterId());
        } else {
            data.getClusterResourcesList().forEach(pair -> {
                this.kubernetesManage.checkResource(pair.getRight(), dto.getKubernetesClusterId());
            });
        }
        // Topic
        data.setTopicMap(new HashMap<>());
        data.getTopicMap().put(MetadataType.CLUSTER, new HashMap<>());
        data.getTopicMap().put(MetadataType.RUNTIME, new HashMap<>());
        Map<MetadataType, Map<Long, List<TopicEntity>>> topicMap = data.getTopicMap();

        List<TopicEntity> topciEntityList = this.topicService.queryByClusterIdList(data.getClusterEntityList());
        topciEntityList.forEach(topicEntity -> {
            boolean isRuntime = topicEntity.getRuntimeId() == 0;
            data.getTopicMap().get(isRuntime ? MetadataType.RUNTIME : MetadataType.CLUSTER)
                .computeIfAbsent(isRuntime ? topicEntity.getRuntimeId() : topicEntity.getClusterId(), k -> new ArrayList<>()).add(topicEntity);
        });
        // config
        data.setConfigMap(new HashMap<>());
        Arrays.stream(MetadataType.values()).toList().forEach(metadataType -> {
            data.getTopicMap().put(metadataType, new HashMap<>());
        });

        List<ConfigEntity> configEntityList = this.configService.queryByClusterIdList(data.getClusterEntityList());

        configEntityList.forEach(configEntity -> {
            data.getConfigMap().get(configEntity.getInstanceType()).computeIfAbsent(configEntity.getInstanceId(), k -> new ArrayList<>())
                .add(configEntity);
        });

        this.deployService.createDeploy(data);

    }
}
