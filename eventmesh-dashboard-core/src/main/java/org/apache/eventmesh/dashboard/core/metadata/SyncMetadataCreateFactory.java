package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.DatabaseAndMetadataMapper;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;

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
    private DataMetadataHandler<Object> dataMetadataHandler;

    @Getter
    private MetadataType metadataType;

    @Getter
    private ConvertMetaData<Object, Object> convertMetaData;


    @Getter
    private DatabaseAndMetadataMapper databaseAndMetadataMapper;

    private Map<Long, List<Object>> metadataMap = new ConcurrentHashMap<>();

    private List<Object> addData = new ArrayList<>();

    private List<Object> updateData = new ArrayList<>();

    private List<Object> deleteData = new ArrayList<>();

    public void loadData() {
        Map<Long, List<Object>> metadataMap = new ConcurrentHashMap<>();
        // TODO 排除
        dataMetadataHandler.getData().forEach(data -> {
            metadataMap.computeIfAbsent(2L, k -> new ArrayList<>()).add(this.convertMetaData.toMetaData(data));
        });

    }

    public void persistence() {
        this.dataMetadataHandler.handleAll(this.addData, this.updateData, this.deleteData);
        this.addData = new ArrayList<>();
        this.updateData = new ArrayList<>();
        this.deleteData = new ArrayList<>();
    }

    public MetadataHandler<Object> createDataMetadataHandler(BaseSyncBase runtimeEntity, Long id) {
        return new MetadataHandler<Object>() {
            @Override
            public void addMetadata(Object meta) {
                SyncMetadataCreateFactory.this.addData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void deleteMetadata(Object meta) {
                SyncMetadataCreateFactory.this.deleteData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(meta));
            }

            @Override
            public void updateMetadata(Object meta) {
                SyncMetadataCreateFactory.this.updateData.add(meta);
            }

            @Override
            public void handleAll(List<Object> addData, List<Object> updateData, List<Object> deleteData) {
                this.addAll(SyncMetadataCreateFactory.this.addData, addData);
                this.addAll(SyncMetadataCreateFactory.this.updateData, updateData);
                this.addAll(SyncMetadataCreateFactory.this.deleteData, deleteData);
            }

            private void addAll(List<Object> persistenceData, List<Object> handleData) {
                handleData.forEach(data -> {
                    persistenceData.add(SyncMetadataCreateFactory.this.convertMetaData.toEntity(data));
                });
            }

            @Override
            public List<Object> getData() {
                return SyncMetadataCreateFactory.this.metadataMap.get(id);
            }

        };
    }
}
