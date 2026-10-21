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
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDifference implements Difference<BaseClusterIdBase> {

    /**
     * 是否相信 allData 数据是正确的，如果不正确怎么做。 有一个 check，每24小时，全量加载一次数据。
     */
    @Setter
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
            targetHandler.handleAll(this.allData.values(), this.insertData, this.updateData, this.deleteData);
            this.closeUpdate();
        } catch (Exception e) {
            // TODO
            log.error(e.getMessage(), e);
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
