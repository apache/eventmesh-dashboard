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

package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.model.deploy.create.CreateClusterByFullMetadataDTO;

import org.springframework.stereotype.Component;

/**
 * TODO
 *  实现这个 handler 需要 对 ClusterAndRuntimeDomainImpl GetSyncObjectHandler 的 数据读取 与 数据整理 进行 解耦。</p>
 *  一个是 db read ， 一个 get read
 *  issue Full metadata 创建 create cluster 以及 GetSyncObjectHandler 数据读取与数据操作解耦，支持 db read 与 get read
 */
@Component
public class CreateClusterByFullMetadataHandler implements UpdateHandler<CreateClusterByFullMetadataDTO> {


    @Override
    public void init() {

    }

    @Override
    public void handler(CreateClusterByFullMetadataDTO dto) {
        // 这是所有 新增 cluster runtime 的名字的 后缀

        // 通过 json 进行序列化，得到所有的配置 cluster runtime relationship config topic 等

        // 然后进行关联

        // 调用 cluster service 进行写入
    }
}
