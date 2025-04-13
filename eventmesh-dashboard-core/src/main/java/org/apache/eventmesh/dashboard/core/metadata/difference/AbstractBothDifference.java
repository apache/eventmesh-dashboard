package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractBothDifference extends AbstractBufferDifference {


    protected Map<String, BaseClusterIdBase> difference(List<BaseClusterIdBase> sourcetList, List<BaseClusterIdBase> targetList) {
        Map<String, BaseClusterIdBase> targetAllData = new HashMap<>();
        targetList.forEach((value) -> {
            targetAllData.put(value.nodeUnique(), value);
        });
        return this.difference(sourcetList, targetAllData);
    }

    /**
     * database 与 cluster 求结果
     */
    protected Map<String, BaseClusterIdBase> difference(List<BaseClusterIdBase> sourcetList, Map<String, BaseClusterIdBase> targetAllData) {
        if (sourcetList.isEmpty()) {
            return targetAllData;
        }
        Map<String, BaseClusterIdBase> newAllData = new HashMap<>();
        sourcetList.forEach((value) -> {
            String key = value.nodeUnique();
            BaseClusterIdBase oldValue = this.allData.remove(key);
            if (Objects.isNull(oldValue)) {
                this.insertData.add(value);
                newAllData.put(key, value);
            } else if (!Objects.equals(oldValue, value)) {
                this.updateData.add(value);
                newAllData.put(key, value);
            } else {
                newAllData.put(key, value);
            }

        });
        this.deleteData.addAll(this.allData.values());
        return newAllData;
    }
}
