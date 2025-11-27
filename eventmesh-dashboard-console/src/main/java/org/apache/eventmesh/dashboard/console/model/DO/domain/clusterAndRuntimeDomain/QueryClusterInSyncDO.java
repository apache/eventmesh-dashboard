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

package org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterOperationHandler;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.List;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryClusterInSyncDO {

    public static QueryClusterInSyncDO create(Long id,Supplier<BaseRuntimeIdEntity> function) {
        return create(id,null,function);
    }

    public static QueryClusterInSyncDO create(Long id , Object object) {
        return create(id,object,null);
    }

    private static QueryClusterInSyncDO create(Long id , Object object,Supplier<BaseRuntimeIdEntity> function) {
        QueryClusterInSyncDO data = new QueryClusterInSyncDO();
        data.setObject(object);
        data.setFunction(function);
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(id);
        data.setClusterEntity(data.getClusterEntity());
        return data;
    }

    private ClusterEntity clusterEntity;

    private List<ClusterType> syncClusterTypeList;

    private Object object;

    /**
     *  clusterEntity and runtimeEntity  转换  操作 entity
     */
    private Supplier<BaseRuntimeIdEntity> function;


    private ClusterOperationHandler clusterOperationHandler;

}
