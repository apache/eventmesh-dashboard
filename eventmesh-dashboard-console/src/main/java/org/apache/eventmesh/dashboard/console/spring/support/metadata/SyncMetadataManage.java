package org.apache.eventmesh.dashboard.console.spring.support.metadata;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.core.metadata.ConvertMetaData;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
public class SyncMetadataManage {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private Map<String, DataMetadataHandler<Object>> dataMetadataHandlerMap;

    @Setter
    private Map<MetadataType, Object> metadataHandlerMap;

    private Map<String, ConvertMetaData<Object, Object>> dataConvertMetaDataMap;

    private


    private Map<MetadataType, SyncMetadataCreateFactory> stringSyncMetadataCreateFactoryMap = new HashMap<>();

    @PostConstruct
    private void init() {
        dataMetadataHandlerMap.forEach((k, v) -> {
            MetadataType metadataType = MetadataType.valueOf(k);
            SyncMetadataCreateFactory syncMetadataCreateFactory = new SyncMetadataCreateFactory();
            syncMetadataCreateFactory.setDataMetadataHandler(v);
            syncMetadataCreateFactory.setConvertMetaData(dataConvertMetaDataMap.get(k));
            syncMetadataCreateFactory.setMetadataType(metadataType);
            stringSyncMetadataCreateFactoryMap.put(metadataType, syncMetadataCreateFactory);
        });
    }

    /**
     * {@link org.apache.eventmesh.dashboard.common.enums.ClusterType}
     * <p>
     * META 同步 runtime store 除 runtime 之外所有
     *
     * @param baseEntity
     * @return
     */
    public Map<String, DataMetadataHandler<Object>> getDataMetadataHandlerMap(BaseEntity baseEntity, ClusterSyncMetadata clusterSyncMetadata) {
        Map<String, DataMetadataHandler<Object>> dataMetadataHandlerMap = new HashMap<>();
        // meta 只有 runtime

        // stago . topic, group ,订阅， client
        return dataMetadataHandlerMap;
    }

    public List<MetadataSyncConfig> createMetadataSyncConfig(BaseEntity baseEntity, ClusterSyncMetadata clusterSyncMetadata) {
        List<MetadataSyncConfig> metadataSyncConfigs = new ArrayList<>();
        List<MetadataType> metadataTypeList = clusterSyncMetadata.getMetadataTypeList();
        ClusterType clusterType = baseEntity.getClusterType();
        metadataTypeList.forEach((value -> {
            SyncMetadataCreateFactory syncMetadataCreateFactory = stringSyncMetadataCreateFactoryMap.get(value);
            MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
            metadataSyncConfig.setDataBasesHandler(syncMetadataCreateFactory.createDataMetadataHandler(baseEntity));
            metadataSyncConfig.setClusterService(this.metadataHandlerMap.get(value));
            metadataSyncConfig.setConvertMetaData(syncMetadataCreateFactory.getConvertMetaData());
            metadataSyncConfig.setClusterServiceType(null);
            metadataSyncConfig.setFirstToWhom(null);

            metadataSyncConfigs.add(metadataSyncConfig);
        });
        return metadataSyncConfigs;
    }

    public void cancel() {

    }
}
