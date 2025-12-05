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

import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 双方直接求差
 */
public class BothNotCacheDifference extends AbstractBothDifference {

    @Override
    void doDifference() {
        List<BaseClusterIdBase> sourcetList = sourceHandler.getData();
        List<BaseClusterIdBase> targetList = targetHandler.getData();
        if (CollectionUtils.isEmpty(sourcetList) && CollectionUtils.isEmpty(targetList)) {
            return;
        }
        if (CollectionUtils.isEmpty(sourcetList)) {
            /**
             *   TODO
             *       有这种极端环境吗？
             *       两边都删除 缓存为空
             *
             */
            this.deleteData.addAll(targetList);
            return;
        }
        if (targetList.isEmpty()) {
            // TODO 全量加入缓存
            this.insertData.addAll(sourcetList);
            targetList.forEach((value) -> {
                this.allData.put(value.nodeUnique(), value);
            });
            return;
        }
        sourcetList.forEach(value -> {
            this.allData.put(value.nodeUnique(), value);
        });
        if (CollectionUtils.isEmpty(targetList)) {
            return;
        }
        this.basedOnSourceDifference(targetList, new HashMap<>(this.allData));
    }
}
