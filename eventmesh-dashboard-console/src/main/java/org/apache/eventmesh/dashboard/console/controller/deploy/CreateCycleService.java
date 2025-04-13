package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.common.enums.SyncErrorType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterByCompleteDataDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.alibaba.fastjson.JSON;

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


    public interface CreateHandler {

        void init();

        void handler();

    }

    private abstract class AbstractetadataExampleHandler implements CreateHandler {

        protected ReplicationType replicationType;

        protected ClusterType clusterType;

        protected ClusterFramework clusterFramework;

        protected ClusterEntity k8sClusterEntity;

        protected KubernetesClient kubernetesClient;


        protected  void handlerMetadata(ClusterEntity clusterEntity) {
            this.clusterType = clusterEntity.getClusterType();
            this.replicationType = clusterEntity.getReplicationType();
            this.clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(this.clusterType);

        }

    }

    private abstract class AbstractBuildDataExampleHandler extends AbstractetadataExampleHandler {

        protected ResourcesConfigEntity resourcesConfigEntity;

        protected DeployScriptEntity deployScriptEntity;

        protected List<ConfigEntity> configEntityList;
    }

    public abstract class AbstractCreateExampleHandler extends AbstractBuildDataExampleHandler {

        protected List<RuntimeEntity> runtimeEntityList = new ArrayList<>();

        protected ClusterEntity clusterEntity;

        protected String metaAddress;


        protected ClusterEntity k8sClusterEntity;


        public void init() {
            ConfigEntity configEntity = new ConfigEntity();
            configEntityList = configService.queryByClusterAndInstanceId(configEntity);
            ClusterEntity k8sClusterEntity = new ClusterEntity();
            this.k8sClusterEntity = clusterService.queryClusterById(k8sClusterEntity);
            ClusterMetadata clusterMetadata = new ClusterMetadata();
            clusterMetadata.setId(k8sClusterEntity.getId());
            clusterMetadata.setClusterType(k8sClusterEntity.getClusterType());
            this.kubernetesClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, clusterMetadata.getUnique());

            clusterMetadataDomain.getMetaColonyDO(configEntity.getId());
        }


        public void handler() {

        }


        /**
         * 端口
         * <p>
         * namespace， 是 大集群id，不是 ceventmesh
         * <p>
         * 标签
         * <p>
         * 注册地址
         * <p>
         * 资源
         */
        public String createK8sYaml(Map<String, Object> yaml) {
            String content = this.deployScriptEntity.getContent();
            yaml.forEach((key, value) -> {
                content.replace("{" + key + "}", value.toString());
            });
            return content;
        }

        /**
         * 批量
         *
         * @param runtimeEntity
         */
        public void createExample(RuntimeEntity runtimeEntity) {
            List<HasMetadata> hasMetadataList = new ArrayList<>();
            this.runtimeEntityList.forEach(entity -> {
                Map<String, Object> dataMap = this.createYamlData(runtimeEntity);
                String yamlData = this.createK8sYaml(dataMap);
                //  List<HasMetadata> hasMetadata = kubernetesClient.load(yamlData).get();
                List<HasMetadata> hasMetadata = kubernetesClient.resourceList(yamlData).get();
                hasMetadataList.addAll(hasMetadata);
            });
            kubernetesClient.resourceList(hasMetadataList).create();
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
        public Map<String, Object> createYamlData(RuntimeEntity runtimeEntity) {
            Map<String, Object> yaml = new HashMap<>();
            // 资源
            yaml.putAll((Map<String, Object>) JSON.toJSON(this.resourcesConfigEntity));

            // kubernetes metadata
            yaml.put("namespace", runtimeEntity.getClusterId());
            yaml.put("name", runtimeEntity.getId());

            // 注册中心， 每次更新，都需要逐一修改runtime，重启
            yaml.put("metaAddress", this.metaAddress);

            return yaml;
        }

        protected void createClusterEntity() {

        }

        protected void createRuntimeEntity(ReplicationType replicationType) {
            RuntimeEntity runtimeEntity = new RuntimeEntity();
            runtimeEntity.setClusterId(clusterEntity.getId());
            runtimeEntity.setClusterType(clusterEntity.getClusterType());
            runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
            runtimeEntity.setFirstToWhom(clusterEntity.getFirstToWhom());
            runtimeEntity.setFirstSyncState(clusterEntity.getFirstSyncState());
            runtimeEntity.setSyncErrorType(clusterEntity.getSyncErrorType());
            runtimeEntity.setReplicationType(replicationType);
            this.runtimeEntityList.add(runtimeEntity);
        }
    }

    public class CreateRuntimeData extends CreateHandler {

        @Override
        public void init() {

        }

        @Override
        public void handler() {

        }
    }

    public class CreateRuntime extends AbstractCreateExampleHandler {

        @Override
        public void handler() {
            ClusterEntity clusterEntity = new ClusterEntity();
            clusterEntity.setId(0L);
            this.clusterEntity = clusterService.queryClusterById(clusterEntity);

            DeployScriptEntity deployScriptEntity = new DeployScriptEntity();
            deployScriptEntity.setId(0L);
            this.deployScriptEntity = deployScriptService.queryById(deployScriptEntity);
            ResourcesConfigEntity resourcesConfigEntity = new ResourcesConfigEntity();

            this.resourcesConfigEntity = resourcesConfigService.queryResourcesById(resourcesConfigEntity);
            this.createRuntimeEntity(ReplicationType.NOT);

            this.createExample(this.runtimeEntityList.get(0));

            // 操作成功，修改 runtime，cluster 配置
        }

    }

    public class CreateClusterData extends CreateHandler {

        public ClusterEntity createClusterBySimpleData(CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
            clusterEntity = ClusterControllerMapper.INSTANCE.createCluster(createClusterBySimpleDataDTO);

            return clusterEntity;
        }

        @Override
        public void init() {

        }

        @Override
        public void handler() {

        }
    }

    public class CreateCluster extends AbstractCreateExampleHandler {

    }

    public class CreateMainSlaveCluster extends AbstractCreateExampleHandler {

    }
}
