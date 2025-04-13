package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractBufferDifference extends AbstractDifference implements DataMetadataHandler<BaseClusterIdBase> {

    @Getter
    @Setter
    protected Map<String, BaseClusterIdBase> allData = new HashMap<>();


    public void handleAll(List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData, List<BaseClusterIdBase> deleteData) {
        this.targetHandler.handleAll(addData, updateData, deleteData);
    }

    /**
     * BothDifference 是否进行一次识别
     *
     * @return
     */
    public List<BaseClusterIdBase> getData() {
        return new ArrayList<>(allData.values());
    }
}
