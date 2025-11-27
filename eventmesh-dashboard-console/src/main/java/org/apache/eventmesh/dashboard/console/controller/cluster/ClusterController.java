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


import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.domain.ClusterAndRuntimeDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.deploy.ClusterAllMetadataDO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.client.QueryClientByUserFormDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.BatchCreateClusterDataDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.ClusterDetailsVO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.CreateClusterByFullAddressDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryClusterByOrganizationIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryRelationClusterByClusterIdAndTypeDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.QueryTreeByClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.SimpleCreateClusterDataDTO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.ClusterTreeVO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.GetClusterBaseMessageVO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.utils.data.controller.cluster.ClusterControllerUtils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @Autowired
    private ClusterAndRuntimeDomain clusterAndRuntimeDomain;


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
     *  TODO 1. 返回全量数据
     *       2. 构建树
     *       3. 类型
     *          1. clusterType
     *          2. cluster 与 runtime 状态
     *              1. 默认显示正常，
     *              2. 是否按照状态排序
     *          3. eventmesh cluster  与 存储 cluster 是否分开 展示
     *          4. 提供获得 url 功能，在 后端进行整理，前端处理太麻烦了
     *           4. cluster 与 关系表内容是否进行关联
     *       5. cluster runtime  与 关联表 需要整理成 一个 DO，适配前端 前端 tree table
     *
     */
    @PostMapping("queryTreeByClusterId")
    public List<ClusterTreeVO> queryTreeByClusterId(@RequestBody QueryTreeByClusterIdDTO data) {
        return this.clusterAndRuntimeDomain.queryClusterTree(ClusterControllerMapper.INSTANCE.queryTreeByClusterId(data));
    }

    /**
     * 这个接口用户 cluster 对应业务的 首页，方便查询
     *
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
     */
    @PostMapping("queryRelationClusterByClusterIdAndType")
    public List<ClusterEntity> queryRelationClusterByClusterIdAndType(@RequestBody @Validated QueryRelationClusterByClusterIdAndTypeDTO dto) {
        return this.clusterService.queryRelationClusterByClusterIdAndType(
            ClusterControllerMapper.INSTANCE.queryRelationClusterByClusterIdAndType(dto));
    }


    public void updateClusterNode() {

    }

    /**
     * TODO
     *  多端口，怎么处理。
     *      - 设计多端口 url
     *          - 127.0.0.1/tcp=9898&grpc=9899&http=9900;127.0.0.2/tcp=9898&grpc=9899&http=9900
     *          - 127.0.0.1,2,3,4/tcp=9898&grpc=9899&http=9900;
     *          - 127.0.0.1,127.0.0.2,127.0.0.3,127.0.0.4/tcp=9898&grpc=9899&http=9900;
     *      - 需要设计 url 表达式？
     *      - 主动从 runtime 读取 端口信息，是否能读取 meta 信息
     *      -
     *  eventmesh 是否可以从 meta 读取 runtime 信息  or 从 runtime 读取 meta
     *  其他基础数据，怎么处理。
     * </p>
     * 适合 meta 与 runtime 弱关联的 架构。比如 eventmesh
     */
    @PostMapping("createClusterByFullAddress")
    public void createClusterByFullAddress(@RequestBody @Validated CreateClusterByFullAddressDTO data) {

    }


    /**
     * 此方法适合 AP 架构，通过 meta address 获得大量 runtime cluster。比如  RocketMQ 架构
     * <p>
     * 先创建 eventmesh 集群，在创建 rocketmq 集群
     */
    @PostMapping("createClusterByBachAddress")
    public void createClusterByBachAddress(@RequestBody BatchCreateClusterDataDTO data) {
        ClusterEntity mainClusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(data);
        ClusterControllerUtils.clusterEntityDataSupplement(mainClusterEntity);

        List<Pair<ClusterEntity, List<RuntimeEntity>>> list = new ArrayList<>();
        data.getSimpleCreateClusterDataList().forEach((value) -> {
            ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(value);
            ClusterControllerUtils.clusterEntityDataSupplement(clusterEntity);
            clusterEntity.setOrganizationId(mainClusterEntity.getOrganizationId());
            Pair<ClusterEntity, List<RuntimeEntity>> pair =
                ClusterControllerUtils.handlerSimpleCreateClusterDataDTO(clusterEntity, value);
            list.add(pair);

        });
        this.clusterService.createClusterInfo(mainClusterEntity, list, this.creatingRelation(data.getMainClusterId(),mainClusterEntity));
    }

    /**
     * 此方法适合 runtime cap 架构 或则 以 runtime 为主 ，比如 kafka
     * TODO
     *    kafka 集群 是否支持0.9 版本 到 2.8版本，因为这个版本 metadata 需要从 zookeeper 集群进行操作
     *    需要进行深度的讨论
     *    如果需要支持，那么作为 T1级以及 极难标准的任务，发布下去. 接口等模块需要支持 runtime meta 双同步操作
     */
    @PostMapping("createClusterByRuntimeAddress")
    public void createClusterByRuntimeAddress(@RequestBody @Validated SimpleCreateClusterDataDTO data) {
        ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(data);
        ClusterType mainClusterType = clusterEntity.getClusterType().getHigher();
        ClusterEntity mainClusterEntity = new ClusterEntity();
        mainClusterEntity.setOrganizationId(clusterEntity.getOrganizationId());
        mainClusterEntity.setClusterType(mainClusterType);
        mainClusterEntity.setName(clusterEntity.getName());
        mainClusterEntity.setTrusteeshipType(ClusterTrusteeshipType.NOT);
        ClusterControllerUtils.clusterEntityDataSupplement(mainClusterEntity);

        clusterEntity.setName(clusterEntity.getName() + "-broker");
        Pair<ClusterEntity, List<RuntimeEntity>> pair =
            ClusterControllerUtils.handlerSimpleCreateClusterDataDTO(clusterEntity, data);
        // runtime cap 架构需要 检查节点数量，
        this.clusterService.createClusterInfo(mainClusterEntity, List.of(pair), this.creatingRelation(data.getMainClusterId(),mainClusterEntity));

    }

    private ClusterRelationshipEntity creatingRelation(Long mainClusterId, ClusterEntity mainClusterEntity) {
        if (Objects.isNull(mainClusterId)) {
            return null;
        }
        ClusterEntity newClusterEntity = new ClusterEntity();
        newClusterEntity.setId(mainClusterId);
        newClusterEntity = this.clusterService.queryClusterById(newClusterEntity);
        ClusterRelationshipEntity clusterRelationshipEntity = clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterRelationshipEntity.setOrganizationId(newClusterEntity.getOrganizationId());
        clusterRelationshipEntity.setClusterId(newClusterEntity.getId());
        clusterRelationshipEntity.setClusterType(newClusterEntity.getClusterType());
        clusterRelationshipEntity.setRelationshipType(mainClusterEntity.getClusterType());
        return clusterRelationshipEntity;
    }

}
