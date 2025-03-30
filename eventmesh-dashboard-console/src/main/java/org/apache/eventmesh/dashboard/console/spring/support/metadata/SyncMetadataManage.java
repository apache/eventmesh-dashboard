package org.apache.eventmesh.dashboard.console.spring.support.metadata;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ClusterSyncMetadata;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseClusterIdEntity;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.core.remoting.CreateProxyDO;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @see org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage  两个需要进行合并。花了一天的时间完成解耦
 */
@Deprecated
@Component
public class SyncMetadataManage {

    @Autowired
    private List<DataMetadataHandler<Object>> dataMetadataHandlerList;

    private final Remoting2Manage remoting2Manage = new Remoting2Manage();

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 20, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    private final Map<Class<?>, Map<Long, List<MetadataSyncBaseWrapper>>> metadataSyncBaseWRapperMap = new ConcurrentHashMap<>();

    /**
     * db
     */
    private final Map<MetadataType, SyncMetadataCreateFactory> syncMetadataCreateFactoryMap = new HashMap<>();

    @PostConstruct
    private void init() {
        Map<Class<?>, DatabaseAndMetadataMapper> databaseAndMetadataMapperMap = new HashMap<>();
        for (DatabaseAndMetadataType databaseAndMetadataType : DatabaseAndMetadataType.values()) {
            databaseAndMetadataMapperMap.put(databaseAndMetadataType.getDatabaseAndMetadataMapper().getDatabaseClass(),
                databaseAndMetadataType.getDatabaseAndMetadataMapper());
        }
        dataMetadataHandlerList.forEach((v) -> {
            DatabaseAndMetadataMapper databaseAndMetadataMapper = databaseAndMetadataMapperMap.get(v.getClass());
            SyncMetadataCreateFactory syncMetadataCreateFactory = new SyncMetadataCreateFactory();
            syncMetadataCreateFactory.setDataMetadataHandler(v);
            syncMetadataCreateFactory.setConvertMetaData(databaseAndMetadataMapper.getConvertMetaData());
            syncMetadataCreateFactory.setMetadataType(databaseAndMetadataMapper.getMetaType());
            syncMetadataCreateFactory.setDatabaseAndMetadataMapper(databaseAndMetadataMapper);

            syncMetadataCreateFactoryMap.put(databaseAndMetadataMapper.getMetaType(), syncMetadataCreateFactory);
        });
    }

    @Scheduled(fixedRate = 200)
    public void sync() {
        syncMetadataCreateFactoryMap.forEach((k, value) -> {
            threadPoolExecutor.execute(() -> {
                value.loadData();
                value.persistence();
            });
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

    public List<MetadataSyncConfig> createMetadataSyncConfig(BaseClusterIdEntity baseEntity, ClusterSyncMetadata clusterSyncMetadata) {
        List<MetadataSyncConfig> metadataSyncConfigList = new ArrayList<>();
        List<MetadataSyncBaseWrapper> metadataSyncBaseWrapperList = new ArrayList<>();

        List<MetadataType> metadataTypeList = clusterSyncMetadata.getMetadataTypeList();

        ClusterType clusterType = baseEntity.getClusterType();
        metadataTypeList.forEach((value -> {
            SyncMetadataCreateFactory syncMetadataCreateFactory = syncMetadataCreateFactoryMap.get(value);
            MetadataSyncConfig metadataSyncConfig = new MetadataSyncConfig();
            metadataSyncConfig.setDataBasesHandler(syncMetadataCreateFactory.createDataMetadataHandler(baseEntity));

            CreateProxyDO createProxyDO =
                remoting2Manage.createProxy(syncMetadataCreateFactory.getDatabaseAndMetadataMapper().getMetadataClass(), baseEntity, clusterType);
            metadataSyncConfig.setClusterService((MetadataHandler) createProxyDO.getRemotingService());
            metadataSyncConfig.setConvertMetaData(syncMetadataCreateFactory.getConvertMetaData());
            metadataSyncConfig.setClusterServiceType(null);
            metadataSyncConfig.setFirstToWhom(null);

            metadataSyncConfigList.add(metadataSyncConfig);

            MetadataSyncBaseWrapper metadataSyncBaseWrapper = new MetadataSyncBaseWrapper();
            metadataSyncBaseWrapper.metadataSyncConfigs = metadataSyncConfig;
            metadataSyncBaseWrapper.syncMetadataCreateFactory = syncMetadataCreateFactory;
            metadataSyncBaseWrapper.createProxyDO = createProxyDO;
            metadataSyncBaseWrapperList.add(metadataSyncBaseWrapper);
        }));
        this.metadataSyncBaseWRapperMap.computeIfAbsent(baseEntity.getClass(), k -> new ConcurrentHashMap<>())
            .put(baseEntity.getId(), metadataSyncBaseWrapperList);
        return metadataSyncConfigList;
    }

    public void resetClient() {

    }

    public void cancel() {

    }

    static class MetadataSyncBaseWrapper {

        private MetadataSyncConfig metadataSyncConfigs;

        private SyncMetadataCreateFactory syncMetadataCreateFactory;

        private CreateProxyDO createProxyDO;
    }

}
