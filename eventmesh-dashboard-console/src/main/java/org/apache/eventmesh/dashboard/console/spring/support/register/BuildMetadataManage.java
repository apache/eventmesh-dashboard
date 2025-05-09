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

package org.apache.eventmesh.dashboard.console.spring.support.register;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.port.PortValidate;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BuildMetadataManage {

    private final Map<ClusterType, BuildMetadata> buildMetadataMap = new HashMap<>();

    {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(BuildMetadata.class);
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(BuildMetadata.class).subPath("/**").interfaceSet(interfaceSet).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(this::register);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void register(Class<?> clazz) {
        try {
            BuildMetadata buildMetadata = (BuildMetadata) clazz.getConstructor().newInstance();
            buildMetadata.runtimeTypes().forEach(clusterType -> {
                buildMetadataMap.put(clusterType, buildMetadata);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BuildMetadata getBuildMetaAddress(ClusterType clusterType) {
        BuildMetadata buildMetadata = this.buildMetadataMap.get(clusterType);
        if (Objects.isNull(buildMetadata)) {
            buildMetadata = this.buildMetadataMap.get(clusterType.getAssemblyBusiness());
        }
        return buildMetadata;
    }


    public void buildMetaAddress(ScriptBuildData data, RuntimeEntity target, List<RuntimeEntity> runtimeMetadataList) {
        this.getBuildMetaAddress(target.getClusterType()).buildMetaAddress(data, target, runtimeMetadataList);
    }

    public void buildConfig(ScriptBuildData data, RuntimeEntity target) {
        this.getBuildMetaAddress(target.getClusterType()).buildConfig(data, target);
    }

    public PortValidate getPortValidate(RuntimeEntity target) {
        return this.getBuildMetaAddress(target.getClusterType()).portValidate();
    }

}
