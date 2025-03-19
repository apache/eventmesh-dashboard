package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterByCompleteDataDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class CreateCycleService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterRelationshipService clusterRelationshipService;

    private ClusterEntity clusterEntity;


    private void verifyName() {
        if (!this.clusterService.nameExist(this.clusterEntity)) {
            //
        }
    }

    private void createDevOps() {

    }

    private void createCLusterEntity() {

    }

    private void createRuntimeEntity() {

    }

    private void createRuntimeConfig() {

    }

    private void persistenceData() {

    }

    private void lockPost() {
        // 获得端口

        //
    }

    private void checkResource() {

    }

    private void lockResource() {

    }


    private void createInstance() {
        // 然后通知，检查
    }


    private void createCluster(Long configGatherId) {
        this.verifyName();
        this.clusterService.createCluster(clusterEntity);
        if (Objects.nonNull(configGatherId)) {
            this.configService.copyConfig(configGatherId, clusterEntity.getId());
        }
    }

    public void createClusterBySimpleData(CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        clusterEntity = ClusterControllerMapper.INSTANCE.createCluster(createClusterBySimpleDataDTO);
        this.createCluster(createClusterBySimpleDataDTO.getConfigGatherId());
    }

    public void createClusterByCompleteData(CreateClusterByCompleteDataDTO createClusterByCompleteDataDTO) {
        // 创建 部署流程
        // 集群名字是否存在
        this.createClusterBySimpleData(createClusterByCompleteDataDTO);
        // 得到所有集群信息
        createClusterByCompleteDataDTO.getRuntimeCLuster();

        createClusterByCompleteDataDTO.getMetadataCLuster();

        createClusterByCompleteDataDTO.getStorageCLuster();
        // 校验集群相关数据
        // 生成本次集群数据
        List<ClusterRelationshipEntity> clusterRelationshipEntityList = Collections.emptyList();
        this.clusterRelationshipService.addClusterRelationshipEntry(clusterRelationshipEntityList);
    }

    public void createClusterByConfigData(CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        // 创建 部署流程
        // 集群名字是否存在
        // 创建 config 里面所有数据
        // 检查资源
        // 锁定资源
        // 锁定 端口
        // 组个调用 kubernetes 创建资源
        // 检查
        // 失败，释放资源，端口，删除 kubernetes内容
    }

    public void registerRuntime(RuntimeEntity runtimeEntity) {
        this.runtimeService.insertRuntime(runtimeEntity);
    }

    public void createRuntime() {
        // 创建 部署流程

        // 创建 runtime

        // 判断是否 有模板

        // 获得 cluster 默认模板

        // 创建 config 里面所有数据
        // 持久化数据
        // 检查资源
        // 锁定资源
        // 需要一个被 console 管理的端口，所以要 锁定 端口
        //
    }
}
