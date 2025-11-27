/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.spring.support;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KubernetesManage {

    @Autowired
    private ClusterService clusterService;

    @Setter
    private boolean useIntelligent = false;

    private final Map<Long, ResourcesDataWrapper> resourcesConfigEntityMap = new ConcurrentHashMap<>();


    public void register(BaseSyncBase baseSyncBase) {
        ResourcesDataWrapper resourcesDataWrapper = new ResourcesDataWrapper();
        resourcesDataWrapper.setBaseSyncBase(baseSyncBase);
        ClientWrapper clientWrapper = SDKManage.getInstance().getClientWrapper(baseSyncBase.getUnique());
        resourcesDataWrapper.setClientWrapper(clientWrapper);
        resourcesConfigEntityMap.put(baseSyncBase.getId(), resourcesDataWrapper);

    }

    public void unregister(BaseSyncBase baseSyncBase) {
        resourcesConfigEntityMap.remove(baseSyncBase.getId());
    }


    public void syncKubernetes() {
        this.resourcesConfigEntityMap.values().forEach(resourcesDataWrapper -> {
            resourcesDataWrapper.remainingConfigEntityList = this.sync(resourcesDataWrapper);
        });
    }


    /**
     * TODO 目前只支持 总量计算，总结计算在单个节点剩余量不够的情况下，会出现失败的情况
     *      解决方案：
     *       1. 循环使用 已实现，
     *       2. 求多维度最优解 未实现
     * TODO 只能做实时匹配，不能做内存匹配，因为会出现同步时差，数据不一致。 所以作废内存匹配实现
     *      不对，实时匹配也有问题，两次申请的时候之间，第一次的create还没成功，就申请了。
     *      以为可以交给其他人做，结果还只能自己做.............>.>>.》。。。
     *      还要关注 kubernetes 集群别其他组件或则业务使用。其他组件使用，会使内存数据与 kubernetes数据不一致
     */
    public boolean checkResource(List<ResourcesConfigEntity> apply, Long kubernetesId) {
        ResourcesDataWrapper resourcesDataWrapper = resourcesConfigEntityMap.get(kubernetesId);
        if (Objects.isNull(resourcesDataWrapper)) {
            return false;
        }
        synchronized (resourcesDataWrapper.lock) {
            List<ResourcesConfigEntity> allocatable = this.sync(resourcesDataWrapper);
            if (this.useIntelligent) {
                return this.checkIntelligent(allocatable, apply);
            } else {
                return this.checkTotalAmount(allocatable, apply);
            }
        }

    }

    private List<ResourcesConfigEntity> sync(ResourcesDataWrapper resourcesDataWrapper) {
        List<ResourcesConfigEntity> allocatable = new ArrayList<>();
        KubernetesClient kubernetesClient = resourcesDataWrapper.clientWrapper.getClient(SDKTypeEnum.ADMIN);
        NodeList nodeList = kubernetesClient.nodes().list();
        nodeList.getItems().forEach(node -> {
            // 获取总量
            // Quantity cpuCapacity = node.getStatus().getCapacity().get("cpu");
            // Quantity memoryCapacity = node.getStatus().getCapacity().get("memory");

            // 获取可分配量（剩余量）
            Quantity cpuAllocatable = node.getStatus().getAllocatable().get("cpu");
            Quantity memoryAllocatable = node.getStatus().getAllocatable().get("memory");
            ResourcesConfigEntity allocatableResourcesConfigEntity = new ResourcesConfigEntity();
            allocatableResourcesConfigEntity.setObjectId(node.getMetadata().getName());
            allocatableResourcesConfigEntity.setCpuNum(cpuAllocatable.getNumericalAmount().floatValue());
            allocatableResourcesConfigEntity.setMemNum(memoryAllocatable.getNumericalAmount().floatValue());
            allocatable.add(allocatableResourcesConfigEntity);
        });
        return allocatable;
    }


    private boolean checkIntelligent(List<ResourcesConfigEntity> allocatable, List<ResourcesConfigEntity> apply) {

        return true;
    }

    private boolean checkTotalAmount(List<ResourcesConfigEntity> allocatable, List<ResourcesConfigEntity> apply) {
        ResourcesConfigEntity allocatableEntity = this.total(allocatable);
        ResourcesConfigEntity applyEntity = this.total(apply);
        log.info("allocatable : {} , apply : {} ", allocatableEntity, applyEntity);
        if(allocatableEntity.getCpuNum() > applyEntity.getCpuNum() &&
           allocatableEntity.getMemNum() > applyEntity.getMemNum() &&
           allocatableEntity.getDiskNum() < applyEntity.getDiskNum()){
            return true;
        }else{
            log.error("check fail");
            return false;
        }
    }

    private ResourcesConfigEntity total(List<ResourcesConfigEntity> resourcesConfigEntityList) {
        ResourcesConfigEntity total = new ResourcesConfigEntity();
        total.setCpuNum(0F);
        total.setMemNum(0F);
        total.setDiskNum(0F);
        total.setGpuNum(0F);
        resourcesConfigEntityList.forEach(resourcesConfigEntity -> {
            total.setCpuNum(total.getCpuNum() + resourcesConfigEntity.getCpuNum());
            total.setMemNum(total.getMemNum() + resourcesConfigEntity.getMemNum());
            total.setDiskNum(total.getDiskNum() + resourcesConfigEntity.getDiskNum());
            total.setGpuNum(total.getGpuNum() + resourcesConfigEntity.getGpuNum());
        });
        return total;
    }


    @Data
    private static class ResourcesDataWrapper {

        /**
         * 如果
         */
        private volatile List<ResourcesConfigEntity> nodeResourcesConfigEntityList = new ArrayList<>();


        private volatile List<ResourcesConfigEntity> remainingConfigEntityList = new ArrayList<>();

        private ClientWrapper clientWrapper;

        private final Object lock = new Object();

        private BaseSyncBase baseSyncBase;
    }


}
