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

package org.apache.eventmesh.dashboard.console.modle.deploy.active;

import org.apache.eventmesh.dashboard.console.modle.OrganizationIdDTO;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateTheEventClusterDTO extends OrganizationIdDTO {


    private CreateClusterDTO eventSpace;

    private RuntimeClusterDTO eventClusterList;

    private CreateCapStorageClusterDTO capStorageClusters;

    private RuntimeClusterDTO monomerStorageClusters;

    private MainStorageClusterDTO mainStorageClusters;

    private List<CreateRuntimeDTO> prometheusRuntime;




    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class BaseCreateTheEntireClusterDTO extends BaseCreateClusterDTO {

        private CreateRuntimeDTO prometheusRuntime;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MainClusterDTO extends BaseCreateTheEntireClusterDTO {

        private List<CreateTheEntireClusterDTO> clusterList;

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CreateCapStorageClusterDTO extends BaseCreateTheEntireClusterDTO{

        private CreateTheEntireClusterDTO brokerClusterList;

        private CreateTheEntireClusterDTO metaClusterList;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RuntimeClusterDTO extends BaseCreateTheEntireClusterDTO{

        private CreateTheEntireClusterDTO metaClusterList;

        private CreateTheEntireClusterDTO brokerClusterList;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MainStorageClusterDTO extends BaseCreateTheEntireClusterDTO{

        private List<CreateTheEntireClusterDTO> metaClusterList;

        private List<MainClusterDTO> brokerClusterList;

    }

}
