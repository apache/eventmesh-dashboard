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

import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.MetadataAllDO;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.health.Health2Service;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
    private HealthDataService dataService;

    @Autowired
    private List<DataMetadataHandler<Object>> dataMetadataHandlerList;


    private final MetadataSyncManage metadataSyncManage = new MetadataSyncManage();

    private final Health2Service healthService = new Health2Service();

    private final ClusterMetadataDomain clusterMetadataDomain = new ClusterMetadataDomain();

    private final RuntimeEntity runtimeEntity = new RuntimeEntity();

    private final ClusterEntity clusterEntity = new ClusterEntity();

    private final ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();


    @PostConstruct
    private void init() {
        clusterMetadataDomain.rootClusterDHO();
        this.initQueueData();
        this.createHandler();
        this.buildMetadataSyncManage();
    }

    private void initQueueData() {
        LocalDateTime date = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
        runtimeEntity.setUpdateTime(date);
        clusterEntity.setCreateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);
    }

    private void buildMetadataSyncManage() {
        List<DatabaseAndMetadataMapper> databaseAndMetadataMapperList = new ArrayList<>();
        for (DatabaseAndMetadataType databaseAndMetadataType : DatabaseAndMetadataType.values()) {
            databaseAndMetadataMapperList.add(databaseAndMetadataType.getDatabaseAndMetadataMapper());
        }
        this.metadataSyncManage.setMetadataSyncResultHandler(this.defaultMetadataSyncResultHandler);
        this.metadataSyncManage.setDataMetadataHandlerList(this.dataMetadataHandlerList);

        this.metadataSyncManage.init(50, 100, databaseAndMetadataMapperList);
    }

    /**
     * TODO 核心逻辑在这里
     */
    private void createHandler() {
        DefaultDataHandler defaultDataHandler = new DefaultDataHandler();
        defaultDataHandler.setHealthService(healthService);
        defaultDataHandler.setMetadataSyncManage(metadataSyncManage);
        this.clusterMetadataDomain.setHandler(defaultDataHandler);
    }

    @Bean
    public ClusterMetadataDomain registerBean() {
        return this.clusterMetadataDomain;
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

    @Scheduled(initialDelay = 100L, fixedDelay = 100)
    public void sync() {
        LocalDateTime date = LocalDateTime.now();
        List<RuntimeEntity> runtimeEntityList = this.runtimeService.queryByUpdateTime(runtimeEntity);
        List<ClusterEntity> clusterEntityList = this.clusterService.queryByUpdateTime(clusterEntity);
        List<ClusterRelationshipEntity> clusterRelationshipEntityList =
            this.clusterRelationshipService.queryByUpdateTime(clusterRelationshipEntity);
        if (runtimeEntityList.isEmpty() && clusterEntityList.isEmpty() && clusterRelationshipEntityList.isEmpty()) {
            log.info("No runtime entities found");
        }
        runtimeEntity.setUpdateTime(date);
        clusterEntity.setUpdateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);

        MetadataAllDO metadataAll =
            MetadataAllDO.builder().clusterEntityList(clusterEntityList).clusterRelationshipEntityList(clusterRelationshipEntityList)
                .runtimeEntityList(runtimeEntityList).build();
        this.clusterMetadataDomain.handlerMetadata(metadataAll);

    }


}
