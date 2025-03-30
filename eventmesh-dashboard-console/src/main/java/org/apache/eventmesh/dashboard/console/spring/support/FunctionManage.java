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

package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain.DataHandler;
import org.apache.eventmesh.dashboard.console.domain.metadata.HandlerMetadataDO;
import org.apache.eventmesh.dashboard.console.domain.metadata.MetadataAllDO;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.SyncMetadataManage;
import org.apache.eventmesh.dashboard.core.cluster.ClusterBaseDO;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeDO;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * FunctionManager is in charge of tasks such as scheduled health checks
 */
@Slf4j
@Component
public class FunctionManage {


    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterRelationshipService clusterRelationshipService;


    @Autowired
    private DefaultMetadataSyncResultHandler defaultMetadataSyncResultHandler;

    @Autowired
    private SyncMetadataManage syncMetadataManage;


    private final MetadataSyncManage metadataSyncManage = new MetadataSyncManage();

    private final Health2Service healthService = new Health2Service();

    private final ClusterMetadataDomain clusterMetadataDomain = new ClusterMetadataDomain();

    private final RuntimeEntity runtimeEntity = new RuntimeEntity();

    private final ClusterEntity clusterEntity = new ClusterEntity();

    private final ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();


    @PostConstruct
    private void init() {
        clusterMetadataDomain.rootClusterDHO();
        LocalDateTime date = LocalDateTime.of(2000, 0, 0, 0, 0, 0, 0);
        runtimeEntity.setUpdateTime(date);
        clusterEntity.setCreateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);
        this.createHandler();
    }


    private void createHandler() {
        this.clusterMetadataDomain.setHandler(new DataHandler<RuntimeDO, ClusterDO>() {

            @Override
            public void registerRuntime(RuntimeEntity runtimeEntity, RuntimeDO runtimeDO, ColonyDO<ClusterDO> colonyDO) {
                ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(runtimeEntity.getClusterType());
                if (clusterSyncMetadata.getClusterFramework().isCAP()) {
                    ClusterBaseDO clusterDO = colonyDO.getClusterDO();
                    SDKManage.getInstance()
                        .createClient(SDKTypeEnum.ADMIN, (BaseSyncBase) clusterDO.getClusterInfo(), clusterDO.getMultiCreateSDKConfig(),
                            runtimeEntity.getClusterType());
                    return;
                }
                SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtimeDO.getRuntimeMetadata(), runtimeDO.getCreateSDKConfig(),
                    runtimeDO.getRuntimeMetadata().getClusterType());
                healthService.register(runtimeDO.getRuntimeMetadata());
                metadataSyncManage.register(runtimeDO.getRuntimeMetadata());
            }

            @Override
            public void unRegisterRuntime(RuntimeEntity runtimeEntity, RuntimeDO runtimeDO, ColonyDO<ClusterDO> colonyDO) {
                ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(runtimeEntity.getClusterType());

                if (clusterSyncMetadata.getClusterFramework().isCAP()) {
                    ClusterBaseDO clusterDO = colonyDO.getClusterDO();
                    SDKManage.getInstance()
                        .createClient(SDKTypeEnum.ADMIN, (BaseSyncBase) clusterDO.getClusterInfo(), clusterDO.getMultiCreateSDKConfig(),
                            runtimeEntity.getClusterType());
                    return;
                }
                healthService.unRegister(runtimeDO.getRuntimeMetadata());
            }

            @Override
            public void registerCluster(ClusterEntity clusterEntity, ClusterDO clusterDO, ColonyDO<ClusterDO> colonyDO) {
                ClusterSyncMetadata clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(runtimeEntity.getClusterType());
                if (!clusterSyncMetadata.getClusterFramework().isCAP()) {
                    return;
                }
                SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, clusterDO.getClusterInfo(), clusterDO.getMultiCreateSDKConfig(),
                    clusterDO.getClusterInfo().getClusterType());
                healthService.register(clusterDO.getClusterInfo());
                metadataSyncManage.register(clusterDO.getClusterInfo());
            }

            @Override
            public void unRegisterCluster(ClusterEntity clusterEntity, ClusterDO clusterDO, ColonyDO<ClusterDO> colonyDO) {
                healthService.unRegisterCluster(clusterEntity.getClusterId());

            }
        });
    }

    /**
     * meta 同步 只获得 实例 实例 同步 kubernetes 数据 全量读取数据 ，创建对应的 meta 对象
     * <p>
     * 然后 然后日常
     */
    @Scheduled(fixedRate = 5000)
    public void initFunctionManager() {
        healthService.executeAll();
    }

    @Scheduled(fixedRate = 5000)
    public void sync() {
        LocalDateTime date = LocalDateTime.now();
        final List<RuntimeEntity> runtimeEntityList = this.runtimeService.selectByUpdateTime(runtimeEntity);
        final List<ClusterEntity> clusterEntityList = this.clusterService.selectByUpdateTime(clusterEntity);
        final List<ClusterRelationshipEntity> clusterRelationshipEntityList =
            this.clusterRelationshipService.selectByUpdateTime(clusterRelationshipEntity);

        runtimeEntity.setUpdateTime(date);
        clusterEntity.setUpdateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);

        MetadataAllDO metadataAll =
            MetadataAllDO.builder().clusterEntityList(clusterEntityList).clusterRelationshipEntityList(clusterRelationshipEntityList)
                .runtimeEntityList(runtimeEntityList).build();
        HandlerMetadataDO handlerMetadataDO = this.clusterMetadataDomain.handlerMetadata(metadataAll);

    }


}
