package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class BothDifference extends AbstractBothDifference {

    @Override
    void doDifference() {
        List<BaseClusterIdBase> sourcetList = sourceHandler.getData();
        List<BaseClusterIdBase> targetList = targetHandler.getData();
        if (CollectionUtils.isEmpty(sourcetList)) {
            /**
             *   TODO
             *       有这种极端环境吗？
             *       两边都删除 缓存为空
             *
             */
            this.deleteData.addAll(targetList);
        } else if (targetList.isEmpty()) {
            // TODO 全量加入缓存
            this.insertData.addAll(sourcetList);
            targetList.forEach((value) -> {
                this.allData.put(value.nodeUnique(), value);
            });
        }
        this.allData = this.difference(sourcetList, targetList);
    }
}
