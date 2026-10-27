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

import java.util.List;

/**
 *
 */
public class BufferDifference extends AbstractBothDifference {


    @Override
    void doDifference() {
        List<BaseClusterIdBase> sourcetList = sourceHandler.getData();
        List<BaseClusterIdBase> live = sourcetList.stream().filter(value -> !value.isDelete()).toList();
        // Explicit tombstones supersede cached rows; otherwise intersection also schedules their deletion.
        sourcetList.stream().filter(BaseClusterIdBase::isDelete).forEach(value -> {
            this.allData.remove(value.nodeUnique());
            this.deleteData.add(value);
        });
        this.intersection(live, allData);
    }
}
