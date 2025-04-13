package org.apache.eventmesh.dashboard.console.domain.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 直接序列化，或则 使用 mapper
 */
@SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
@Slf4j
public class ClusterMetadataDomain {

    private ColonyDO<ClusterDO> colonyDO;


    private boolean coreModel = true;

    private boolean buildConfig = false;

    @Setter
    private DataHandler handler;

    public void isConsoleModel() {
        this.coreModel = false;
    }

    public void useBuildConfig() {
        this.buildConfig = true;
    }


    public ColonyDO<ClusterDO> getColonyDO(Long clusterId) {
        return colonyDO.getAllColonyDO().get(clusterId);
    }

    public List<ColonyDO<ClusterDO>> getMetaColonyDO(Long clusterId) {
        ColonyDO<ClusterDO> clusterDO = colonyDO.getAllColonyDO().get(clusterId);
        if (Objects.isNull(clusterDO)) {
            return null;
        }

        // TODO
        if (colonyDO.getClusterType().isMetaAndRuntime()) {
            return new ArrayList<>(clusterDO.getRuntimeColonyDOMap().values());
        }
        return new ArrayList<>(clusterDO.getMetaColonyDOList().values());
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

    public QueueCondition createQueueCondition() {
        return new QueueCondition();
    }


    public void handlerMetadata(MetadataAllDO metadataAllDO) {
        Map<Long, ColonyDO<ClusterDO>> colonyDOMap = new HashMap<>();

        this.setClusterEntityAndDefinitionCluster(metadataAllDO.getClusterEntityList(), metadataAllDO.getClusterRelationshipEntityList(),
            colonyDOMap);
        this.setRuntimeEntity(metadataAllDO.getRuntimeEntityList(), colonyDOMap);

        colonyDOMap.forEach((key, value) -> {
            AbstractMultiCreateSDKConfig createSDKConfig = value.getClusterDO().getMultiCreateSDKConfig();
            if (Objects.isNull(createSDKConfig) || createSDKConfig.isNullAddress()) {
                return;
            }
            if (!ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType()).isCAP()) {
                return;
            }
            if (Objects.nonNull(this.handler)) {
                // TODO
                this.handler.registerCluster(null, value.getClusterDO(), value);
                //this.handler.registerCluster(value.getClusterDO().getClusterInfo(), value.getClusterDO(), value);
            }
        });
    }


    public List<ClusterEntity> setClusterEntityAndDefinitionCluster(List<ClusterEntity> clusterEntityList,
        List<ClusterRelationshipEntity> clusterRelationshipEntityList,
        Map<Long, ColonyDO<ClusterDO>> colonyDOMap) {
        List<ClusterEntity> definitionClusteList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(clusterEntityList)) {
            clusterEntityList.forEach(v -> {
                if (v.getClusterType().isDefinition()) {
                    definitionClusteList.add(v);
                }
                if (Objects.equals(v.getStatus(), 0L)) {
                    ColonyDO<ClusterDO> colonyDO = this.colonyDO.remove(v.getId());
                    if (Objects.nonNull(this.handler)) {
                        this.handler.unRegisterCluster(v, colonyDO.getClusterDO(), colonyDO);
                    }
                } else {
                    ClusterBaseDO clusterEntityDO = this.createClusterBaseDO(v);
                    ColonyDO<ClusterDO> colonyDO = this.colonyDO.register(v.getId(), v.getClusterType(), clusterEntityDO);
                    if (!v.getClusterType().isDefinition()) {
                        colonyDOMap.put(v.getId(), colonyDO);
                    }
                }
            });
        }
        if (CollectionUtils.isEmpty(clusterRelationshipEntityList)) {
            return definitionClusteList;
        }
        clusterRelationshipEntityList.forEach(v -> {
            try {
                if (Objects.equals(v.getStatus(), 0L)) {
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

    public void setRuntimeEntity(List<RuntimeEntity> runtimeEntityList, Map<Long, ColonyDO<ClusterDO>> colonyDOMap) {
        if (Objects.isNull(runtimeEntityList)) {
            return;
        }
        runtimeEntityList.forEach(value -> {
            NetAddress netAddress = new NetAddress();
            netAddress.setAddress(value.getHost());
            netAddress.setPort(value.getPort());
            ColonyDO<ClusterDO> colonyDO;
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(value.getClusterType());
            if (Objects.equals(value.getStatus(), 0L)) {
                colonyDO = this.colonyDO.removeRuntime(value.getClusterId(), value.getId(), netAddress);
                if (Objects.nonNull(colonyDO) && clusterFramework.isCAP()) {
                    colonyDOMap.put(value.getClusterId(), colonyDO);
                } else {
                    if (Objects.nonNull(this.handler)) {
                        this.handler.unRegisterRuntime(value, null, colonyDO);
                    }
                }
            } else {
                RuntimeBaseDO runtimeBaseDO = this.createRuntimeDO(value);
                colonyDO = this.colonyDO.register(value.getClusterId(), value.getId(), runtimeBaseDO, netAddress);

                if (Objects.isNull(colonyDO)) {
                    log.warn(" not queue colonyDO, runtime is {}", value);
                    return;
                }
                if (clusterFramework.isCAP()) {
                    colonyDOMap.put(value.getClusterId(), colonyDO);
                } else {
                    if (Objects.nonNull(this.handler)) {
                        this.handler.registerRuntime(value, runtimeBaseDO, colonyDO);
                    }
                }
            }
        });

    }

    private ClusterBaseDO createClusterBaseDO(ClusterEntity clusterEntity) {
        ClusterBaseDO clusterBaseDO;
        ClusterType clusterType = clusterEntity.getClusterType();
        if (this.coreModel) {
            clusterBaseDO = new ClusterDO();
            ClusterMetadata clusterMetadata = ClusterConvertMetaData.INSTANCE.toMetaData(clusterEntity);
            clusterBaseDO.setClusterInfo(clusterMetadata);
        } else {
            clusterBaseDO = new ClusterEntityDO();
            clusterBaseDO.setClusterInfo(clusterEntity);
        }
        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
        if (this.buildConfig && !clusterType.isDefinition() && clusterFramework.isCAP()) {
            AbstractMultiCreateSDKConfig config =
                ConfigManage.getInstance().getMultiCreateSDKConfig(clusterEntity.getClusterType(), SDKTypeEnum.ADMIN);
            config.setKey(clusterEntity.getId().toString());
            clusterBaseDO.setMultiCreateSDKConfig(config);
        }
        return clusterBaseDO;
    }

    private RuntimeBaseDO createRuntimeDO(RuntimeEntity runtimeEntity) {
        RuntimeBaseDO runtimeBaseDO;
        if (this.coreModel) {
            runtimeBaseDO = new RuntimeDO();
            RuntimeMetadata runtimeMetadata = RuntimeConvertMetaData.INSTANCE.toMetaData(runtimeEntity);
            runtimeBaseDO.setRuntimeMetadata(runtimeMetadata);
            ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(runtimeEntity.getClusterType());
            if (!clusterFramework.isCAP()) {
                AbstractSimpleCreateSDKConfig config =
                    ConfigManage.getInstance().getSimpleCreateSDKConfig(runtimeEntity.getClusterType(), SDKTypeEnum.ADMIN);
                config.setKey(runtimeEntity.getId().toString());
                config.setNetAddress(this.createNetAddress(runtimeEntity));
                runtimeBaseDO.setCreateSDKConfig(config);
            }
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


    public void operation(Long clusterId, ClusterOperationHandler clusterOperationHandler) {

        // 需要识别最小维度

        ColonyDO<ClusterDO> colonyDO = this.colonyDO.getAllColonyDO().get(clusterId);
        ClusterType clusterType = colonyDO.getClusterType();
        if (Objects.equals(clusterType, ClusterType.EVENTMESH_RUNTIME)) {
            colonyDO.getClusterDO().getRuntimeMap().forEach((key, value) -> {
                clusterOperationHandler.handler(value.getRuntimeMetadata());
            });
        } else if (Objects.equals(clusterType, ClusterType.EVENTMESH_CLUSTER)) {
            colonyDO.getRuntimeColonyDOMap().forEach((key, value) -> {
                value.getClusterDO().getRuntimeMap().forEach((k, v) -> {
                    clusterOperationHandler.handler(v.getRuntimeMetadata());
                });
            });

        } else if (clusterType.isStorage()) {
            if (clusterType.isDefinition()) {
                colonyDO.getStorageColonyDOMap().forEach((key, value) -> {
                    value.getRuntimeColonyDOMap().forEach((k, v) -> {
                        value.getClusterDO().getRuntimeMap().forEach((kk, vv) -> {
                            clusterOperationHandler.handler(vv.getRuntimeMetadata());
                        });
                    });
                });
            } else {
                ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(clusterType);
                if (clusterFramework.isCAP()) {
                    clusterOperationHandler.handler(colonyDO.getClusterDO().getClusterInfo());
                } else {
                    colonyDO.getClusterDO().getRuntimeMap().forEach((k, v) -> {
                        clusterOperationHandler.handler(v.getRuntimeMetadata());
                    });
                }
            }
        }
    }


    public <T> T queue(QueueCondition queueCondition) {
        QueueConditionHandler queueConditionHandler = new QueueConditionHandler();
        queueConditionHandler.colonyDO = this.colonyDO.getAllColonyDO().get(queueCondition.clusterId);
        queueConditionHandler.queueCondition = queueCondition;
        return (T) queueConditionHandler.handler();
    }


    /**
     *
     */
    @SuppressWarnings("rawtypes")
    public interface DataHandler<T extends RuntimeBaseDO, C extends ClusterBaseDO> {

        /**
         * TODO 只有 非 CAP 模式的 runtime调用此方法。 CAP 模式的 runtime 更新默认为 Cluster 更新
         */
        void registerRuntime(RuntimeEntity runtimeEntity, T t, ColonyDO<C> colonyDO);

        /**
         * TODO 只有 非 CAP 模式的 runtime调用此方法。 CAP 模式的 runtime 更新默认为 Cluster 更新
         */
        void unRegisterRuntime(RuntimeEntity runtimeEntity, T t, ColonyDO<C> colonyDO);

        void registerCluster(ClusterEntity clusterEntity, C c, ColonyDO<C> colonyDO);

        void unRegisterCluster(ClusterEntity clusterEntity, C c, ColonyDO<C> colonyDO);
    }

    @SuppressWarnings("rawtypes")
    private static class QueueConditionHandler {

        private QueueCondition queueCondition;

        private ColonyDO<ClusterDO> colonyDO;

        private Map<Long, ColonyDO<ClusterDO>> currentColonyDOMap;

        private final List<Object> resultData = new ArrayList<>();

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
                this.currentColonyDOMap = this.colonyDO.getRuntimeColonyDOMap();
            } else if (this.queueCondition.clusterType == ClusterType.STORAGE) {
                this.currentColonyDOMap = this.colonyDO.getStorageColonyDOMap();
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


    public static class QueueCondition {

        private Long clusterId;

        /**
         *
         */
        private ClusterType clusterType;

        private final ClusterType resultType = ClusterType.CLUSTER;

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
