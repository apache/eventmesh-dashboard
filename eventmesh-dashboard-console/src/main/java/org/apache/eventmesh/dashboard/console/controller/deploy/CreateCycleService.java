package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterByCompleteDataDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@Slf4j
public class CreateCycleService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ClusterRelationshipService clusterRelationshipService;

    @Autowired
    private ResourcesConfigService resourcesConfigService;

    @Autowired
    private DeployScriptService deployScriptService;


    @Autowired
    private ClusterMetadataDomain clusterMetadataDomain;

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

    public ClusterEntity createClusterBySimpleData(CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        clusterEntity = ClusterControllerMapper.INSTANCE.createCluster(createClusterBySimpleDataDTO);
        this.createCluster(createClusterBySimpleDataDTO.getConfigGatherId());
        return clusterEntity;
    }

    public void createClusterByCompleteData(CreateClusterByCompleteDataDTO createClusterByCompleteDataDTO) {
        // 对进群进行判断，kafka broker 集群，只能有一个
        // eventmesh 集群，目前只支持一个 meta， 一个runtime, 一个存储。

        // 创建 部署流程
        // 集群名字是否存在
        ClusterEntity clusterEntity = this.createClusterBySimpleData(createClusterByCompleteDataDTO);
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(createClusterByCompleteDataDTO.getClusterType());
        if (clusterFramework.isMainSlave()) {
            this.createMainSlave(createClusterByCompleteDataDTO);
        } else {
            this.createNotMainSlave(createClusterByCompleteDataDTO);
        }
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

    private void createNotMainSlave(CreateClusterByCompleteDataDTO createClusterByCompleteDataDTO) {
        ClusterEntity clusterEntity = this.createClusterBySimpleData(createClusterByCompleteDataDTO);
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        for (int i = 0; i < createClusterByCompleteDataDTO.getCreateNum(); i++) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterEntity.getId());
            runtimeEntity.setClusterType(clusterEntity.getClusterType());
            runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
            runtimeEntity.setFirstToWhom(clusterEntity.getFirstToWhom());
            runtimeEntity.setFirstSyncState(clusterEntity.getFirstSyncState());
            runtimeEntity.setSyncErrorType(clusterEntity.getSyncErrorType());
            runtimeEntity.setReplicationType(ReplicationType.NOT);
            runtimeEntityList.add(runtimeEntity);
        }
    }

    public void createMainSlave(CreateClusterByCompleteDataDTO createClusterByCompleteDataDTO) {
        List<ClusterEntity> clusterEntityList = new ArrayList<>();
        for (int i = 0; i < createClusterByCompleteDataDTO.getCreateNum(); i++) {
            ClusterEntity newCluster = ClusterControllerMapper.INSTANCE.createCluster(createClusterByCompleteDataDTO);
            newCluster.setTrusteeshipType(ClusterTrusteeshipType.SELF);
            newCluster.setFirstToWhom(FirstToWhom.COMPLETE);
            newCluster.setFirstSyncState(FirstToWhom.COMPLETE);
            newCluster.setSyncErrorType(SyncErrorType.NOT);
            newCluster.setReplicationType(ReplicationType.NOT);
            clusterEntityList.add(newCluster);
        }
        List<RuntimeEntity> runtimeEntityList = new ArrayList<>();
        clusterEntityList.forEach(entity -> {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(entity.getId());
            runtimeEntity.setClusterType(entity.getClusterType());
            runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
            runtimeEntity.setFirstToWhom(entity.getFirstToWhom());
            runtimeEntity.setFirstSyncState(entity.getFirstSyncState());
            runtimeEntity.setSyncErrorType(entity.getSyncErrorType());
            runtimeEntity.setReplicationType(ReplicationType.MAIN);

            runtimeEntityList.add(runtimeEntity);
            if (Objects.equals(createClusterByCompleteDataDTO.getReplicationType(), ReplicationType.MAIN_SLAVE)) {
                runtimeEntity = new RuntimeEntity();
                runtimeEntity.setClusterId(entity.getId());
                runtimeEntity.setClusterType(entity.getClusterType());
                runtimeEntity.setTrusteeshipType(entity.getTrusteeshipType());
                runtimeEntity.setFirstToWhom(entity.getFirstToWhom());
                runtimeEntity.setFirstSyncState(entity.getFirstSyncState());
                runtimeEntity.setSyncErrorType(entity.getSyncErrorType());
                runtimeEntity.setReplicationType(ReplicationType.SLAVE);
                runtimeEntityList.add(runtimeEntity);
            }
        });
        runtimeEntityList.forEach(entity -> {
            ClusterType clusterType = entity.getClusterType();
            Map<String, Object> portMap = new HashMap<>();

            portMap.forEach((key, value) -> {

            });
        });


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


    // TODO  添加修改绑定解绑 meta 集群，都需要 所有的 runtime集群。是通过事件方式处理还是立即处理？
    // IP 管理
    // 配置写入，
    // 可动态修改配置（可以在线修改），
    // 不可能动态修改配置,修改 yaml 重新启动容器，是否支持灰度修改
    // 配置末班， a 配置 1， 变成2
    // meta deployment
    // 存储 StatefulSet 暴露端口 hostIP
    // 是否直接暴露端口， 是否使用 service。 每次只能穿件一个 实例
    //  1. jmx
    //  2. nameservice 负载不太均匀问题
    //  3. eventmesh runtime admin
    //  broker + queue
    // 磁盘，


}
