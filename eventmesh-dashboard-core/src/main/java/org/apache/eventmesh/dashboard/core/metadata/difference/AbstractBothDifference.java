/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.core.metadata.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractBothDifference extends AbstractBufferDifference {


    protected Map<String, BaseClusterIdBase> basedOnSourceDifference(List<BaseClusterIdBase> sourcetList, List<BaseClusterIdBase> targetList) {
        Map<String, BaseClusterIdBase> sourceData = new HashMap<>();
        sourcetList.forEach((value) -> {
            sourceData.put(value.nodeUnique(), value);
        });
        return this.basedOnSourceDifference(targetList, sourceData);
    }

    /**
     * database 与 cluster 求结果
     * 依据 target 删除了 source 集合里面的数据， sourceData 里面剩下的数据 是 target 不存在啊的
     * target 4 5 6 7 ,  source  1 2 3 4 5 6
     * 4 5 6 remove 不等于 null， 不做任何操作。
     * 7 remove 时， 为空，那么需要 删除 7.
     * 剩下 1 2 3 ，那么表示需要新增
     * TODO update 操作如何识别，equals 识别。比如 topic 元信息 ， 配置信息，是 一起 equals，还是分开 </p>
     */
    protected Map<String, BaseClusterIdBase> basedOnSourceDifference(List<BaseClusterIdBase> targetList, Map<String, BaseClusterIdBase> sourceData) {
        if (targetList.isEmpty()) {
            return sourceData;
        }
        Map<String, BaseClusterIdBase> newAllData = new HashMap<>();
        targetList.forEach((value) -> {
            String key = value.nodeUnique();
            BaseClusterIdBase oldValue = sourceData.remove(key);
            if(Objects.isNull(oldValue)) {// source 不存在， 而 target 存在，删处理
                this.deleteData.add(value);
            }
        });

        this.insertData.addAll(sourceData.values());
        return newAllData;
    }

    /**
     * sourceData 存在 ， allData 不存在的  添加
     * sourceData 不存在 ， allData 存在的  删除
     * @param sourceData
     * @param allData
     */
    protected void intersection(List<BaseClusterIdBase> sourceData,Map<String, BaseClusterIdBase> allData) {
        if (CollectionUtils.isEmpty(sourceData)) {
            return;
        }
        if(MapUtils.isEmpty(allData)){
            sourceData.forEach(data -> {
                this.allData.put(data.getUnique(),data);
                this.insertData.add(data);
            });
            return;
        }
        Map<String, BaseClusterIdBase> newAllData = new HashMap<>();
        sourceData.forEach((value) -> {
            String key = value.getUnique();
            BaseClusterIdBase oldValue = allData.remove(key);
            if (Objects.isNull(oldValue)) {
                this.insertData.add(value);
            }
            newAllData.put(key, value);

        });
        // 从 cluster 同步，需要
        this.deleteData.addAll(allData.values());
        allData.clear();
        allData.putAll(newAllData);
    }
}
