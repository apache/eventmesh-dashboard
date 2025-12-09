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


package org.apache.eventmesh.dashboard.console.controller.deploy.uninstall;

import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UninstallRuntimeHandler implements UpdateHandler<RuntimeEntity> {

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void init() {

    }

    @Override
    public void handler(RuntimeEntity runtimeEntity) {
        // 主从 架构 如果有从存在则不能操作主

        // 直接修改 runtime 状态就行了
        runtimeEntity.setDeployStatusType(DeployStatusType.UNINSTALL_ING);
        this.runtimeService.batchUpdateDeployStatusType(List.of(runtimeEntity));
    }
}
