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


package org.apache.eventmesh.dashboard.console.domain;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.GetClusterInSyncReturnDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterInSyncDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterTreeDO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.ClusterTreeVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.alibaba.fastjson.JSON;

/**
 *
 */
public interface ClusterAndRuntimeDomain {


    List<ClusterEntity> getClusterByClusterId(ClusterEntity clusterEntity);

    List<ClusterTreeVO> queryClusterTree(QueryClusterTreeDO data);

    ClusterAndRuntimeOfRelationshipDO getAllClusterAndRuntimeByCluster(ClusterEntity clusterEntity, DeployStatusType deployStatusType);

    GetClusterInSyncReturnDO queryClusterInSync(ClusterEntity clusterEntity, List<ClusterType> syncClusterTypeList);

    default <T> T queryClusterInSync(QueryClusterInSyncDO data) {
        GetClusterInSyncReturnDO getClusterInSyncReturnDO = this.queryClusterInSync(data.getClusterEntity(), data.getSyncClusterTypeList());
        if (Objects.isNull(getClusterInSyncReturnDO.getClusterEntityList())
            && Objects.isNull(getClusterInSyncReturnDO.getRuntimeEntityList())) {
            throw new RuntimeException("");
        }
        /*
         * 如果  entity 的就直接调用。 是  core 的需要转换一次。
         * 目前只有 core 的， 没有 entity 的
         *
         * 转两次，虽然有性能损失，对于一个运维系统意义不大。
         */
        if (Objects.nonNull(data.getClusterOperationHandler())) {
            getClusterInSyncReturnDO.getRuntimeEntityList().forEach(runtimeEntity -> {
                RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
                data.getClusterOperationHandler().handler(runtimeMetadata);
            });
            getClusterInSyncReturnDO.getClusterEntityList().forEach(entity -> {
                ClusterMetadata clusterMetadata = new ClusterMetadata();
                data.getClusterOperationHandler().handler(clusterMetadata);
            });
            return null;
        }
        Supplier<BaseRuntimeIdEntity> function =
            Objects.nonNull(data.getFunction()) ? data.getFunction() : this.createBaseRuntimeIdEntity(data.getObject());
        List<BaseRuntimeIdEntity> runtimeIdEntityList =
            new ArrayList<>(getClusterInSyncReturnDO.getClusterEntityList().size() + getClusterInSyncReturnDO.getRuntimeEntityList().size());

        getClusterInSyncReturnDO.getClusterEntityList().forEach(clusterEntity -> {
            BaseRuntimeIdEntity baseRuntimeIdEntity = function.get();
            runtimeIdEntityList.add(baseRuntimeIdEntity);
            baseRuntimeIdEntity.setClusterId(clusterEntity.getId());
            baseRuntimeIdEntity.setClusterType(clusterEntity.getClusterType());
            baseRuntimeIdEntity.setRuntimeId(clusterEntity.getId());
        });

        getClusterInSyncReturnDO.getRuntimeEntityList().forEach(runtimeEntity -> {
            BaseRuntimeIdEntity baseRuntimeIdEntity = function.get();
            runtimeIdEntityList.add(baseRuntimeIdEntity);
            baseRuntimeIdEntity.setClusterId(runtimeEntity.getClusterId());
            baseRuntimeIdEntity.setClusterType(runtimeEntity.getClusterType());
            baseRuntimeIdEntity.setRuntimeId(runtimeEntity.getId());
        });
        return (T) runtimeIdEntityList;
    }


    default Supplier<BaseRuntimeIdEntity> createBaseRuntimeIdEntity(Object object) {
        String json = JSON.toJSONString(object);
        Class<?> clazz = object.getClass();
        return () -> (BaseRuntimeIdEntity) JSON.parseObject(json, clazz);
    }


}
