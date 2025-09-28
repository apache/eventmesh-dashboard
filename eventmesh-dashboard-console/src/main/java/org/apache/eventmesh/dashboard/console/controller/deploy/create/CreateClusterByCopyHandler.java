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

package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByDeployScriptDTO;

public class CreateClusterByCopyHandler implements UpdateHandler<CreateRuntimeByDeployScriptDTO> {

    @Override
    public void init() {

    }

    @Override
    public void handler(CreateRuntimeByDeployScriptDTO createRuntimeByDeployScriptDTO) {
        // 这是所有 新增 cluster runtime 的名字的 后缀

        // 查询所有的集群

        // 查询 关系

        // 查询所有 runtime

        // 查询所有  config

        // 查询所有  topic

        // 查询所有 acl

        // 组织关系

        // 调用 cluster service
    }
}
