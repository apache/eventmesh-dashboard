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
import org.apache.eventmesh.dashboard.common.port.PortValidate;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.spring.support.register.ScriptBuildData;

import java.util.List;
import java.util.Objects;

public class RocketMQBuildMetadata extends AbstractRuntimeBuildMetadata {

    {
        PortValidate portValidate = new RocketMQBrokerPortValidate();
        this.setPortValidate(portValidate);
    }

    @Override
    public List<ClusterType> runtimeTypes() {
        return List.of(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE, ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT);
    }


    @Override
    public void buildMetaAddress(ScriptBuildData data, RuntimeEntity target, List<RuntimeEntity> runtimeMetadataList) {
        this.standardRuntime(data, target, runtimeMetadataList);
    }

    @Override
    public void buildConfig(ScriptBuildData data, RuntimeEntity target) {

    }

    static class RocketMQBrokerPortValidate extends AbstractPortValidate {


        {
            PortRule portRule = PortRule.builder().valid(true).index(1).name("fastPort").build();
            this.setPortRules(portRule);
            portRule = PortRule.builder().valid(true).index(2).name("nullPort").spanValue(1).build();
            this.setPortRules(portRule);
            portRule = PortRule.builder().valid(true).index(3).name("port").spanValue(1).build();
            this.setPortRules(portRule);
        }

        @Override
        public boolean validate(PortRule lastPortRule, Integer lastPort, PortRule current, Integer port, List<PortRule> portRules) {
            if (Objects.equals(lastPortRule.getIndex(), 1)) {
                return true;
            }
            return true;
        }
    }
}
