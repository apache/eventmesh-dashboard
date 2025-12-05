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


package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * eventmesh ClusterDO meta    ClusterDO runtime ClusterDO storage ClusterDO meta（注册中心，zk，） ClusterDO runtime（broker）   ClusterDO 一个集群 eventmesh 集群有
 * 1.有多个 runtime 集群 2.有多个 存储 集群 3.有多个 meta 集群
 * <p>
 * 一个 RocketMQ 集群 1. 有多个 存储群集（这里是 runtime 集群，是内部实例，不是外部实例） 2. 有一个注册中心集群（这里是 meta ）
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Data
@Slf4j
public class ColonyDO<C extends ClusterBaseDO> {

    private Long superiorId;

    //private ColonyDO<C> superiorDO;

    @Getter
    private Long clusterId;

    private C clusterDO;

    // 双活集群 所以是可以是一个list的
    // 可以默认一个集群
    private Map<Long, ColonyDO<C>> runtimeColonyDOMap = new ConcurrentHashMap<>();

    // 只有 eventmesh 集群有这个点，其他没有。
    private Map<Long, ColonyDO<C>> storageColonyDOMap = new ConcurrentHashMap<>();

    /**
     * A(nameserver cluster) a1 a2 a3 B(nameserver cluster) b1 b2 b3
     * <p>
     * rocketmq   a1,a2,a3,b1,b2,b3
     */
    private Map<Long, ColonyDO<C>> metaColonyDOList = new ConcurrentHashMap<>();


    private Map<Long, ColonyDO<C>> allColonyDO;

    private Class<?> clusterBaseDOClass;

    private ClusterType clusterType;

    private ClusterSyncMetadata clusterSyncMetadata;


    public static <T> T create(Class<?> clusterDO, Object metadata) {

        try {
            ColonyDO<ClusterBaseDO> colonyDO = new ColonyDO<>();
            //ClusterBaseDO clusterEntityDO = (ClusterBaseDO) clusterDO.newInstance();
            //clusterEntityDO.setClusterInfo(metadata);
            colonyDO.setClusterDO((ClusterBaseDO) metadata);
            colonyDO.allColonyDO = new ConcurrentHashMap<>();
            return (T) colonyDO;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static <T> T createBaseDO(Class<?> clusterDO, Object metadata) {
        try {
            ClusterBaseDO clusterEntityDO = (ClusterBaseDO) clusterDO.newInstance();
            clusterEntityDO.setClusterInfo(metadata);
            return (T) clusterEntityDO;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void setClusterType(ClusterType clusterType) {
        this.clusterType = clusterType;
        this.clusterSyncMetadata = ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterType);
    }


    public ColonyDO<C> register(Long mainId, Long runtimeId, Object object, NetAddress netAddress) {
        ColonyDO<C> mainColony = this.allColonyDO.get(mainId);
        if (Objects.isNull(mainColony)) {
            return null;
        }
        mainColony.getClusterDO().getRuntimeMap().put(runtimeId, object);
        if (mainColony.getClusterSyncMetadata().getClusterFramework().isCAP()) {
            AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig = mainColony.getClusterDO().getMultiCreateSDKConfig();
            if (mainColony.getClusterType().isMeta()) {
                abstractMultiCreateSDKConfig.addMetaAddress(netAddress);
            } else {
                abstractMultiCreateSDKConfig.addNetAddress(netAddress);
            }
        }
        return mainColony;
    }

    public ColonyDO<C> register(Long clusterId, ClusterType clusterType, Object object) {
        ColonyDO<C> current = this.allColonyDO.get(clusterId);
        if (Objects.isNull(current)) {
            ColonyDO<C> colonyDO = create(this.clusterBaseDOClass, object);
            colonyDO.setClusterId(clusterId);
            colonyDO.setClusterType(clusterType);
            colonyDO.setClusterSyncMetadata(ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterType));
            this.allColonyDO.put(clusterId, colonyDO);
            current = colonyDO;
        } else {
            current.setClusterDO((C) object);
        }
        return current;
    }

    public void relationship(Long mainId, Long clusterId) {
        ColonyDO<C> mainColony = this.allColonyDO.get(mainId);

        ColonyDO<C> colonyDO = this.allColonyDO.get(clusterId);
        if (Objects.isNull(colonyDO)) {
            log.error("clusterId to colonyDO is null, main is is {}, cluster id {}", mainId, clusterId);
            return;
        }
        colonyDO.setSuperiorId(mainId);
        //colonyDO.setSuperiorDO(mainColony);

        mainColony.relationship(colonyDO.getClusterType(), clusterId, colonyDO);
    }

    private void relationship(ClusterType clusterType, Long clusterId, ColonyDO<C> colonyDO) {
        Map<Long, ColonyDO<C>> colonyDOMap = this.getColonyDOMap(clusterType);
        colonyDOMap.put(clusterId, colonyDO);
    }

    public void unRelationship(Long mainId, Long clusterId) {
        ColonyDO<C> mainColony = this.allColonyDO.get(mainId);
        ColonyDO<C> colonyDO = this.allColonyDO.get(clusterId);
        mainColony.remove(colonyDO.getClusterType(), clusterId);
    }


    public ColonyDO<C> removeRuntime(Long clusterId, Long runtimeId, NetAddress netAddress) {
        ColonyDO<C> mainColony = this.allColonyDO.get(clusterId);
        if (Objects.isNull(mainColony)) {
            return null;
        }
        mainColony.getClusterDO().getRuntimeMap().remove(runtimeId);
        if (mainColony.getClusterSyncMetadata().getClusterFramework().isCAP()) {
            AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig = mainColony.getClusterDO().getMultiCreateSDKConfig();
            if (mainColony.getClusterType().isMeta()) {
                abstractMultiCreateSDKConfig.removeMetaAddress(netAddress);
            } else {
                abstractMultiCreateSDKConfig.removeNetAddress(netAddress);
            }
        }
        return mainColony;
    }

    public ColonyDO<C> remove(Long clusterId) {
        return this.allColonyDO.remove(clusterId);
    }

    public void remove(ClusterType clusterType, Long clusterId) {
        Map<Long, ColonyDO<C>> colonyDOMap = this.getColonyDOMap(clusterType);
        colonyDOMap.remove(clusterId);
    }


    public Map<Long, ColonyDO<C>> getColonyDOMap(ClusterType clusterType) {
        if (Objects.equals(this.clusterType, ClusterType.EVENTMESH_CLUSTER)) {
            return clusterType.isStorage() ? this.storageColonyDOMap
                : clusterType.isMeta() ? this.metaColonyDOList : this.runtimeColonyDOMap;
        }
        return clusterType.isMeta() ? this.metaColonyDOList : this.runtimeColonyDOMap;

    }

}
