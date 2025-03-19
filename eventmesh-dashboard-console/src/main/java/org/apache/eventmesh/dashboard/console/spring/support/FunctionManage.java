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
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.console.config.FunctionManagerConfigs;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.SyncMetadataManage;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * FunctionManager is in charge of tasks such as scheduled health checks
 */
@Slf4j
@Component
public class FunctionManage {


    private Health2Service healthService = new Health2Service();

    @Autowired
    private DefaultMetadataSyncResultHandler defaultMetadataSyncResultHandler;

    @Autowired
    private SyncMetadataManage syncMetadataManage;


    private MetadataSyncManage metadataSyncManage = new MetadataSyncManage();


    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private ClusterService clusterService;

    private RuntimeEntity runtimeEntity = new RuntimeEntity();

    private ClusterEntity clusterEntity = new ClusterEntity();


    @PostConstruct
    private void init() {
        runtimeEntity.setUpdateTime(LocalDateTime.of(2000, 0, 0, 0, 0, 0, 0));
        Map<MetadataType, Object> map = new HashMap<>();
        map.put(MetadataType.TOPIC, TopicRemotingService.class);



        syncMetadataManage.setMetadataHandlerMap(map);

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

        this.syncCluster();
        this.syncRuntime();
    }

    public void syncRuntime() {
        LocalDateTime date = LocalDateTime.now();
        List<RuntimeEntity> runtimeEntityList = runtimeService.selectByUpdateTime(runtimeEntity);
        runtimeEntity.setUpdateTime(date);
        if (runtimeEntityList.isEmpty()) {
            log.info("not update rutnime");
            return;
        }
        List<RuntimeEntity> firstToWhom = new ArrayList<>();
        runtimeEntityList.forEach(entity -> {
            if (entity.getStatus() == 1) {
                healthService.unRegister(entity);
                return;
            }
            ClusterSyncMetadata clusterSyncMetadata =
                ClusterSyncMetadataEnum.valueOf(ClusterSyncMetadataEnum.class, ClusterType.STORAGE_KAFKA_RAFT.toString()).getClusterSyncMetadata();
            healthService.register(entity);

        });

    }

    private void syncCluster() {

    }

    private void handler(List<BaseEntity> entities) {
        // 闯进来的数据一定是可靠的
        entities.forEach((value) -> {
            ClusterSyncMetadata clusterSyncMetadata =
                ClusterSyncMetadataEnum.valueOf(ClusterSyncMetadataEnum.class, value.getClusterType().toString()).getClusterSyncMetadata();
            if (value instanceof RuntimeEntity) {
                RuntimeEntity runtime = (RuntimeEntity) value;
                if (value.getStatus() == 1) {
                    healthService.unRegister(runtime);
                } else {
                    healthService.register(runtime);
                }
            }
            if (value.getStatus() == 1) {
                List<MetadataSyncConfig> metadataSyncConfigs = syncMetadataManage.createMetadataSyncConfig(value,clusterSyncMetadata);
            } else {
                healthService.register();
            }

        });
    }

}
