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


import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.client.QueryClientByUserFormDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.cluster.ClusterDetailsVO;
import org.apache.eventmesh.dashboard.console.modle.cluster.cluster.QueryClusterByOrganizationIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.cluster.QueryRelationClusterByClusterIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.ClusterAllMetadataDO;
import org.apache.eventmesh.dashboard.console.modle.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("user/cluster")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    public RuntimeService runtimeService;


    @GetMapping("queryHomeClusterData")
    public GetClusterBaseMessageVO queryHomeClusterData(@RequestBody @Validated ClusterIdDTO clusterIdDTO) {
        return clusterService.getClusterBaseMessage(clusterIdDTO);
    }

    @PostMapping("queryClusterDetails")
    public ClusterDetailsVO queryClusterDetails(@RequestBody @Validated ClusterIdDTO clusterIdDTO) {
        ClusterEntity clusterEntity = new ClusterEntity();
        // eventmesh 集群详情
        // 基本统计信息
        // 部署信息，巡查信息
        // meta 列表， runtime 列表  存储列表
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterIdDTO.getClusterId());
        CompletableFuture<ClusterAllMetadataDO> completableFuture =
            CompletableFuture.supplyAsync(() -> this.runtimeService.queryAllByClusterId(runtimeEntity, true, false));

        // 存储集群详情
        // meta 集群详情
        // runtime集群详情
        ClusterDetailsVO clusterDetailsVO = new ClusterDetailsVO();
        return clusterDetailsVO;
    }


    @PostMapping("queryClusterByUserForm")
    public List<ClientEntity> queryClusterByUserForm(QueryClientByUserFormDTO queryClientByUserFormDTO) {
        return null;
    }

    /**
     * 这个接口用户 cluster 对应业务的 首页，方便查询
     *
     * @param dto
     * @return
     */
    @PostMapping("queryVisualizationClusterByOrganizationIdAndType")
    public List<ClusterEntity> queryVisualizationClusterByOrganizationIdAndType(@RequestBody @Validated QueryClusterByOrganizationIdAndTypeDTO dto) {
        return this.clusterService.queryClusterByOrganizationIdAndType(ClusterControllerMapper.INSTANCE.queryClusterByOrganizationIdAndType(dto));
    }

    @PostMapping("queryClusterByOrganizationIdAndType")
    public List<ClusterEntity> queryClusterByOrganizationIdAndType(@RequestBody @Validated QueryClusterByOrganizationIdAndTypeDTO dto) {
        return this.clusterService.queryClusterByOrganizationIdAndType(ClusterControllerMapper.INSTANCE.queryClusterByOrganizationIdAndType(dto));
    }

    /**
     * 查询 cluster 的关联集群列表
     *
     * @param dto
     * @return
     */
    @PostMapping("queryRelationClusterByClusterIdAndType")
    public List<ClusterEntity> queryRelationClusterByClusterIdAndType(@RequestBody @Validated QueryRelationClusterByClusterIdAndTypeDTO dto) {
        return this.clusterService.queryRelationClusterByClusterIdAndType(
            ClusterControllerMapper.INSTANCE.queryRelationClusterByClusterIdAndType(dto));
    }


}
