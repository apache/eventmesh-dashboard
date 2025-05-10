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

package org.apache.eventmesh.dashboard.console.spring.support.register.runtime;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.port.AbstractPortValidate;
import org.apache.eventmesh.dashboard.common.port.PortRule;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.spring.support.register.ScriptBuildData;

import java.util.List;
import java.util.Objects;

public class EventMethRuntimeBuildMetadata extends AbstractRuntimeBuildMetadata {

    {
        EventmeshPortValidate runTimeValidate = new EventmeshPortValidate();
        setPortValidate(runTimeValidate);
    }

    @Override
    public List<ClusterType> runtimeTypes() {
        return List.of(ClusterType.EVENTMESH_RUNTIME);
    }


    @Override
    public void buildMetaAddress(ScriptBuildData data, RuntimeEntity target, List<RuntimeEntity> runtimeMetadataList) {
        ClusterType clusterType = runtimeMetadataList.get(0).getClusterType();
        if (clusterType.isEventMethMeta()) {
            StringBuffer nameService = new StringBuffer();
            runtimeMetadataList.forEach(runtimeMetadata -> {
                if (Objects.equals(target.getKubernetesClusterId(), runtimeMetadata.getKubernetesClusterId())) {
                    nameService.append(runtimeMetadata.getHost()).append(":").append(runtimeMetadata.getPort()).append(";");
                } else {
                    nameService.append(runtimeMetadata.getHost()).append(":").append(runtimeMetadata.getPort()).append(";");
                }
            });
            data.put("metaService", nameService.toString());
        }
        if (clusterType.isStorageMeta()) {
            //
        }
    }

    @Override
    public void buildConfig(ScriptBuildData data, RuntimeEntity target) {
        this.generatePropertiesConfig(data);
    }

    static class EventmeshPortValidate extends AbstractPortValidate {


        {
            this.createNotValid(List.of("tcpPort", "httpPort", "grpcPort", "adminPort"));
        }

        @Override
        public boolean validate(PortRule lastPortRule, Integer lastPort, PortRule current, Integer port, List<PortRule> portRules) {
            return true;
        }
    }
}
