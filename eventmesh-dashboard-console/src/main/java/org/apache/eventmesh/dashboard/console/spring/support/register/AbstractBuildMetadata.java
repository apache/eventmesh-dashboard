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

import org.apache.eventmesh.dashboard.common.port.PortValidate;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import java.util.List;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractBuildMetadata implements BuildMetadata {

    private PortValidate portValidate;


    protected void setPortValidate(PortValidate portValidate) {
        this.portValidate = portValidate;
    }

    @Override
    public PortValidate portValidate() {
        return this.portValidate;
    }

    public char separator() {
        return ';';
    }


    public String buildRuntime(RuntimeEntity target, RuntimeEntity runtimeEntity) {
        if (Objects.equals(target.getKubernetesClusterId(), runtimeEntity.getKubernetesClusterId())) {
            return runtimeEntity.getHost() + ":" + runtimeEntity.getPort() + this.separator();
        } else {
            return runtimeEntity.getHost() + ":" + runtimeEntity.getPort() + this.separator();
        }
    }

    public void standardRuntime(ScriptBuildData data, RuntimeEntity target, List<RuntimeEntity> runtimeEntityList) {
        StringBuffer nameService = new StringBuffer();
        runtimeEntityList.forEach(runtimeEntity -> {
            nameService.append(this.buildRuntime(target, runtimeEntity));
        });
        data.put("metaService", nameService.toString());
    }

    public void generatePropertiesConfig(ScriptBuildData data) {
        StringBuffer config = new StringBuffer();
        data.getConfigEntityList().forEach(configEntity -> {
            config.append(configEntity.getConfigName()).append("=").append(configEntity.getConfigValue()).append(System.lineSeparator());
        });
        String runtimeConfig = config.toString();
        data.put("runtimeConfig", runtimeConfig);
    }
}
