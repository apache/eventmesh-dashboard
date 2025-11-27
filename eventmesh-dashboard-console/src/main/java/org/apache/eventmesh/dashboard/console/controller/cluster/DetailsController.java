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

package org.apache.eventmesh.dashboard.console.controller.cluster;

import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organization/")
public class DetailsController {


    @PostMapping("/details/eventMesh")
    public void eventMeshDetails(ClusterIdDTO clusterIdDTO) {
        // eventmesh 集群详情
        // 基本统计信息
        // 部署信息，巡查信息
        // meta 列表， runtime 列表  存储列表
        // 存储集群详情
        // meta 集群详情
        // runtime集群详情
    }

    @PostMapping("/details/meta")
    public void metaDetails(ClusterIdDTO clusterIdDTO) {

    }

    @PostMapping("/details/storage")
    public void storageDetails(ClusterIdDTO clusterIdDTO) {

    }

    @PostMapping("/details/storage/runtime")
    public void storageRuntimeDetails(ClusterIdDTO clusterIdDTO) {

    }



}
