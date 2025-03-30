package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseClusterIdEntity;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

/**
 * MetadataHandler。 读写行为 每个 runtime or cluster 。所有 MetadataType 的 MetadataHandler 已经关系 从 runtime（Cluster） -> 读 -> createDataMetadataHandler 缓存 -> 定时写
 * 从 db 定时读  发动  -> SyncMetadataCreateFactory -> createDataMetadataHandler 获得 runtime 或则 cluster 维度数据，在写入 runtime 1000个节点，进行一千次db操作， db直接费了。
 *
 * @param <T>
 */
public class SyncMetadataCreateFactory<T> {

    @Setter
    private DataMetadataHandler<BaseEntity> dataMetadataHandler;

    @Setter
    @Getter
    private MetadataType metadataType;

    @Setter
    @Getter
    private ConvertMetaData<BaseEntity, Object> convertMetaData;


    @Setter
    @Getter
    private DatabaseAndMetadataMapper databaseAndMetadataMapper;

    private Map<Long, List<BaseEntity>> metadataMap = new ConcurrentHashMap<>();

    private List<BaseEntity> addData = new ArrayList<>();

    private List<BaseEntity> updateData = new ArrayList<>();

    private List<BaseEntity> deleteData = new ArrayList<>();

    public void loadData() {
        Map<Long, List<Object>> metadataMap = new ConcurrentHashMap<>();
        dataMetadataHandler.getData().forEach(data -> {
            metadataMap.computeIfAbsent(data.getId(), k -> new ArrayList<>()).add(this.convertMetaData.toMetaData(data));
        });

    }

    public void persistence() {
        this.dataMetadataHandler.handleAll(this.addData, this.updateData, this.deleteData);
        this.addData = new ArrayList<>();
        this.updateData = new ArrayList<>();
        this.deleteData = new ArrayList<>();
    }

    public MetadataHandler<BaseEntity> createDataMetadataHandler(BaseClusterIdEntity runtimeEntity) {
        return new MetadataHandler<BaseEntity>() {
            @Override
            public void addMetadata(BaseEntity meta) {
                SyncMetadataCreateFactory.this.addData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void deleteMetadata(BaseEntity meta) {
                SyncMetadataCreateFactory.this.deleteData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void updateMetadata(BaseEntity meta) {
                SyncMetadataCreateFactory.this.updateData.add(meta);
            }

            @Override
            public void handleAll(List<BaseEntity> addData, List<BaseEntity> updateData, List<BaseEntity> deleteData) {
                this.addAll(SyncMetadataCreateFactory.this.addData, addData);
                this.addAll(SyncMetadataCreateFactory.this.updateData, updateData);
                this.addAll(SyncMetadataCreateFactory.this.deleteData, deleteData);
            }

            private void addAll(List<BaseEntity> persistenceData, List<BaseEntity> handleData) {
                handleData.forEach(data -> {
                    persistenceData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(data));
                });
            }

            @Override
            public List<BaseEntity> getData() {
                return SyncMetadataCreateFactory.this.metadataMap.get(runtimeEntity.getId());
            }

        };
    }
}
