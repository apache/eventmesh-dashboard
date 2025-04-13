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


import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("cluster")
public class ClusterController {

    @Autowired
    ClusterService clusterService;




    @GetMapping("queryHomeClusterData")
    public GetClusterBaseMessageVO queryHomeClusterData(@RequestBody @Validated ClusterIdDTO clusterIdDTO) {
        return clusterService.getClusterBaseMessage(clusterIdDTO);
    }



    /**
     * 这里只传递
     * @param createClusterBySimpleDataDTO
     */
    @PostMapping("createClusterByConfig")
    public void createClusterByConfig(@RequestBody CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        this.clusterService.createCluster(ClusterControllerMapper.INSTANCE.createCluster(createClusterBySimpleDataDTO));
    }

}
