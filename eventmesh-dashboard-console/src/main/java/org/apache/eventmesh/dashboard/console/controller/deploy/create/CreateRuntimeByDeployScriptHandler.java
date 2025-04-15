package org.apache.eventmesh.dashboard.console.controller.deploy.create;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.DeployStatusType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationType;
import org.apache.eventmesh.dashboard.console.controller.deploy.handler.UpdateHandler;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.cluster.ClusterControllerMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ClusterCycleControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateRuntimeByDeployScriptHandler implements UpdateHandler<CreateClusterByDeployScriptDO> {


    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void init() {

    }

    @Override
    public void handler(CreateClusterByDeployScriptDO createRuntimeByDeployScriptDO) {
        ClusterEntity clusterEntity = ClusterControllerMapper.INSTANCE.toClusterEntity(createRuntimeByDeployScriptDO);
        clusterEntity = this.clusterService.queryClusterById(clusterEntity);
        RuntimeEntity runtimeEntity = ClusterCycleControllerMapper.INSTANCE.createRuntimeByDeployScript(createRuntimeByDeployScriptDO);
        if (Objects.isNull(runtimeEntity.getDeployScriptId()) && Objects.isNull(clusterEntity.getDeployScriptId())) {
            return;
        }

        if (Objects.isNull(runtimeEntity.getResourcesConfigId()) && Objects.isNull(clusterEntity.getResourcesConfigId())) {
            return;
        }
        if (Objects.nonNull(runtimeEntity.getDeployScriptId())) {
            runtimeEntity.setDeployScriptId(createRuntimeByDeployScriptDO.getDeployScriptId());
        }

        if (Objects.nonNull(runtimeEntity.getResourcesConfigId())) {
            runtimeEntity.setResourcesConfigId(createRuntimeByDeployScriptDO.getResourcesConfigId());
        }
        ReplicationType replicationType = createRuntimeByDeployScriptDO.getReplicationType();
        Deque<Integer> linkedList = new ArrayDeque<>();
        if (!Objects.equals(replicationType, ReplicationType.SLAVE) && clusterEntity.getClusterType().isStorage()) {
            linkedList = this.clusterService.getIndex(clusterEntity);
        } else if (Objects.equals(replicationType, ReplicationType.SLAVE)) {
            linkedList.add(1);
        } else {
            linkedList.add(0);
        }

        runtimeEntity.setClusterType(clusterEntity.getClusterType());
        runtimeEntity.setTrusteeshipType(ClusterTrusteeshipType.SELF);
        runtimeEntity.setReplicationType(replicationType);
        runtimeEntity.setDeployStatusType(DeployStatusType.CREATE_WAIT);
        runtimeEntity.setIndex(linkedList.poll());
        this.runtimeService.insertRuntime(runtimeEntity);

    }

}
