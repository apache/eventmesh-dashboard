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

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;

/**
 * 不会处理 关联
 */
@Slf4j
@Component
public class RuntimeDeployService {

    private static final Map<DeployStatusType, DeployStatusType> DEPLOY_FAIL_STATUS = new HashMap<>();

    static {

    }

    static DeployStatusType getFailedType(DeployStatusType deployStatusType) {
        return DEPLOY_FAIL_STATUS.get(deployStatusType);
    }

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterRelationshipService clusterRelationshipService;

    @Autowired
    private DeployScriptService deployScriptService;

    @Autowired
    private ResourcesConfigService resourcesConfigService;

    @Autowired
    private ConfigService configService;

    private final ThreadPoolExecutor deployThreadPoolExecutor = new ThreadPoolExecutor(10, 10, 20, TimeUnit.SECONDS, new LinkedBlockingDeque<>(),
        new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "deploy-" + counter.incrementAndGet());
            }
        });


    @Scheduled(initialDelay = 100, fixedDelay = 100)
    public void deploy() {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setUpdateTime(LocalDateTime.now().minusMinutes(500));
        List<RuntimeEntity> runtimeEntityList = this.runtimeService.queryByUpdateTime(runtimeEntity);

        runtimeEntityList.forEach(entity -> {
            AbstractRuntimeServiceTask abstractRuntimeServiceTask = this.createTask(entity);
            if (Objects.isNull(abstractRuntimeServiceTask)) {
                return;
            }
            abstractRuntimeServiceTask.runtimeEntity = entity;
            deployThreadPoolExecutor.execute(abstractRuntimeServiceTask);
        });
    }


    private AbstractRuntimeServiceTask createTask(RuntimeEntity entity) {
        switch (entity.getDeployStatusType()) {
            case CREATE_WAIT, CREATE_FULL_WAIT, CREATE_CAP_UPDATE_WAIT, UPDATE_WAIT -> {
                return new RuntimeCreateDeploy();
            }
            case PAUSE_ING, PAUSE_FULL_WAIT -> {
                return new RuntimePauseDeploy();
            }
            case UNINSTALL -> {
                return new RuntimeDeleteDeploy();
            }
            default -> {
                return null;
            }
        }
    }

    abstract class AbstractRuntimeServiceTask implements Runnable {

        protected RuntimeEntity runtimeEntity;

        protected ClusterEntity clusterEntity;

        protected ClusterType clusterType;

        protected ClusterFramework clusterFramework;

        protected ClusterEntity kubeClusterEntity;

        protected KubernetesClient kubernetesClient;

        protected List<RuntimeEntity> metaAndRuntimeList;

        protected List<RuntimeEntity> updateRuntimeList = new ArrayList<>();

        @Override
        public void run() {
            try {
                this.clusterType = this.runtimeEntity.getClusterType();
                ClusterEntity clusterEntity = new ClusterEntity();
                clusterEntity.setId(this.runtimeEntity.getClusterId());
                this.clusterEntity = clusterService.queryClusterById(clusterEntity);

                this.kubeClusterEntity = clusterService.queryRelationshipClusterByClusterIdAndType(clusterEntity);
                BaseSyncBase base = new ClusterMetadata();
                base.setId(this.kubeClusterEntity.getId());
                this.kubernetesClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, base.getUnique());
                /*
                 *   查询 kubernetes  cluster
                 *  TODO 目前无法处理 多 kubernetes 的问题
                 */

                if (Objects.equals(this.runtimeEntity.getDeployStatusType(), DeployStatusType.UNINSTALL)) {
                    this.handler();
                    return;
                }

                // TODO 非 mate 都需要  meta
                if (!this.clusterType.isMeta() && !this.clusterType.isMetaAndRuntime()) {
                    // 存储节点必须要 meta信息
                    runtimeService.queryMetaRuntimeByStorageClusterId(runtimeEntity);
                }
                if (this.clusterType.isMetaAndRuntime()) {
                    // 只更新
                    metaAndRuntimeList = runtimeService.queryOnlyRuntimeByClusterId(runtimeEntity);
                }

                this.handler();
                if (!Objects.equals(this.runtimeEntity.getDeployStatusType(), DeployStatusType.PAUSE_FULL_WAIT) && !Objects.equals(
                    this.runtimeEntity.getDeployStatusType(), DeployStatusType.CREATE_FULL_WAIT)) {

                    if (this.clusterType.isMetaAndRuntime() && Objects.equals(this.runtimeEntity.getDeployStatusType(),
                        DeployStatusType.CREATE_WAIT)) {
                        // 修改数据
                        this.metaAndRuntimeList.forEach((value) -> {
                            value.setDeployStatusType(DeployStatusType.CREATE_CAP_UPDATE_WAIT);
                            this.updateRuntimeList.add(value);
                        });
                    }
                    if (this.clusterType.isMeta()) {
                        runtimeService.queryMetaRuntimeByStorageClusterId(runtimeEntity).values().forEach((value) -> {
                            value.forEach(runtimeEntity -> {
                                runtimeEntity.setDeployStatusType(DeployStatusType.UPDATE_WAIT);
                                this.updateRuntimeList.add(runtimeEntity);
                            });
                        });
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                this.runtimeEntity.setDeployStatusType(getFailedType(runtimeEntity.getDeployStatusType()));
            }
            this.updateRuntimeList.add(this.runtimeEntity);

            try {
                runtimeService.batchUpdate(this.updateRuntimeList);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        abstract void handler();
    }

    class RuntimeDeleteDeploy extends AbstractRuntimeServiceTask {


        @Override
        void handler() {
            // 直接从 云 or 容器 删除资源
        }

    }

    class RuntimePauseDeploy extends AbstractRuntimeServiceTask {

        @Override
        void handler() {

        }
    }


    class RuntimeCreateDeploy extends AbstractRuntimeServiceTask {

        private ResourcesConfigEntity resourcesConfigEntity;

        private DeployScriptEntity deployScriptEntity;

        private List<ConfigEntity> configEntityList;


        @Override
        void handler() {
            DeployScriptEntity deployScriptEntity = new DeployScriptEntity();
            deployScriptEntity.setId(this.clusterEntity.getDeployScriptId());
            this.deployScriptEntity = deployScriptService.queryById(deployScriptEntity);

            ResourcesConfigEntity resourcesConfigEntity = new ResourcesConfigEntity();
            resourcesConfigEntity.setId(this.clusterEntity.getResourcesConfigId());

            this.resourcesConfigEntity = resourcesConfigService.queryResourcesById(resourcesConfigEntity);

            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setClusterId(this.clusterEntity.getId());
            configEntity.setInstanceId(this.clusterEntity.getId());
            configEntity.setInstanceType(MetadataType.CLUSTER);

            this.configEntityList = configService.queryByClusterAndInstanceId(configEntity);


        }

        public Map<String, Object> createYamlData(RuntimeEntity runtimeEntity) {
            Map<String, Object> yaml = new HashMap<>();
            // 资源
            yaml.putAll((Map<String, Object>) JSON.toJSON(this.resourcesConfigEntity));

            // kubernetes metadata
            yaml.put("namespace", runtimeEntity.getClusterId());
            yaml.put("name", runtimeEntity.getId());

            // 注册中心， 每次更新，都需要逐一修改runtime，重启
            yaml.put("metaAddress", this.metaAddress);

            return yaml;
        }
    }
}
