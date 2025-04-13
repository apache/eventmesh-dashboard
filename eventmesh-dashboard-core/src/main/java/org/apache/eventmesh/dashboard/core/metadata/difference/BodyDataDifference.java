package org.apache.eventmesh.dashboard.core.metadata.difference;


import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import java.util.List;

public class BodyDataDifference extends AbstractBufferDifference {


    @Override
    void doDifference() {
        List<BaseClusterIdBase> objectList = sourceHandler.getData();
        if (objectList.isEmpty()) {
            return;
        }
        objectList.forEach((value) -> {
            String key = value.nodeUnique();
            if (value.isInsert()) {
                this.insertData.add(value);
                this.allData.put(key, value);
            } else if (value.isUpdate()) {
                this.updateData.add(value);
                this.allData.put(key, value);
            } else if (value.isDelete()) {
                this.deleteData.add(value);
                this.allData.remove(key);
            }
        });

    }
}
