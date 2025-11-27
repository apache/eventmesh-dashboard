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

package org.apache.eventmesh.dashboard.console.model.DO;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import java.util.List;

import javax.validation.Valid;

import lombok.Data;

@Data
@Deprecated
public class CreateTheEventClusterDO {



    private ClusterEntity eventSpace;

    private MainStorageClusterDO eventCluster;

    private CreateCapStorageClusterDO capStorageClusters;

    private List<MainStorageClusterDO> mainStorageClusters;


    @Data
    public static class CreateCapStorageClusterDO {

        private ClusterEntity brokerCluster;

        private CreateTheEntireClusterDO runtimeCluster;

        private CreateTheEntireClusterDO metaCluster;

        private RuntimeEntity prometheusRuntime;
    }

    @Data
    public static class  MainStorageClusterDO{

        private ClusterEntity cluster;

        private CreateTheEntireClusterDO metaClusterList;

        private CreateTheEntireClusterDO brokerClusterList;

        private RuntimeEntity prometheusRuntime;
    }

    @Data
    public static class CreateTheEntireClusterDO {

        @Valid
        private ClusterEntity createClusterDTO;

        @Valid
        private List<RuntimeEntity> createRuntimeDTOList;

    }
}
