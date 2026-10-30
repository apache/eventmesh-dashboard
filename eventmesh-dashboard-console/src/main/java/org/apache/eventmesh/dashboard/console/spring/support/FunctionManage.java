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
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressManage;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DatabaseAndMetadataType;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.DefaultMetadataSyncResultHandler;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * FunctionManager is in charge of tasks such as scheduled health checks
 */
@Slf4j
@Component
public class FunctionManage {

    private final MetadataSyncManage metadataSyncManage = new MetadataSyncManage();
    private final Health2Service healthService = new Health2Service();
    private final ClusterMetadataDomain clusterMetadataDomain = new ClusterMetadataDomain();
    /**
     * 定时任务查询对象
     */
    private final RuntimeEntity runtimeEntity = new RuntimeEntity();
    /**
     * 定时任务查询对象
     */
    private final ClusterEntity clusterEntity = new ClusterEntity();
    /**
     * 定时任务查询对象
     */
    private final ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
    @Autowired
    private FunctionConfig functionConfig;
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
    @SuppressWarnings("rawtypes")
    @Autowired
    private List<DataMetadataHandler> dataMetadataHandlerList;

    @Autowired
    private DBRemotingResultHook dbRemotingResultHook;

    @Autowired(required = false)
    private DruidDataSource dataSource;

    @Value("${function.enabled:false}")
    private boolean enabled;

    {
        this.initQueueData();
    }

    @Bean
    public ReportHandlerManage buildReportHandlerManage() {
        ReportHandlerManage reportHandlerManage = new ReportHandlerManage();
        reportHandlerManage.setReportConfig(functionConfig.getReportConfig());
        reportHandlerManage.setEnable(functionConfig.isEnabledReport());
        // 采集任务共用元数据同步；原同步入口需要应用数据源才能读取集群配置。
        reportHandlerManage.setDataSource(dataSource);
        reportHandlerManage.init();
        return reportHandlerManage;
    }

    @Bean
    public AddressManage buildAddressManage() {
        return new AddressManage();
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
    @Scheduled(initialDelayString = "${function.health.initialDelay}", fixedDelayString = "${function.health.fixedDelay}")
    public void health() {
        if (!this.functionConfig.isEnabledHealth()) {
            return;
        }
        healthService.executeAll();

    }

    /**
     * 需要一个日志打印管理模块
     */
    @Scheduled(initialDelayString = "${function.sync.initialDelay}", fixedDelayString = "${function.sync.fixedDelay}")
    public void sync() {
        if (!this.functionConfig.isEnabledSync()) {
            return;
        }
        LocalDateTime date = LocalDateTime.now();
        List<RuntimeEntity> runtimeEntityList = this.runtimeService.queryByUpdateTime(runtimeEntity);
        List<ClusterEntity> clusterEntityList = this.clusterService.queryByUpdateTime(clusterEntity);
        List<ClusterRelationshipEntity> clusterRelationshipEntityList =
            this.clusterRelationshipService.queryByUpdateTime(clusterRelationshipEntity);
        if (runtimeEntityList.isEmpty() && clusterEntityList.isEmpty() && clusterRelationshipEntityList.isEmpty()) {
            log.debug("No runtime entities found");
            return;
        }

        MetadataAllDO metadataAll =
            MetadataAllDO.builder().clusterEntityList(clusterEntityList).clusterRelationshipEntityList(clusterRelationshipEntityList)
                .runtimeEntityList(runtimeEntityList).build();
        this.clusterMetadataDomain.handlerMetadata(metadataAll);
        runtimeEntity.setUpdateTime(date);
        clusterEntity.setUpdateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);

    }

    @PostConstruct
    private void init() {
        if (!this.enabled) {
            return;
        }
        Remoting2Manage.getInstance().registerHook(dbRemotingResultHook);
        this.clusterMetadataDomain.rootClusterDHO();
        this.createHandler();
        this.buildMetadataSyncManage();

        healthService.setDataService(dataService);
        if (this.functionConfig.isMockJvm()) {
            MockJvmRemotingServiceHandler mockJvmRemotingServiceHandler = new MockJvmRemotingServiceHandler();
            mockJvmRemotingServiceHandler.setSyncMetadataCreateFactoryMap(metadataSyncManage.getSyncMetadataCreateFactoryMap());
            Remoting2Manage.getInstance().registerServiceHandler(mockJvmRemotingServiceHandler);
        }
    }

    private void initQueueData() {
        LocalDateTime date = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
        runtimeEntity.setUpdateTime(date);
        clusterEntity.setUpdateTime(date);
        clusterRelationshipEntity.setUpdateTime(date);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildMetadataSyncManage() {
        List<DataMetadataHandler<Object>> dataMetadataHandlerList;
        List<DatabaseAndMetadataMapper> databaseAndMetadataMapperList = new ArrayList<>();
        if (!this.functionConfig.getIncludeSyncType().isEmpty()) {
            Set<Class<?>> handlerSet = new HashSet<>();
            for (DatabaseAndMetadataType databaseAndMetadataType : DatabaseAndMetadataType.values()) {
                DatabaseAndMetadataMapper databaseAndMetadataMapper = databaseAndMetadataType.getDatabaseAndMetadataMapper();
                if (!this.functionConfig.getIncludeSyncType().contains(databaseAndMetadataMapper.getMetaType())) {
                    continue;
                }
                databaseAndMetadataMapperList.add(databaseAndMetadataMapper);
                handlerSet.add(databaseAndMetadataMapper.getMetadataHandlerClass());
            }
            dataMetadataHandlerList = new ArrayList<>();
            for (DataMetadataHandler dataMetadataHandler : this.dataMetadataHandlerList) {
                if (handlerSet.contains(dataMetadataHandler.getClass())) {
                    continue;
                }
                dataMetadataHandlerList.add((DataMetadataHandler<Object>) dataMetadataHandler);
            }
        } else {
            dataMetadataHandlerList = (List<DataMetadataHandler<Object>>) ((Object) this.dataMetadataHandlerList);
        }

        this.metadataSyncManage.setMetadataSyncResultHandler(this.defaultMetadataSyncResultHandler);
        this.metadataSyncManage.setDataMetadataHandlerList(dataMetadataHandlerList);
        this.metadataSyncManage.setColonyDO(this.clusterMetadataDomain.getColonyDO());

        this.metadataSyncManage.init(1000, 5000, databaseAndMetadataMapperList);
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


}
