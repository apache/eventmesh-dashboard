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
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * eventmesh ClusterDO meta    ClusterDO runtime ClusterDO storage ClusterDO meta（注册中心，zk，） ClusterDO runtime（broker）   ClusterDO 一个集群 eventmesh 集群有
 * 1.有多个 runtime 集群 2.有多个 存储 集群 3.有多个 meta 集群
 * <p>
 * 一个 RocketMQ 集群 1. 有多个 存储群集（这里是 runtime 集群，是内部实例，不是外部实例） 2. 有一个注册中心集群（这里是 meta ）
 */
@Data
public class ColonyDO<C extends ClusterBaseDO> {

    private Long superiorId;

    private ColonyDO<C> superiorDO;

    private Long clusterId;

    private C clusterDO;

    // 双活集群 所以是可以是一个list的
    // 可以默认一个集群
    private Map<Long, ColonyDO<C>> runtimeColonyDOList = new ConcurrentHashMap<>();

    // 只有 eventmesh 集群有这个点，其他没有。
    private Map<Long, ColonyDO<C>> storageColonyDOList = new ConcurrentHashMap<>();

    /**
     * A(nameserver cluster) a1 a2 a3 B(nameserver cluster) b1 b2 b3
     * <p>
     * rocketmq   a1,a2,a3,b1,b2,b3
     */
    private Map<Long, ColonyDO<C>> metaColonyDOList = new ConcurrentHashMap<>();


    private Map<Long, ColonyDO<C>> allColonyDO;

    private Class<?> clusterBaseDO;

    private ClusterType clusterType;

    private ClusterSyncMetadata clusterSyncMetadata;


    public static <T> T create(Class<?> clusterDO, Object metadata) {

        try {
            ColonyDO<ClusterBaseDO> colonyDO = new ColonyDO<>();
            ClusterBaseDO clusterEntityDO = (ClusterBaseDO) clusterDO.newInstance();
            clusterEntityDO.setClusterInfo(metadata);
            colonyDO.setClusterDO(clusterEntityDO);
            colonyDO.setClusterBaseDO(clusterDO);
            colonyDO.allColonyDO = new ConcurrentHashMap<>();
            return (T) colonyDO;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public void setClusterType(ClusterType clusterType) {
        this.clusterType = clusterType;
        this.clusterSyncMetadata = ClusterSyncMetadataEnum.valueOf(clusterType.toString()).getClusterSyncMetadata();
    }


    public void register(Long mainId, Long runtimeId, Object object, NetAddress netAddress) {
        ColonyDO<C> mainColony = this.allColonyDO.get(mainId);
        mainColony.getClusterDO().getRuntimeMap().put(runtimeId, object);
        if (mainColony.getClusterSyncMetadata().getClusterFramework().isCAP()) {
            AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig = mainColony.getClusterDO().getMultiCreateSDKConfig();
            if (mainColony.getClusterType().isMeta()) {
                abstractMultiCreateSDKConfig.addNetAddress(netAddress);
            } else {
                abstractMultiCreateSDKConfig.addMetaAddress(netAddress);
            }
        }
    }

    public void register(Long clusterId, ClusterType clusterType, Object object) {
        ColonyDO<C> colonyDO = create(this.clusterBaseDO, object);
        colonyDO.setClusterId(clusterId);
        colonyDO.setClusterType(clusterType);
        colonyDO.setClusterSyncMetadata(ClusterSyncMetadataEnum.getClusterSyncMetadata(clusterType));
        this.allColonyDO.put(clusterId, colonyDO);
    }

    public void relationship(Long mainId, Long clusterId) {
        ColonyDO<C> mainColony = this.allColonyDO.get(mainId);

        ColonyDO<C> colonyDO = this.allColonyDO.get(clusterId);
        colonyDO.setSuperiorId(mainId);
        colonyDO.setSuperiorDO(mainColony);

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


    public void removeRuntime(Long clusterId, Long runtimeId, NetAddress netAddress) {
        ColonyDO<C> mainColony = this.allColonyDO.get(clusterId);
        mainColony.getClusterDO().getRuntimeMap().remove(runtimeId);
        if (mainColony.getClusterSyncMetadata().getClusterFramework().isCAP()) {
            AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig = mainColony.getClusterDO().getMultiCreateSDKConfig();
            if (mainColony.getClusterType().isMeta()) {
                abstractMultiCreateSDKConfig.removeMetaAddress(netAddress);
            } else {
                abstractMultiCreateSDKConfig.removeNetAddress(netAddress);
            }
        }
    }

    public void remove(Long clusterId) {
        ColonyDO<C> colonyDO = this.allColonyDO.remove(clusterId);
        this.remove(colonyDO.getClusterType(), clusterId);
    }

    public void remove(ClusterType clusterType, Long clusterId) {
        Map<Long, ColonyDO<C>> colonyDOMap = this.getColonyDOMap(clusterType);
        colonyDOMap.remove(clusterId);
    }

    public void setAbstractMultiCreateSDKConfig(Long clusterId, AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig) {
        ColonyDO<?> colonyDO = this.allColonyDO.get(clusterId);
        colonyDO.getClusterDO().setMultiCreateSDKConfig(abstractMultiCreateSDKConfig);
    }

    public void setCreateSDKConfig(Long clusterId, Long runtime, AbstractCreateSDKConfig createSDKConfig) {
        ColonyDO<?> colonyDO = this.allColonyDO.get(clusterId);
        RuntimeBaseDO runtimeBaseDO = (RuntimeBaseDO) colonyDO.getClusterDO().getRuntimeMap().get(runtime);
        runtimeBaseDO.setCreateSDKConfig(createSDKConfig);
    }

    public Map<Long, ColonyDO<C>> getColonyDOMap(ClusterType clusterType) {
        if (Objects.equals(this.clusterType, ClusterType.EVENTMESH_CLUSTER)) {
            return clusterType.isStorage() ? this.storageColonyDOList
                : clusterType.isMeta() ? this.metaColonyDOList : this.runtimeColonyDOList;
        }
        return clusterType.isMeta() ? this.metaColonyDOList : this.runtimeColonyDOList;

    }

    public Long getClusterId() {
        return clusterId;
    }

}
