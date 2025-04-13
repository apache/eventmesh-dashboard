package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;

public abstract class AbstractDifference implements Difference<BaseClusterIdBase> {


    protected Map<String, BaseClusterIdBase> allData = new HashMap<>();


    protected List<BaseClusterIdBase> deleteData = new ArrayList<>();

    protected List<BaseClusterIdBase> insertData = new ArrayList<>();

    protected List<BaseClusterIdBase> updateData = new ArrayList<>();

    @Setter
    protected DataMetadataHandler<BaseClusterIdBase> sourceHandler;

    @Setter
    protected DataMetadataHandler<BaseClusterIdBase> targetHandler;


    @Override
    public void difference() {
        try {
            this.doDifference();
            targetHandler.handleAll(this.insertData, this.updateData, this.deleteData);
        } catch (Exception e) {
            // TODO
        } finally {
            this.closeUpdate();
        }
    }

    public void closeAll() {
        this.allData.clear();
    }

    public void closeUpdate() {
        this.deleteData.clear();
        this.updateData.clear();
        this.insertData.clear();
    }


    abstract void doDifference();

}
