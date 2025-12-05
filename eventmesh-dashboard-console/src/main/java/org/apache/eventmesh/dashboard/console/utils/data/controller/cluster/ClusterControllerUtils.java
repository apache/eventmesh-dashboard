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

package org.apache.eventmesh.dashboard.console.utils.data.controller.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterOwnType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.model.entity.RuntimeEntityMapstruct;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.SimpleCreateClusterDataDTO;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.dubbo.common.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

public class ClusterControllerUtils {


    public static void clusterEntityDataSupplement(ClusterEntity clusterEntity) {
        clusterEntity.setReplicationType(ReplicationType.NOT);
        clusterEntity.setClusterOwnType(ClusterOwnType.INDEPENDENCE);
        clusterEntity.setConfig("");
        clusterEntity.setResourcesConfigId(0L);
        clusterEntity.setDeployScriptId(0L);
        clusterEntity.setDeployStatusType(DeployStatusType.SETTLE);
        clusterEntity.setDeployScriptVersion("Not");
        clusterEntity.setAuthType("Not");
        clusterEntity.setJmxProperties("1");
        if (Objects.isNull(clusterEntity.getTrusteeshipType())) {
            clusterEntity.setTrusteeshipType(ClusterTrusteeshipType.NOT);
        }
        if (Objects.isNull(clusterEntity.getFirstToWhom())) {
            clusterEntity.setFirstToWhom(FirstToWhom.NOT);
        }
    }


    public static Pair<ClusterEntity, List<RuntimeEntity>> handlerSimpleCreateClusterDataDTO(ClusterEntity clusterEntity,
        SimpleCreateClusterDataDTO data) {

        String address = data.getAddress();
        String[] addressArray = StringUtils.split(address, ",");
        URL url = URL.valueOf(addressArray[addressArray.length - 1]);
        int port = url.getParameter("tcp", url.getPort());

        if (Objects.nonNull(data.getDiscoverRuntimeTrusteeshipType()) && Objects.nonNull(data.getDiscoverRuntimeFirstToWhom())) {
            Map<String, Object> map = new HashMap<>();
            map.put("discoverRuntimeTrusteeshipType", data.getDiscoverRuntimeTrusteeshipType());
            map.put("discoverRuntimeFirstToWhom", data.getDiscoverRuntimeFirstToWhom());
            clusterEntity.setConfig(JSON.toJSONString(map));
        }

        List<RuntimeEntity> runtimesList = new ArrayList<>();
        Arrays.stream(addressArray).spliterator().forEachRemaining((value) -> {
            RuntimeEntity runtimeEntity = RuntimeEntityMapstruct.INSTANCE.byClusterEntity(clusterEntity);
            runtimesList.add(runtimeEntity);
            URL newUrl = URL.valueOf(value);
            runtimeEntity.setHost(newUrl.getHost());
            runtimeEntity.setPort(newUrl.getPort() >= 0 ? port : newUrl.getPort());
            runtimeEntity.setCreateScriptContent("");
            runtimeEntity.setKubernetesClusterId(0L);
            if (Objects.nonNull(newUrl.getParameter("tcp"))) {
                runtimeEntity.setPort(Integer.parseInt(newUrl.getParameter("tcp")));
            }
            if (Objects.nonNull(data.getJmxPort())) {
                runtimeEntity.setJmxPort(Integer.parseInt(data.getJmxPort()));
            } else {
                runtimeEntity.setJmxPort(0);
            }
        });
        return Pair.of(clusterEntity, runtimesList);
    }
}
