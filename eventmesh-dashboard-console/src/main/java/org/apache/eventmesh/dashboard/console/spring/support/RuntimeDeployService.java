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
import org.apache.eventmesh.dashboard.common.port.PortValidate;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.modle.DO.runtime.QueryRuntimeByBigExpandClusterDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.GetPortsDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;
import org.apache.eventmesh.dashboard.console.service.deploy.PortService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.console.spring.support.register.BuildMetadata;
import org.apache.eventmesh.dashboard.console.spring.support.register.BuildMetadataManage;
import org.apache.eventmesh.dashboard.console.spring.support.register.ScriptBuildData;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import lombok.extern.slf4j.Slf4j;

/**
 * 不会处理 关联
 */
@Slf4j
@Component
public class RuntimeDeployService {

    private static final Map<DeployStatusType, DeployStatusType> DEPLOY_FAIL_STATUS = new HashMap<>();

    private static final Map<DeployStatusType, DeployStatusType> DEPLOY_ING_STATUS = new HashMap<>();

    private static final Map<DeployStatusType, DeployStatusType> DEPLOY_SUCCESS_STATUS = new HashMap<>();

    static {
        DEPLOY_FAIL_STATUS.put(DeployStatusType.CREATE_WAIT, DeployStatusType.CREATE_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.CREATE_FULL_WAIT, DeployStatusType.CREATE_FULL_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.CREATE_CAP_UPDATE_WAIT, DeployStatusType.CREATE_CAP_UPDATE_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.UPDATE_WAIT, DeployStatusType.UPDATE_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.UPDATE_FULL_WAIT, DeployStatusType.UPDATE_FULL_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.PAUSE_WAIT, DeployStatusType.UPDATE_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.PAUSE_FULL_WAIT, DeployStatusType.PAUSE_FULL_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.RESET_WAIT, DeployStatusType.RESET_FAIL);
        DEPLOY_FAIL_STATUS.put(DeployStatusType.UNINSTALL, DeployStatusType.UNINSTALL_FAIL);

        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.CREATE_WAIT, DeployStatusType.CREATE_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.CREATE_FULL_WAIT, DeployStatusType.CREATE_FULL_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.CREATE_CAP_UPDATE_WAIT, DeployStatusType.CREATE_CAP_UPDATE_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.UPDATE_WAIT, DeployStatusType.UPDATE_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.UPDATE_FULL_WAIT, DeployStatusType.UPDATE_FULL_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.PAUSE_WAIT, DeployStatusType.UPDATE_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.PAUSE_FULL_WAIT, DeployStatusType.PAUSE_FULL_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.RESET_WAIT, DeployStatusType.RESET_SUCCESS);
        DEPLOY_SUCCESS_STATUS.put(DeployStatusType.UNINSTALL, DeployStatusType.UNINSTALL_SUCCESS);

        DEPLOY_ING_STATUS.put(DeployStatusType.CREATE_WAIT, DeployStatusType.CREATE_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.CREATE_FULL_WAIT, DeployStatusType.CREATE_FULL_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.CREATE_CAP_UPDATE_WAIT, DeployStatusType.CREATE_CAP_UPDATE_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.UPDATE_WAIT, DeployStatusType.UPDATE_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.UPDATE_FULL_WAIT, DeployStatusType.UPDATE_FULL_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.PAUSE_WAIT, DeployStatusType.UPDATE_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.PAUSE_FULL_WAIT, DeployStatusType.PAUSE_FULL_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.RESET_WAIT, DeployStatusType.RESET_ING);
        DEPLOY_ING_STATUS.put(DeployStatusType.UNINSTALL, DeployStatusType.UNINSTALL_ING);

    }

    static DeployStatusType getFailedType(DeployStatusType deployStatusType) {
        return DEPLOY_FAIL_STATUS.get(deployStatusType);
    }

    static DeployStatusType getIngType(DeployStatusType deployStatusType) {
        return DEPLOY_ING_STATUS.get(deployStatusType);
    }

    static DeployStatusType getSuccess(DeployStatusType deployStatusType) {
        return DEPLOY_ING_STATUS.get(deployStatusType);
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

    @Autowired
    private PortService portService;


    private final BuildMetadataManage buildMetadataManage = new BuildMetadataManage();

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


    public AbstractRuntimeServiceTask createTask(RuntimeEntity entity) {
        switch (entity.getDeployStatusType()) {
            case CREATE_WAIT, CREATE_FULL_WAIT, CREATE_CAP_UPDATE_WAIT, UPDATE_WAIT -> {
                return new RuntimeCreateDeploy();
            }
            case PAUSE_ING, PAUSE_FULL_WAIT -> {
                return new RuntimePauseDeploy();
            }
            case UNINSTALL -> {
                return new RuntimeUninstallDeploy();
            }
            default -> {
                return null;
            }
        }
    }

    public abstract class AbstractRuntimeServiceTask implements Runnable {

        protected RuntimeEntity runtimeEntity;

        protected ClusterEntity clusterEntity;

        protected ClusterType clusterType;

        protected ClusterFramework clusterFramework;

        protected ClusterEntity kubeClusterEntity;

        protected KubernetesClient kubernetesClient;

        protected List<RuntimeEntity> metaAndRuntimeList;

        protected List<RuntimeEntity> updateRuntimeList = new ArrayList<>();

        protected ScriptBuildData scriptBuildData = new ScriptBuildData();

        protected BuildMetadata buildMetadata;


        public void queryHandlerData() {
            this.clusterType = this.runtimeEntity.getClusterType();
            ClusterEntity clusterEntity = new ClusterEntity();
            clusterEntity.setId(this.runtimeEntity.getClusterId());
            this.clusterEntity = clusterService.queryClusterById(clusterEntity);
            /*
             *   查询 kubernetes  cluster
             *  TODO 目前无法处理 多 kubernetes 的问题
             */
            this.kubeClusterEntity = clusterService.queryRelationshipClusterByClusterIdAndType(clusterEntity);
            BaseSyncBase base = new ClusterMetadata();
            base.setId(this.kubeClusterEntity.getId());
            this.kubernetesClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, base.getUnique());
            this.buildMetadata = buildMetadataManage.getBuildMetaAddress(this.runtimeEntity.getClusterType());
        }

        public void buildOperationMetaData() {
            scriptBuildData.put("namespace", runtimeEntity.getClusterId());
            scriptBuildData.put("name", runtimeEntity.getId());

            scriptBuildData.put("clusterType", runtimeEntity.getClusterType());
            scriptBuildData.put("clusterId", runtimeEntity.getClusterId());
            scriptBuildData.put("runtimeId", runtimeEntity.getId());
        }

        public void buildMetaAddress() {
            if (!Objects.equals(RuntimeCreateDeploy.class, this.getClass())) {
                return;
            }
            // TODO 非 mate 都需要  meta,
            if (!this.clusterType.isMeta() && !this.clusterType.isMetaAndRuntime()) {
                // 存储节点必须要 meta信息

                QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO = QueryRuntimeByBigExpandClusterDO.builder()
                    .followClusterId(clusterEntity.getId()).queryClusterTypeList(this.clusterType.getMetaClusterType()).build();

                List<RuntimeEntity> newRuntimeEntityList = runtimeService.queryMetaRuntimeByStorageClusterId(queryRuntimeByBigExpandClusterDO);
                this.buildMetadata.buildMetaAddress(this.scriptBuildData, runtimeEntity, newRuntimeEntityList);
            }
            // 如果是创建 runtime ， 需要得到 所有存储的 runtime
            if (Objects.equals(this.clusterType, ClusterType.EVENTMESH_RUNTIME)) {
                QueryRuntimeByBigExpandClusterDO data = QueryRuntimeByBigExpandClusterDO.builder().followClusterId(this.clusterEntity.getId())
                    .mainClusterType(ClusterType.EVENTMESH_CLUSTER).storageClusterTypeList(ClusterType.getStorageCluster())
                    .storageMetaClusterTypeList(ClusterType.getStorageMetaCluster()).build();

                List<RuntimeEntity> storageMetaRuntimeList = runtimeService.queryRuntimeByBigExpandCluster(data);
                this.buildMetadata.buildMetaAddress(this.scriptBuildData, runtimeEntity, storageMetaRuntimeList);
            }
            if (this.clusterType.isMetaAndRuntime() || (this.clusterType.isMeta() && this.clusterFramework.isCAP())) {
                // CAP 模式 需要得到本集群里面所有 runtime
                List<RuntimeEntity> metaAndRuntimeList = runtimeService.queryRuntimeToFrontByClusterId(runtimeEntity);
                this.buildMetadata.buildMetaAddress(this.scriptBuildData, runtimeEntity, metaAndRuntimeList);
            }
        }



        /**
         * runtime 进行操作的时候，会影响 当前集群 或则 依赖菊琴
         *
         * <ol>
         *     <li> CAP 集群修改，集群内所有节点都必须修改，同时 依赖集群同时也要修改 </li>
         *     <li> TODO CAP 集群是 批量创建创建 @see  PAUSE_FULL_WAIT CREATE_FULL_WAIT ， 不会影响</li>
         * </ol>
         */
        public void influence() {
            if (Objects.equals(this.runtimeEntity.getDeployStatusType(), DeployStatusType.PAUSE_FULL_WAIT) || Objects.equals(
                this.runtimeEntity.getDeployStatusType(), DeployStatusType.CREATE_FULL_WAIT)) {
                return;
            }

            if (this.clusterType.isMetaAndRuntime() && Objects.equals(this.runtimeEntity.getDeployStatusType(),
                DeployStatusType.CREATE_WAIT)) {
                // 修改数据
                this.metaAndRuntimeList.forEach((value) -> {
                    value.setDeployStatusType(DeployStatusType.CREATE_CAP_UPDATE_WAIT);
                    this.updateRuntimeList.add(value);
                });
            }
            if (this.clusterType.isMeta()) {
                // 查询 当前集群下 所有 节点， 然后更新这些节点的内容
                runtimeService.queryRuntimeToFrontByClusterId(this.runtimeEntity).forEach((value) -> {
                    if (Objects.equals(this.runtimeEntity.getId(), value.getId())) {
                        return;
                    }
                    value.setDeployStatusType(DeployStatusType.UPDATE_WAIT);
                    this.updateRuntimeList.add(runtimeEntity);
                });
            }

        }

        @Override
        public void run() {
            try {
                this.queryHandlerData();
                this.buildOperationMetaData();
                if (Objects.equals(this.runtimeEntity.getDeployStatusType(), DeployStatusType.UNINSTALL)) {
                    this.handler();
                    return;
                }
                this.buildMetaAddress();
                this.handler();
                this.influence();
                this.runtimeEntity.setDeployStatusType(getSuccess(runtimeEntity.getDeployStatusType()));
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

    class RuntimeUninstallDeploy extends AbstractRuntimeServiceTask {


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


    class RuntimeCreateCheckDeploy extends AbstractRuntimeServiceTask {

        @Override
        void handler() {

        }
    }

    class RuntimeCreateDeploy extends AbstractRuntimeServiceTask {

        private DeployScriptEntity deployScriptEntity;


        @Override
        void handler() {
            runtimeEntity.setKubernetesClusterId(this.kubeClusterEntity.getId());
            deployScriptEntity = new DeployScriptEntity();
            deployScriptEntity.setId(this.clusterEntity.getDeployScriptId());
            this.deployScriptEntity = deployScriptService.queryById(deployScriptEntity);

            ResourcesConfigEntity resourcesConfigEntity = new ResourcesConfigEntity();
            resourcesConfigEntity.setId(this.clusterEntity.getResourcesConfigId());

            resourcesConfigEntity = resourcesConfigService.queryResourcesById(resourcesConfigEntity);
            this.scriptBuildData.setResourcesConfigEntity(resourcesConfigEntity);

            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setClusterId(this.clusterEntity.getId());
            configEntity.setInstanceId(this.clusterEntity.getId());
            configEntity.setInstanceType(MetadataType.CLUSTER);
            List<ConfigEntity> configEntityList = configService.queryByClusterAndInstanceId(configEntity);

            this.scriptBuildData.setConfigEntityList(configEntityList);
            this.buildMetadata.buildConfig(this.scriptBuildData, this.runtimeEntity);
            this.applyPort();
            this.kubernetesClient.resource(this.buildScriptContent()).serverSideApply();

            //kubernetesClient.load(new ByteArrayInputStream(this.buildScriptContent().getBytes())).serverSideApply().

        }

        private void applyPort() {
            PortValidate portValidate = this.buildMetadata.portValidate();
            portValidate.portRules();
            GetPortsDO getPortsDO = new GetPortsDO();
            getPortsDO.setClusterId(this.clusterEntity.getId());
            getPortsDO.setPortNum(portValidate.portRules().size());
            List<String> port = portService.getPorts(getPortsDO);
            for (int i = 0; i < port.size(); i++) {
                this.scriptBuildData.put(portValidate.portRules().get(i).getName(), port.get(i));
            }
        }

        private String buildScriptContent() {
            String scriptContent = this.deployScriptEntity.getContent();

            for (Entry<String, Object> entity : this.scriptBuildData.getData().entrySet()) {
                scriptContent = scriptContent.replace("${" + entity.getKey() + "}", entity.getValue().toString());
            }

            return scriptContent;
        }
    }
}
