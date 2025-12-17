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

package org.apache.eventmesh.dashboard.console.utils.data.controller.agent;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.spring.support.address.AddressServiceIPDO;

import java.util.List;
import java.util.stream.Collectors;

public class AgentActionControllerUtils {


    public static AddressServiceIPDO byRuntimeList(List<RuntimeEntity> runtimeEntities) {
        AddressServiceIPDO addressServiceData = new AddressServiceIPDO();
        addressServiceData.setRuntimeEntityList(runtimeEntities);
        return addressServiceData;
    }

    public static AddressServiceIPDO byClusterAndRuntimeOfRelationshipDO(ClusterAndRuntimeOfRelationshipDO data) {
        AddressServiceIPDO addressServiceData = new AddressServiceIPDO();
        addressServiceData.setClusterTypeMapMap(data.getRuntimeEntityList().stream().collect(Collectors.groupingBy(RuntimeEntity::getClusterType)));
        return addressServiceData;
    }

}

