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
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 1. 用户首页列表
 * 2. 集群首页概要
 */
@RestController
@RequestMapping("cluster")
public class ClusterController {

    @Autowired
    ClusterService clusterService;

    @GetMapping("queryHomeClusterData")
    public GetClusterBaseMessageVO queryHomeClusterData(@RequestBody @Validated ClusterIdDTO clusterIdDTO) {
        return clusterService.selectClusterBaseMessage(clusterIdDTO.getClusterId());
    }


    @PostMapping("createCluster")
    public void createCluster(@RequestBody CreateClusterDTO createClusterDTO) {
        this.clusterService.createCluster(ClusterControllerMapper.INSTANCE.createCluster(createClusterDTO));
    }

    /**
     * 那些集群可以暂停。被依赖的集群不允许暂停。暂停的含义是什么 暂停是否释放资源
     *
     * @return
     */
    public Integer pauseCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖
        return null;
    }

    /**
     * 重新开始集群
     *
     * @return
     */
    public Integer resumeCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖
        return null;
    }

    /**
     * 注销集群
     *
     * @return
     */
    public Integer cancelCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖

        // 如果是全程托管，释放k8s 集群
        return null;
    }



}
