package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.AbstractGetBuildDataExampleHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.alibaba.fastjson.JSON;

public abstract class AbstractCreateExampleHandler<T> extends AbstractGetBuildDataExampleHandler<T> {

    protected List<RuntimeEntity> runtimeEntityList = new ArrayList<>();

    protected ClusterEntity clusterEntity;

    protected String metaAddress;

    protected ClusterEntity k8sClusterEntity;


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

    protected void createRuntimeEntity(ReplicationType replicationType, Integer index) {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(clusterEntity.getId());
        runtimeEntity.setClusterType(clusterEntity.getClusterType());
        runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        runtimeEntity.setFirstToWhom(clusterEntity.getFirstToWhom());
        runtimeEntity.setFirstSyncState(clusterEntity.getFirstSyncState());
        runtimeEntity.setSyncErrorType(clusterEntity.getSyncErrorType());
        runtimeEntity.setReplicationType(replicationType);
        runtimeEntity.setIndex(index);
        this.runtimeEntityList.add(runtimeEntity);
    }


    public void createExample() {
        List<HasMetadata> hasMetadataList = new ArrayList<>();
        this.runtimeEntityList.forEach(entity -> {
            Map<String, Object> dataMap = this.createYamlData(entity);
            String yamlData = this.createK8sYaml(dataMap);
            //  List<HasMetadata> hasMetadata = kubernetesClient.load(yamlData).get();
            List<HasMetadata> hasMetadata = kubernetesClient.resourceList(yamlData).get();
            hasMetadataList.addAll(hasMetadata);
        });
        kubernetesClient.resourceList(hasMetadataList).create();
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
}
