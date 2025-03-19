package org.apache.eventmesh.dashboard.console.domain;

import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.dto.operation.OperationBaseDTO;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.cluster.RuntimeService;
import org.apache.eventmesh.dashboard.console.service.function.ConfigService;
import org.apache.eventmesh.dashboard.console.service.message.GroupService;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 可以得到返回的数据为两种： 1. 写入数据的 2. 直接请求其他组件
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class OperationRangeDomain {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private GroupService groupService;

    @Autowired
    private TopicService topicService;


    @Autowired
    private ConfigService configService;

    private ClusterEntity clusterEntity;

    private OperationRangeDomainDataHandler rangeDomainDataHandler;

    public <T> T hander(OperationBaseDTO operationBaseDTO, OperationRangeDomainDataHandler rangeDomainDataHandler, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        this.rangeDomainDataHandler = rangeDomainDataHandler;
        this.clusterEntity = new ClusterEntity();
        clusterEntity = clusterService.selectClusterById(clusterEntity);

        OperationRangeType operationRangeType = operationBaseDTO.getMetadataOperationType().getOperationRangeType(clusterEntity.getClusterType());
        if (Objects.equals(operationRangeType, OperationRangeType.ONCE_CLUSTER)) {
            //
        } else if (Objects.equals(operationRangeType, OperationRangeType.ALL_RUNTIME)) {
            //
        } else if (Objects.equals(operationRangeType, OperationRangeType.RANGE_RUNTIME)) {
            //
        }

        switch (operationRangeType) {
            case CLUSTER:
                this.cluster();
                break;
            case RANGE_RUNTIME:
                this.runtime();
                break;
            case GROUP:
            case TOPIC:
            case SUBSCRIBER:
            case QUEUE:
            default:

        }
        return (T) list;
    }

    private void cluster() {

        // 查询集群所有
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(this.clusterEntity.getId());

        List<RuntimeEntity> runtimeEntityList =
            this.clusterEntity.getClusterType().isDefinition() ? runtimeService.getRuntimeByClusterRelationship(runtimeEntity)
                : runtimeService.getRuntimeToFrontByClusterId(runtimeEntity);
        runtimeEntityList.forEach((value) -> {

        });
    }

    private void runtime() {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(this.clusterEntity.getId());
        List<RuntimeEntity> runtimeEntityList = runtimeService.getRuntimeToFrontByClusterId(runtimeEntity);
        runtimeEntityList.forEach((value) -> {

        });
    }

    private void mainSalve() {

    }


    private void group() {
    }

    private void topic() {
    }

    private void queue() {
    }

    /**
     *
     */
    public static interface OperationRangeDomainDataHandler {


        Object handler(BaseEntity baseEntity);
    }

}
