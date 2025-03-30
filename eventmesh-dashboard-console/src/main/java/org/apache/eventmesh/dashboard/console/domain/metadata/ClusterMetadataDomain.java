package org.apache.eventmesh.dashboard.console.domain.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.domain.ClusterEntityDO;
import org.apache.eventmesh.dashboard.console.modle.domain.RuntimeEntityDO;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ClusterConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.RuntimeConvertMetaData;
import org.apache.eventmesh.dashboard.core.cluster.ClusterBaseDO;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeBaseDO;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeDO;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;

import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 直接序列化，或则 使用 mapper
 */
public class ClusterMetadataDomain {

    private ColonyDO<ClusterEntityDO> colonyDO;


    private boolean coreModel = true;

    private DataHandler handler;

    public void isConsoleModel() {
        this.coreModel = false;
    }


    public void setMainCluster(ClusterEntity clusterEntity) {
        this.colonyDO = ColonyDO.create(ClusterEntityDO.class, clusterEntity);
        this.colonyDO.setClusterId(clusterEntity.getClusterId());
        this.colonyDO.setClusterType(clusterEntity.getClusterType());
        this.colonyDO.setSuperiorId(-0L);
    }

    public void rootClusterDHO() {
        this.colonyDO = ColonyDO.create(ClusterEntityDO.class, null);
        this.colonyDO.setSuperiorId(-0L);
    }

    public void setHandler(DataHandler handler) {
        this.handler = handler;
    }

    public QueueCondition createQueueCondition() {
        return new QueueCondition();
    }

    public HandlerMetadataDO handlerMetadata(MetadataAllDO metadataAllDO) {
        HandlerMetadataDO handlerMetadataDO = new HandlerMetadataDO();
        this.setClusterEntityAndDefinitionCluster(metadataAllDO.getClusterEntityList(), metadataAllDO.getClusterRelationshipEntityList());
        this.setRuntimeEntity(metadataAllDO.getRuntimeEntityList(), handlerMetadataDO);

        return handlerMetadataDO;
    }

    public List<ClusterEntity> setClusterEntityAndDefinitionCluster(List<ClusterEntity> clusterEntity,
        List<ClusterRelationshipEntity> clusterRelationshipEntity) {
        List<ClusterEntity> definitionClusteList = new ArrayList<>();
        clusterEntity.forEach(v -> {
            if (v.getClusterType().isDefinition()) {
                definitionClusteList.add(v);
            }
            if (Objects.equals(v.getStatus(), 0)) {
                this.colonyDO.remove(v.getId());
            } else {
                this.colonyDO.register(v.getId(), v.getClusterType(), this.createClusterBaseDO(v));
            }
        });

        clusterRelationshipEntity.forEach(v -> {
            try {
                if (Objects.equals(v.getStatus(), 0)) {
                    this.colonyDO.unRelationship(v.getClusterId(), v.getRelationshipId());
                } else {
                    this.colonyDO.relationship(v.getClusterId(), v.getRelationshipId());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return definitionClusteList;
    }

    public void setRuntimeEntity(List<RuntimeEntity> runtimeEntity, HandlerMetadataDO handlerMetadataDO) {

        runtimeEntity.forEach(value -> {
            NetAddress netAddress = new NetAddress();
            netAddress.setAddress(value.getHost());
            netAddress.setPort(value.getPort());
            if (Objects.equals(value.getStatus(), 0)) {
                this.colonyDO.removeRuntime(value.getClusterId(), value.getId(), netAddress);
            } else {
                this.colonyDO.register(value.getClusterId(), value.getId(), this.createRuntimeDO(value), netAddress);
            }
        });
    }

    private ClusterBaseDO createClusterBaseDO(ClusterEntity clusterEntity) {
        ClusterBaseDO clusterBaseDO;
        if (this.coreModel) {
            clusterBaseDO = new ClusterDO();
            ClusterMetadata clusterMetadata = ClusterConvertMetaData.INSTANCE.toMetaData(clusterEntity);
            clusterBaseDO.setClusterInfo(clusterMetadata);
            AbstractMultiCreateSDKConfig config =
                ConfigManage.getInstance().getMultiCreateSDKConfig(clusterEntity.getClusterType(), SDKTypeEnum.ADMIN);
            config.setKey(clusterEntity.getId().toString());

            clusterBaseDO.setMultiCreateSDKConfig(config);
        } else {
            clusterBaseDO = new ClusterEntityDO();
            clusterBaseDO.setClusterInfo(clusterEntity);
        }
        return clusterBaseDO;
    }

    private RuntimeBaseDO createRuntimeDO(RuntimeEntity runtimeEntity) {
        RuntimeBaseDO runtimeBaseDO;
        if (this.coreModel) {
            runtimeBaseDO = new RuntimeDO();
            RuntimeMetadata runtimeMetadata = RuntimeConvertMetaData.INSTANCE.toMetaData(runtimeEntity);
            runtimeBaseDO.setRuntimeMetadata(runtimeMetadata);
            AbstractSimpleCreateSDKConfig config =
                ConfigManage.getInstance().getSimpleCreateSDKConfig(runtimeEntity.getClusterType(), SDKTypeEnum.ADMIN);
            config.setKey(runtimeEntity.getId().toString());
            config.setNetAddress(this.createNetAddress(runtimeEntity));
            runtimeBaseDO.setCreateSDKConfig(config);
        } else {
            runtimeBaseDO = new RuntimeEntityDO();
            runtimeBaseDO.setRuntimeMetadata(runtimeEntity);
        }
        return runtimeBaseDO;
    }

    private NetAddress createNetAddress(RuntimeEntity runtimeEntity) {
        NetAddress netAddress = new NetAddress();
        netAddress.setAddress(runtimeEntity.getHost());
        netAddress.setPort(runtimeEntity.getPort());
        return netAddress;
    }


    public void setResource() {

    }

    public void setConfigEntity() {

    }


    public <T> T queue(QueueCondition queueCondition) {
        QueueConditionHandler queueConditionHandler = new QueueConditionHandler();
        return (T) queueConditionHandler.handler();
    }


    /**
     *
     */
    public static interface DataHandler<T extends RuntimeBaseDO, C extends ClusterBaseDO> {


        void registerRuntime(RuntimeEntity runtimeEntity, T t, ColonyDO<C> colonyDO);

        void unRegisterRuntime(RuntimeEntity runtimeEntity, T t, ColonyDO<C> colonyDO);

        void registerCluster(ClusterEntity clusterEntity, C c, ColonyDO<C> colonyDO);

        void unRegisterCluster(ClusterEntity clusterEntity, C c, ColonyDO<C> colonyDO);
    }

    private static class QueueConditionHandler {

        private QueueCondition queueCondition;

        private ColonyDO<ClusterEntityDO> colonyDO;

        private Map<Long, ColonyDO<ClusterEntityDO>> currentColonyDOMap;

        private List<Object> resultData = new ArrayList<>();

        public <T> T handler() {
            this.getColonyDOMap();
            if (MapUtils.isEmpty(currentColonyDOMap)) {
                return (T) resultData;
            }
            this.currentColonyDOMap.forEach((k, v) -> {
                this.handlerData(v);
            });
            return (T) this.resultData;
        }

        public void getColonyDOMap() {
            if (this.queueCondition.clusterType == ClusterType.META) {
                this.currentColonyDOMap = this.colonyDO.getMetaColonyDOList();
            } else if (this.queueCondition.clusterType == ClusterType.RUNTIME) {
                this.currentColonyDOMap = this.colonyDO.getRuntimeColonyDOList();
            } else if (this.queueCondition.clusterType == ClusterType.STORAGE) {
                this.currentColonyDOMap = this.colonyDO.getStorageColonyDOList();
            }
        }

        public void handlerData(ColonyDO colonyDO) {
            if (this.queueCondition.resultType == ClusterType.CLUSTER) {
                if (this.queueCondition.resultId) {
                    resultData.add(colonyDO.getClusterId());
                } else {
                    resultData.add(colonyDO.getClusterDO().getClusterInfo());
                }
            } else {
                Map<Long, Object> data = colonyDO.getClusterDO().getRuntimeMap();
                resultData.add(this.queueCondition.resultId ? data.keySet() : data.values());
            }
        }


    }


    static class QueueCondition {

        private Long clusterId;

        /**
         *
         */
        private ClusterType clusterType;

        private ClusterType resultType = ClusterType.CLUSTER;

        private boolean resultId = false;


        public QueueCondition clusterId(Long clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public QueueCondition clusterType(ClusterType clusterType) {
            this.clusterType = clusterType;
            return this;
        }

        public QueueCondition meta() {
            this.clusterType = ClusterType.META;
            return this;
        }

        public QueueCondition storage() {
            this.clusterType = ClusterType.STORAGE;
            return this;
        }

        public QueueCondition runtime() {
            this.clusterType = ClusterType.RUNTIME;
            return this;
        }

        public QueueCondition() {
        }

        public QueueCondition resultId() {
            this.resultId = true;
            return this;
        }
    }
}
