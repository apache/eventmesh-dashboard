package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import java.util.List;

public class BufferDifference extends AbstractBothDifference {


    @Override
    void doDifference() {
        List<BaseClusterIdBase> sourcetList = sourceHandler.getData();
        this.difference(sourcetList, allData);
    }
}
