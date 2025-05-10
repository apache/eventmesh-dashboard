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
