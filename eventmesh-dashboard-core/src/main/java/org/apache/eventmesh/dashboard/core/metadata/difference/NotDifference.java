package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import java.util.List;

public class NotDifference extends AbstractDifference {

    @Override
    void doDifference() {
        List<BaseClusterIdBase> bothDifferenceList = this.sourceHandler.getData();
        this.insertData.addAll(bothDifferenceList);
    }
}
